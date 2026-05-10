package com.aftersales.common.enums;

/**
 * 售后明细项状态枚举。
 */
public enum AfterSalesItemStatus {

    PENDING("PENDING", "待处理"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝"),
    REFUNDED("REFUNDED", "已退款"),
    RETURNED("RETURNED", "已退货"),
    EXCHANGED("EXCHANGED", "已换货"),
    ;

    private final String code;
    private final String description;

    AfterSalesItemStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static AfterSalesItemStatus fromCode(String code) {
        for (AfterSalesItemStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
