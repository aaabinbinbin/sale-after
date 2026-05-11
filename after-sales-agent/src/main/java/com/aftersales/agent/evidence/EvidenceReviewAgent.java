package com.aftersales.agent.evidence;

import com.aftersales.agent.trace.AgentTraceService;
import com.aftersales.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 多模态凭证审查 Agent。
 *
 * 独立于主 Agent 的多模态 Agent，用于审查售后凭证图片/视频。
 * 模型：qwen3.5-omni-flash-realtime（百炼多模态）
 *
 * 设计原则（doc 05 Section 7）：
 * - 与订单查询等文本 Skill 可并行执行
 * - 按金额分级调用（低风险跳过、中风险只送第一张、高风险送全部）
 * - 输出结构化审查结论
 */
@Component
public class EvidenceReviewAgent {

    private static final Logger log = LoggerFactory.getLogger(EvidenceReviewAgent.class);
    private static final String MULTIMODAL_MODEL = "qwen3.5-omni-flash-realtime";

    private final RestTemplate restTemplate;
    private final AgentTraceService traceService;

    @Value("${spring.ai.openai.api-key:${MY_API_KEY:sk-placeholder}}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:${MY_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode}}")
    private String baseUrl;

    private static final String SYSTEM_PROMPT = """
            你是售后凭证审查专家。分析用户上传的售后凭证，判断：
            1. 凭证是否清晰可辨认
            2. 凭证内容是否与用户描述一致
            3. 是否存在明显伪造痕迹

            输出JSON格式：
            {
              "verdict": "CONSISTENT|INCONSISTENT|UNCERTAIN",
              "confidence": 0.0-1.0,
              "imageQuality": "CLEAR|BLURRY|UNUSABLE",
              "findings": ["发现1", "发现2"],
              "risks": [],
              "summary": "一句话总结"
            }
            只输出JSON，不要其他内容。""";

    public EvidenceReviewAgent(AgentTraceService traceService) {
        this.restTemplate = new RestTemplate();
        this.traceService = traceService;
    }

    /**
     * 审查售后凭证。
     *
     * @param traceId   关联的 Agent Trace ID
     * @param imageUrls 凭证图片 URL 列表
     * @param userClaim 用户声称的问题（如"耳机左耳无声"）
     * @param orderAmount 订单金额（用于分级决策，由调用方判断）
     * @return 审查结论
     */
    public ReviewResult review(String traceId, List<String> imageUrls, String userClaim, double orderAmount) {
        // 分级策略：按金额控制送审图片数
        List<String> imagesToReview = selectImages(imageUrls, orderAmount);
        if (imagesToReview.isEmpty()) {
            return ReviewResult.skip("低风险场景（金额≤50元），跳过凭证审查");
        }

        log.info("凭证审查开始 traceId={} images={} amount={}", traceId, imagesToReview.size(), orderAmount);

        String userPrompt = buildUserPrompt(imagesToReview, userClaim);
        long start = System.currentTimeMillis();

        try {
            String response = callMultimodalApi(userPrompt);
            long latency = System.currentTimeMillis() - start;

            // 估算 token
            int inputTokens = estimateTokens(SYSTEM_PROMPT + userPrompt);
            int outputTokens = estimateTokens(response);
            traceService.recordLlmCall(traceId, "EVIDENCE_REVIEW", MULTIMODAL_MODEL, 1,
                    SYSTEM_PROMPT, userPrompt, response,
                    inputTokens, outputTokens, latency, true, null);

            ReviewResult result = parseReviewResponse(response);
            log.info("凭证审查完成 traceId={} verdict={} confidence={}", traceId,
                    result.verdict, result.confidence);
            return result;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            traceService.recordLlmCall(traceId, "EVIDENCE_REVIEW", MULTIMODAL_MODEL, 1,
                    SYSTEM_PROMPT, userPrompt, null, 0, 0, latency, false, e.getMessage());
            log.warn("凭证审查失败 traceId={}: {}", traceId, e.getMessage());
            return ReviewResult.uncertain("凭证审查服务异常: " + e.getMessage());
        }
    }

    /** 按金额分级选择送审图片数 */
    private List<String> selectImages(List<String> imageUrls, double orderAmount) {
        if (orderAmount <= 50) return List.of();           // 跳过
        if (orderAmount <= 500) return imageUrls.stream().limit(1).toList(); // 只送第一张
        return imageUrls.stream().limit(5).toList();       // 全送（最多5张）
    }

    /** 构建多模态请求的 user prompt */
    private String buildUserPrompt(List<String> imageUrls, String userClaim) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户声称：").append(userClaim != null ? userClaim : "未提供").append("\n\n");
        sb.append("请审查以下").append(imageUrls.size()).append("张凭证图片：\n");
        for (int i = 0; i < imageUrls.size(); i++) {
            sb.append("图片").append(i + 1).append(": ").append(imageUrls.get(i)).append("\n");
        }
        return sb.toString();
    }

    /** 调用百炼多模态 API（OpenAI 兼容格式） */
    @SuppressWarnings("unchecked")
    private String callMultimodalApi(String userPrompt) {
        String url = baseUrl + "/v1/chat/completions";

        // 构建消息体
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", MULTIMODAL_MODEL);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null) throw new RuntimeException("多模态 API 返回空响应");

        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("多模态 API 无有效选择");

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    /** 解析审查响应 JSON */
    @SuppressWarnings("unchecked")
    private ReviewResult parseReviewResponse(String json) {
        try {
            String jsonStr = json.trim().replaceAll("```json|```", "").trim();
            Map<String, Object> map = JsonUtils.fromJson(jsonStr, Map.class);
            return new ReviewResult(
                    (String) map.getOrDefault("verdict", "UNCERTAIN"),
                    toDouble(map.get("confidence")),
                    (String) map.getOrDefault("imageQuality", "UNKNOWN"),
                    (List<String>) map.getOrDefault("findings", List.of()),
                    (List<String>) map.getOrDefault("risks", List.of()),
                    (String) map.getOrDefault("summary", "")
            );
        } catch (Exception e) {
            return ReviewResult.uncertain("解析审查结果失败: " + e.getMessage());
        }
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) { try { return Double.parseDouble(s); } catch (Exception e) {} }
        return 0.5;
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        int chars = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) chars++; else other++;
        }
        return (int) (chars / 1.5 + other / 4.0);
    }

    // ====== 审查结果 ======

    /** 凭证审查结论 */
    public record ReviewResult(
            String verdict,         // CONSISTENT / INCONSISTENT / UNCERTAIN
            double confidence,      // 置信度
            String imageQuality,    // CLEAR / BLURRY / UNUSABLE
            List<String> findings,  // 审查发现
            List<String> risks,     // 风险点
            String summary          // 一句话总结
    ) {
        public static ReviewResult skip(String reason) {
            return new ReviewResult("SKIPPED", 1.0, "N/A", List.of(reason), List.of(), reason);
        }
        public static ReviewResult uncertain(String reason) {
            return new ReviewResult("UNCERTAIN", 0.0, "UNKNOWN", List.of(reason), List.of(), reason);
        }
        public boolean isConsistent() { return "CONSISTENT".equals(verdict); }
        public boolean isSkipped() { return "SKIPPED".equals(verdict); }
    }
}
