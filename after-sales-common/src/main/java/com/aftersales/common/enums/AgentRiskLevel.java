package com.aftersales.common.enums;

/**
 * Agent 风险等级枚举。
 *
 * 高风险动作必须生成 confirmToken 并等待用户/客服确认。
 */
public enum AgentRiskLevel {

    LOW("LOW", "低风险：普通问答、政策解释、进度查询"),
    MEDIUM("MEDIUM", "中风险：售后草稿、退款预估、库存检查"),
    HIGH("HIGH", "高风险：创建售后、审核通过、退款、补偿、换货发货"),
    ;

    private final String code;
    private final String description;

    AgentRiskLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static AgentRiskLevel fromCode(String code) {
        for (AgentRiskLevel l : values()) {
            if (l.code.equals(code)) return l;
        }
        return LOW;
    }

    /** 高风险必须确认 */
    public boolean requiresConfirmation() {
        return this == HIGH;
    }
}
