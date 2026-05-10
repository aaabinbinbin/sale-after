package com.aftersales.common.enums;

/**
 * Agent 意图枚举。
 *
 * Agent 根据用户输入识别意图后按需选择技能执行。
 */
public enum AgentIntent {

    AFTER_SALES_POLICY_QA("AFTER_SALES_POLICY_QA", "售后政策问答"),
    ORDER_AFTER_SALES_ELIGIBILITY("ORDER_AFTER_SALES_ELIGIBILITY", "售后资格判断"),
    CREATE_AFTER_SALES_APPLICATION("CREATE_AFTER_SALES_APPLICATION", "创建售后申请"),
    QUERY_AFTER_SALES_PROGRESS("QUERY_AFTER_SALES_PROGRESS", "查询售后进度"),
    SUPPLEMENT_AFTER_SALES_PROOF("SUPPLEMENT_AFTER_SALES_PROOF", "补充售后凭证"),
    CUSTOMER_SERVICE_ASSISTANT("CUSTOMER_SERVICE_ASSISTANT", "客服辅助分析"),
    COMPLAINT_ANALYSIS("COMPLAINT_ANALYSIS", "投诉分析"),
    REFUND_ESTIMATION("REFUND_ESTIMATION", "退款预估"),
    EXCHANGE_STOCK_CHECK("EXCHANGE_STOCK_CHECK", "换货库存检查"),
    COMPENSATION_SUGGESTION("COMPENSATION_SUGGESTION", "补偿建议"),
    UNKNOWN("UNKNOWN", "未知意图"),
    ;

    private final String code;
    private final String description;

    AgentIntent(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static AgentIntent fromCode(String code) {
        for (AgentIntent i : values()) {
            if (i.code.equals(code)) return i;
        }
        return UNKNOWN;
    }
}
