package com.aftersales.common.enums;

/**
 * 退款状态枚举。
 */
public enum RefundStatus {

    CREATED("CREATED", "已创建"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "退款成功"),
    FAILED("FAILED", "退款失败"),
    ;

    private final String code;
    private final String description;

    RefundStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static RefundStatus fromCode(String code) {
        for (RefundStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
