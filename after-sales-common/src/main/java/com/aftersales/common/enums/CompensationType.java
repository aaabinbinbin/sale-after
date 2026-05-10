package com.aftersales.common.enums;

/**
 * 补偿类型枚举。
 */
public enum CompensationType {

    COUPON("COUPON", "优惠券"),
    POINTS("POINTS", "积分"),
    BALANCE("BALANCE", "余额"),
    MANUAL("MANUAL", "人工补偿"),
    ;

    private final String code;
    private final String description;

    CompensationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static CompensationType fromCode(String code) {
        for (CompensationType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return null;
    }
}
