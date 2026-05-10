package com.aftersales.domain.event;

/**
 * 售后完成领域事件。
 *
 * 售后完成后发布此事件，触发异步知识构建任务。
 */
public class AfterSalesCompletedEvent {

    private final String afterSalesNo;
    private final Long afterSalesId;
    private final String afterSalesType;
    private final String orderNo;

    public AfterSalesCompletedEvent(String afterSalesNo, Long afterSalesId, String afterSalesType, String orderNo) {
        this.afterSalesNo = afterSalesNo;
        this.afterSalesId = afterSalesId;
        this.afterSalesType = afterSalesType;
        this.orderNo = orderNo;
    }

    public String getAfterSalesNo() { return afterSalesNo; }
    public Long getAfterSalesId() { return afterSalesId; }
    public String getAfterSalesType() { return afterSalesType; }
    public String getOrderNo() { return orderNo; }

    @Override
    public String toString() {
        return "AfterSalesCompletedEvent{afterSalesNo='" + afterSalesNo + "', type=" + afterSalesType + "}";
    }
}
