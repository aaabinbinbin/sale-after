package com.aftersales.common.enums;

/**
 * 售后主单状态枚举。
 *
 * 状态流转必须经过 AfterSalesStateMachine 校验。
 */
public enum AfterSalesStatus {

    CREATED("CREATED", "已创建"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    NEED_MORE_INFO("NEED_MORE_INFO", "需补充信息"),
    REJECTED("REJECTED", "审核拒绝"),
    APPROVED("APPROVED", "审核通过"),
    WAIT_RETURN_SHIPMENT("WAIT_RETURN_SHIPMENT", "等待用户退货"),
    WAIT_RETURN_RECEIVE("WAIT_RETURN_RECEIVE", "等待商家收货"),
    REFUND_PROCESSING("REFUND_PROCESSING", "退款处理中"),
    EXCHANGE_PROCESSING("EXCHANGE_PROCESSING", "换货处理中"),
    COMPENSATION_PROCESSING("COMPENSATION_PROCESSING", "补偿处理中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消"),
    FAILED("FAILED", "处理失败"),
    ;

    private final String code;
    private final String description;

    AfterSalesStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static AfterSalesStatus fromCode(String code) {
        for (AfterSalesStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    /** 是否为终态 */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == REJECTED || this == FAILED;
    }
}
