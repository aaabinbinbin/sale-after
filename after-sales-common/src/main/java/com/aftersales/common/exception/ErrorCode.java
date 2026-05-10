package com.aftersales.common.exception;

/**
 * 业务错误码枚举。
 *
 * 所有业务异常统一使用错误码，避免散落字符串。
 * 编码规则：模块缩写 + 错误序号。
 */
public enum ErrorCode {

    // ========== 通用 ==========
    SUCCESS("SUCCESS", "成功"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常"),
    PARAM_INVALID("PARAM_INVALID", "参数校验失败"),
    UNAUTHORIZED("UNAUTHORIZED", "未登录或 Token 无效"),
    FORBIDDEN("FORBIDDEN", "无权限执行该操作"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),

    // ========== 幂等 ==========
    IDEMPOTENT_PROCESSING("IDEMPOTENT_PROCESSING", "请求处理中，请勿重复提交"),
    IDEMPOTENT_KEY_MISMATCH("IDEMPOTENT_KEY_MISMATCH", "相同幂等Key但请求内容不一致"),
    IDEMPOTENT_MISSING_KEY("IDEMPOTENT_MISSING_KEY", "缺少 Idempotency-Key 请求头"),

    // ========== 认证 ==========
    AUTH_LOGIN_FAILED("AUTH_LOGIN_FAILED", "用户名或密码错误"),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "Token 已过期"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "Token 无效"),

    // ========== 订单 ==========
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "订单不存在"),
    ORDER_NOT_BELONG_TO_USER("ORDER_NOT_BELONG_TO_USER", "订单不属于当前用户"),
    ORDER_STATUS_NOT_ALLOW_AFTER_SALES("ORDER_STATUS_NOT_ALLOW_AFTER_SALES", "订单状态不允许售后"),

    // ========== 售后 ==========
    AFTER_SALES_NOT_FOUND("AFTER_SALES_NOT_FOUND", "售后单不存在"),
    AFTER_SALES_STATUS_INVALID("AFTER_SALES_STATUS_INVALID", "售后单当前状态不允许该操作"),
    AFTER_SALES_VERSION_CONFLICT("AFTER_SALES_VERSION_CONFLICT", "售后单已被他人修改，请刷新后重试"),
    AFTER_SALES_DUPLICATE("AFTER_SALES_DUPLICATE", "该订单项已有售后处理中"),
    AFTER_SALES_EXCEED_QUANTITY("AFTER_SALES_EXCEED_QUANTITY", "申请数量超过可售后数量"),
    AFTER_SALES_EXCEED_AMOUNT("AFTER_SALES_EXCEED_AMOUNT", "申请金额超过可退金额"),
    AFTER_SALES_OUT_OF_WINDOW("AFTER_SALES_OUT_OF_WINDOW", "已超出售后申请窗口期"),
    AFTER_SALES_ITEM_NOT_IN_ORDER("AFTER_SALES_ITEM_NOT_IN_ORDER", "订单项不属于该订单"),

    // ========== 退款 ==========
    REFUND_EXCEED_AMOUNT("REFUND_EXCEED_AMOUNT", "退款金额超过可退金额"),
    REFUND_ALREADY_EXECUTED("REFUND_ALREADY_EXECUTED", "退款已执行，不可重复退款"),
    REFUND_FAILED("REFUND_FAILED", "退款执行失败"),

    // ========== 换货 ==========
    EXCHANGE_STOCK_NOT_ENOUGH("EXCHANGE_STOCK_NOT_ENOUGH", "换货目标 SKU 库存不足"),
    EXCHANGE_LOCK_FAILED("EXCHANGE_LOCK_FAILED", "换货库存锁定失败"),
    EXCHANGE_NOT_LOCKED("EXCHANGE_NOT_LOCKED", "换货库存未锁定，无法发货"),

    // ========== 补偿 ==========
    COMPENSATION_ALREADY_GRANTED("COMPENSATION_ALREADY_GRANTED", "补偿已发放，不可重复发放"),
    COMPENSATION_FAILED("COMPENSATION_FAILED", "补偿发放失败"),

    // ========== Agent ==========
    AGENT_CONFIRM_TOKEN_EXPIRED("AGENT_CONFIRM_TOKEN_EXPIRED", "确认 Token 已过期"),
    AGENT_CONFIRM_TOKEN_INVALID("AGENT_CONFIRM_TOKEN_INVALID", "确认 Token 无效"),
    AGENT_RISK_ACTION_NOT_CONFIRMED("AGENT_RISK_ACTION_NOT_CONFIRMED", "高风险动作需要显式确认"),
    AGENT_SKILL_NOT_FOUND("AGENT_SKILL_NOT_FOUND", "Agent 技能未找到"),

    // ========== RAG ==========
    RAG_VECTOR_STORE_NOT_READY("RAG_VECTOR_STORE_NOT_READY", "向量库未就绪"),
    RAG_BUILD_TASK_FAILED("RAG_BUILD_TASK_FAILED", "知识构建任务失败"),
    ;

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
