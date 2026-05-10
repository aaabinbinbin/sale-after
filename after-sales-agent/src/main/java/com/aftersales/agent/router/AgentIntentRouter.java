package com.aftersales.agent.router;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.common.enums.AgentIntent;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent 意图路由器。
 *
 * 基于规则 + 关键词匹配的意图识别。
 * 生产环境可替换为 LLM 分类。
 */
@Component
public class AgentIntentRouter {

    // 关键词 -> 意图
    private static final Map<String, AgentIntent> KEYWORD_INTENT = new LinkedHashMap<>();

    static {
        KEYWORD_INTENT.put("退货退款", AgentIntent.CREATE_AFTER_SALES_APPLICATION);
        KEYWORD_INTENT.put("退款", AgentIntent.CREATE_AFTER_SALES_APPLICATION);
        KEYWORD_INTENT.put("换货", AgentIntent.EXCHANGE_STOCK_CHECK);
        KEYWORD_INTENT.put("补偿", AgentIntent.CUSTOMER_SERVICE_ASSISTANT);
        KEYWORD_INTENT.put("售后进度", AgentIntent.QUERY_AFTER_SALES_PROGRESS);
        KEYWORD_INTENT.put("售后状态", AgentIntent.QUERY_AFTER_SALES_PROGRESS);
        KEYWORD_INTENT.put("政策", AgentIntent.AFTER_SALES_POLICY_QA);
        KEYWORD_INTENT.put("规则", AgentIntent.AFTER_SALES_POLICY_QA);
        KEYWORD_INTENT.put("退货政策", AgentIntent.AFTER_SALES_POLICY_QA);
        KEYWORD_INTENT.put("能不能退", AgentIntent.ORDER_AFTER_SALES_ELIGIBILITY);
        KEYWORD_INTENT.put("可以退", AgentIntent.ORDER_AFTER_SALES_ELIGIBILITY);
        KEYWORD_INTENT.put("售后资格", AgentIntent.ORDER_AFTER_SALES_ELIGIBILITY);
        KEYWORD_INTENT.put("估值", AgentIntent.REFUND_ESTIMATION);
        KEYWORD_INTENT.put("退多少钱", AgentIntent.REFUND_ESTIMATION);
        KEYWORD_INTENT.put("库存", AgentIntent.EXCHANGE_STOCK_CHECK);
        KEYWORD_INTENT.put("有货", AgentIntent.EXCHANGE_STOCK_CHECK);
        KEYWORD_INTENT.put("投诉", AgentIntent.COMPLAINT_ANALYSIS);
        KEYWORD_INTENT.put("建议", AgentIntent.CUSTOMER_SERVICE_ASSISTANT);
        KEYWORD_INTENT.put("凭证", AgentIntent.SUPPLEMENT_AFTER_SALES_PROOF);
        KEYWORD_INTENT.put("上传", AgentIntent.SUPPLEMENT_AFTER_SALES_PROOF);
    }

    /**
     * 识别意图。
     *
     * @return 意图路由结果
     */
    public IntentResult route(AgentContext context) {
        String input = context.getUserInput();
        if (input == null || input.isBlank()) {
            return new IntentResult(AgentIntent.UNKNOWN.getCode(), 0.0, false, false, Map.of());
        }

        // 1. 关键词匹配
        for (var entry : KEYWORD_INTENT.entrySet()) {
            if (input.contains(entry.getKey())) {
                AgentIntent intent = entry.getValue();
                return buildResult(intent, input);
            }
        }

        // 2. 判断是否需要 RAG（包含"政策"、"规则"、"怎么"等词）
        boolean needRag = input.contains("政策") || input.contains("规则") || input.contains("怎么")
                || input.contains("什么") || input.contains("条件") || input.contains("标准");

        // 3. 默认 UNKNOWN
        return new IntentResult(AgentIntent.UNKNOWN.getCode(), 0.5, needRag, false, Map.of());
    }

    private IntentResult buildResult(AgentIntent intent, String input) {
        boolean needRag = intent == AgentIntent.AFTER_SALES_POLICY_QA
                || intent == AgentIntent.CUSTOMER_SERVICE_ASSISTANT
                || intent == AgentIntent.COMPLAINT_ANALYSIS;
        boolean needTool = intent != AgentIntent.AFTER_SALES_POLICY_QA;

        Map<String, Object> entities = new LinkedHashMap<>();
        // 提取订单号 (O 开头后跟数字)
        if (input.matches(".*O\\d{12}.*")) {
            String orderNo = input.replaceAll(".*(O\\d{12}).*", "$1");
            entities.put("orderNo", orderNo);
        }
        if (input.contains("退货退款")) entities.put("afterSalesType", "RETURN_REFUND");
        if (input.contains("仅退款")) entities.put("afterSalesType", "REFUND_ONLY");
        if (input.contains("换货")) entities.put("afterSalesType", "EXCHANGE");

        return new IntentResult(intent.getCode(), 0.85, needRag, needTool, entities);
    }

    /** 意图识别结果 */
    public record IntentResult(String intent, double confidence, boolean needRag,
                                boolean needTool, Map<String, Object> entities) {}
}
