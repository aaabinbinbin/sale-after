package com.aftersales.common.enums;

/**
 * 用户角色枚举。
 */
public enum UserRole {

    USER("USER", "普通用户"),
    CUSTOMER_SERVICE("CUSTOMER_SERVICE", "客服"),
    ADMIN("ADMIN", "管理员"),
    ;

    private final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static UserRole fromCode(String code) {
        for (UserRole r : values()) {
            if (r.code.equals(code)) return r;
        }
        return null;
    }
}
