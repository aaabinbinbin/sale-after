package com.aftersales.common.enums;

/**
 * 支付状态枚举。
 */
public enum PaymentStatus {

    PENDING("PENDING", "待支付"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败"),
    REFUNDED("REFUNDED", "已退款"),
    CLOSED("CLOSED", "已关闭"),
    ;

    private final String code;
    private final String description;

    PaymentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
