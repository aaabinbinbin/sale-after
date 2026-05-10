package com.aftersales.common.enums;

/**
 * 知识文档类型枚举。
 */
public enum KnowledgeDocType {

    POLICY("POLICY", "售后政策"),
    FAQ("FAQ", "常见问题"),
    CASE("CASE", "历史案例"),
    MANUAL("MANUAL", "客服处理手册"),
    SCRIPT("SCRIPT", "标准话术"),
    ;

    private final String code;
    private final String description;

    KnowledgeDocType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static KnowledgeDocType fromCode(String code) {
        for (KnowledgeDocType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return null;
    }
}
