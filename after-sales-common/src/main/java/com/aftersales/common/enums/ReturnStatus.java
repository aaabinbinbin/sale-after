package com.aftersales.common.enums;

/**
 * 退货状态枚举。
 */
public enum ReturnStatus {

    CREATED("CREATED", "已创建"),
    WAIT_SHIPMENT("WAIT_SHIPMENT", "等待用户寄回"),
    SHIPPED("SHIPPED", "用户已寄出"),
    RECEIVED("RECEIVED", "商家已收货"),
    ;

    private final String code;
    private final String description;

    ReturnStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ReturnStatus fromCode(String code) {
        for (ReturnStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
