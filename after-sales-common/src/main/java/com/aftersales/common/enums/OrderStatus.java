package com.aftersales.common.enums;

/**
 * 订单状态枚举。
 */
public enum OrderStatus {

    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),
    PAID("PAID", "已支付"),
    SHIPPED("SHIPPED", "已发货"),
    DELIVERED("DELIVERED", "已签收"),
    COMPLETED("COMPLETED", "已完成"),
    CLOSED("CLOSED", "已关闭"),
    ;

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    /** 是否允许发起售后 */
    public boolean allowsAfterSales() {
        return this == PAID || this == SHIPPED || this == DELIVERED || this == COMPLETED;
    }
}
