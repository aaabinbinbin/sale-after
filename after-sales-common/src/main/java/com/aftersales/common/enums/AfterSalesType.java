package com.aftersales.common.enums;

/**
 * 售后类型枚举。
 *
 * 一个售后主单只处理一种售后类型。
 */
public enum AfterSalesType {

    REFUND_ONLY("REFUND_ONLY", "仅退款"),
    RETURN_REFUND("RETURN_REFUND", "退货退款"),
    EXCHANGE("EXCHANGE", "换货"),
    COMPENSATION("COMPENSATION", "补偿"),
    ;

    private final String code;
    private final String description;

    AfterSalesType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static AfterSalesType fromCode(String code) {
        for (AfterSalesType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return null;
    }
}
