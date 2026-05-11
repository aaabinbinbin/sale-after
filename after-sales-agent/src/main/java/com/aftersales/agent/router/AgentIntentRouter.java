package com.aftersales.agent.router;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.trace.AgentTraceService;
import com.aftersales.common.enums.AgentIntent;
import com.aftersales.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Agent 意图路由器。LLM 优先，降级为关键词规则兜底。
 */
@Component
public class AgentIntentRouter {

    private static final Logger log = LoggerFactory.getLogger(AgentIntentRouter.class);
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("O\\d{12}");
    private static final String MODEL = "qwen3.6-flash";

    private final ChatClient chatClient;
    private final AgentTraceService traceService;

    private static final Map<String, AgentIntent> KEYWORD_MAP = new LinkedHashMap<>();
    // LLM 是意图路由的主力，此表仅用于 LLM 完全不可用时的最小兜底
    static {
        KEYWORD_MAP.put("退货", AgentIntent.CREATE_AFTER_SALES_APPLICATION);
        KEYWORD_MAP.put("退款", AgentIntent.CREATE_AFTER_SALES_APPLICATION);
        KEYWORD_MAP.put("换货", AgentIntent.EXCHANGE_STOCK_CHECK);
        KEYWORD_MAP.put("补偿", AgentIntent.COMPENSATION_SUGGESTION);
        KEYWORD_MAP.put("进度", AgentIntent.QUERY_AFTER_SALES_PROGRESS);
        KEYWORD_MAP.put("政策", AgentIntent.AFTER_SALES_POLICY_QA);
        KEYWORD_MAP.put("投诉", AgentIntent.COMPLAINT_ANALYSIS);
    }

    private static final String SYSTEM_PROMPT = """
            你是电商售后意图分类器。分析用户输入，输出JSON。

            意图类型（intent字段）：
            AFTER_SALES_POLICY_QA - 问售后政策、规则、流程
            ORDER_AFTER_SALES_ELIGIBILITY - 问某个订单能不能售后
            CREATE_AFTER_SALES_APPLICATION - 要发起售后（退货/退款/换货/补偿）
            QUERY_AFTER_SALES_PROGRESS - 查售后进度
            CUSTOMER_SERVICE_ASSISTANT - 客服需要建议
            REFUND_ESTIMATION - 问退多少钱
            EXCHANGE_STOCK_CHECK - 问有没有货可以换
            COMPENSATION_SUGGESTION - 问能补偿多少
            COMPLAINT_ANALYSIS - 投诉分析
            UNKNOWN - 无法判断

            从输入中提取实体（entities字段）：orderNo、afterSalesType
            判断是否需要RAG（needRag）和工具调用（needTool）。
            置信度（confidence）0-1。

            输出格式：{"intent":"...","confidence":0.0,"entities":{},"needRag":false,"needTool":false}
            只输出JSON，不要其他内容。""";

    public AgentIntentRouter(ChatClient chatClient, AgentTraceService traceService) {
        this.chatClient = chatClient;
        this.traceService = traceService;
    }

    /** 意图路由：LLM 优先，降级为关键词规则 */
    public IntentResult route(AgentContext ctx) {
        String input = ctx.getUserInput();
        if (input == null || input.isBlank()) {
            return new IntentResult(AgentIntent.UNKNOWN.getCode(), 0.0, false, false, Map.of());
        }

        // LLM 路由 + 记录 trace（含 token 使用量）
        // LLM 是意图路由的主力，最多重试 1 次
        if (chatClient != null) {
            for (int attempt = 0; attempt < 2; attempt++) {
                long start = System.currentTimeMillis();
                try {
                    var chatResponse = chatClient.prompt()
                            .system(SYSTEM_PROMPT).user(input).call().chatResponse();
                    String llmResponse = chatResponse.getResult().getOutput().getContent();
                    long latency = System.currentTimeMillis() - start;
                    int[] tokens = extractUsage(chatResponse, SYSTEM_PROMPT + input, llmResponse);
                    traceService.recordLlmCall(ctx.getTraceId(), "INTENT_ROUTING", MODEL, 1,
                            SYSTEM_PROMPT, input, llmResponse,
                            tokens[0], tokens[1], latency, true, null);
                    log.info("LLM 意图路由响应(attempt={}): {}", attempt, llmResponse);
                    IntentResult result = parseLlmResponse(llmResponse, input);
                    if (result != null) return result;
                    break; // LLM 成功但解析失败，跳出重试走关键词
                } catch (Exception e) {
                    long latency = System.currentTimeMillis() - start;
                    log.warn("LLM 意图路由失败(attempt={}): {}", attempt, e.getMessage());
                    if (attempt == 1) { // 两次都失败才记录
                        traceService.recordLlmCall(ctx.getTraceId(), "INTENT_ROUTING", MODEL, 1,
                                SYSTEM_PROMPT, input, null,
                                estimateTokens(SYSTEM_PROMPT + input), 0, latency, false, e.getMessage());
                    }
                }
            }
        }

        // 降级：最小关键词表兜底
        return keywordRoute(input);
    }

    @SuppressWarnings("unchecked")
    private IntentResult parseLlmResponse(String json, String input) {
        try {
            String jsonStr = json.trim().replaceAll("```json|```", "").trim();
            Map<String, Object> map = JsonUtils.fromJson(jsonStr, Map.class);
            String intent = (String) map.get("intent");
            if (intent == null || AgentIntent.fromCode(intent) == null) return null;
            double confidence = toDouble(map.get("confidence"));
            boolean needRag = Boolean.TRUE.equals(map.get("needRag"));
            boolean needTool = Boolean.TRUE.equals(map.get("needTool"));
            Map<String, Object> entities = new LinkedHashMap<>();
            Object entObj = map.get("entities");
            if (entObj instanceof Map) entities.putAll((Map<String, Object>) entObj);
            String orderNo = extractOrderNo(input);
            if (orderNo != null && !entities.containsKey("orderNo")) entities.put("orderNo", orderNo);
            return new IntentResult(intent, confidence, needRag, needTool, entities);
        } catch (Exception e) {
            log.warn("LLM 响应解析失败: {}", e.getMessage());
            return null;
        }
    }

    private IntentResult keywordRoute(String input) {
        for (var entry : KEYWORD_MAP.entrySet()) {
            if (input.contains(entry.getKey())) {
                AgentIntent intent = entry.getValue();
                boolean needRag = intent == AgentIntent.AFTER_SALES_POLICY_QA
                        || intent == AgentIntent.CUSTOMER_SERVICE_ASSISTANT;
                boolean needTool = intent != AgentIntent.AFTER_SALES_POLICY_QA;
                Map<String, Object> entities = new LinkedHashMap<>();
                String orderNo = extractOrderNo(input);
                if (orderNo != null) entities.put("orderNo", orderNo);
                if (input.contains("退货退款")) entities.put("afterSalesType", "RETURN_REFUND");
                else if (input.contains("仅退款")) entities.put("afterSalesType", "REFUND_ONLY");
                else if (input.contains("换货")) entities.put("afterSalesType", "EXCHANGE");
                return new IntentResult(intent.getCode(), 0.85, needRag, needTool, entities);
            }
        }
        return new IntentResult(AgentIntent.UNKNOWN.getCode(), 0.3, true, false, Map.of());
    }

    private String extractOrderNo(String input) {
        var m = ORDER_NO_PATTERN.matcher(input);
        return m.find() ? m.group() : null;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0.5; } }
        return 0.5;
    }

    /** 从 API 返回中提取真实 token 数，不可用时降级估算。返回 [inputTokens, outputTokens] */
    private int[] extractUsage(org.springframework.ai.chat.model.ChatResponse response,
                                String inputText, String outputText) {
        try {
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                return new int[]{
                        usage.getPromptTokens().intValue(),
                        usage.getGenerationTokens().intValue()
                };
            }
        } catch (Exception ignored) {}
        return new int[]{estimateTokens(inputText), estimateTokens(outputText)};
    }

    /** 粗略 token 估算 */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        int chars = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chars++;
            } else {
                other++;
            }
        }
        return (int) (chars / 1.5 + other / 4.0);
    }

    public record IntentResult(
            String intent, double confidence, boolean needRag,
            boolean needTool, Map<String, Object> entities) {}
}
