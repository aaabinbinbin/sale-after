package com.aftersales.common.enums;

/**
 * 换货状态枚举。
 */
public enum ExchangeStatus {

    CREATED("CREATED", "已创建"),
    STOCK_LOCKED("STOCK_LOCKED", "库存已锁定"),
    WAIT_RETURN("WAIT_RETURN", "等待用户退回"),
    RETURNED("RETURNED", "用户已退回"),
    SHIPPED("SHIPPED", "换货已发出"),
    COMPLETED("COMPLETED", "换货完成"),
    ;

    private final String code;
    private final String description;

    ExchangeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static ExchangeStatus fromCode(String code) {
        for (ExchangeStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
