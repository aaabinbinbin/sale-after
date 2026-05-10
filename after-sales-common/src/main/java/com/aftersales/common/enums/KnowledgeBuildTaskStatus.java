package com.aftersales.common.enums;

/**
 * 知识构建任务状态枚举。
 */
public enum KnowledgeBuildTaskStatus {

    CREATED("CREATED", "已创建"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "构建成功"),
    FAILED("FAILED", "构建失败"),
    ;

    private final String code;
    private final String description;

    KnowledgeBuildTaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static KnowledgeBuildTaskStatus fromCode(String code) {
        for (KnowledgeBuildTaskStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
