package com.aftersales.common.enums;

/**
 * 补偿状态枚举。
 */
public enum CompensationStatus {

    CREATED("CREATED", "已创建"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "发放成功"),
    FAILED("FAILED", "发放失败"),
    ;

    private final String code;
    private final String description;

    CompensationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static CompensationStatus fromCode(String code) {
        for (CompensationStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
