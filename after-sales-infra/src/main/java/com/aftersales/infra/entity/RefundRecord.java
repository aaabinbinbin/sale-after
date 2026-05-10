package com.aftersales.infra.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体。
 *
 * 退款必须可追踪、可幂等、可重试。
 * external_refund_no 关联外部支付渠道退款流水。
 */
public class RefundRecord {

    private Long id;
    private String refundNo;
    private Long afterSalesId;
    private String afterSalesNo;
    private Long orderId;
    private String orderNo;
    private String paymentNo; // 原支付流水号
    private BigDecimal refundAmount;
    private String refundStatus;
    private String refundChannel;
    private String externalRefundNo; // 外部退款流水号
    private String failureReason;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }

    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }

    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }

    public String getRefundChannel() { return refundChannel; }
    public void setRefundChannel(String refundChannel) { this.refundChannel = refundChannel; }

    public String getExternalRefundNo() { return externalRefundNo; }
    public void setExternalRefundNo(String externalRefundNo) { this.externalRefundNo = externalRefundNo; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
