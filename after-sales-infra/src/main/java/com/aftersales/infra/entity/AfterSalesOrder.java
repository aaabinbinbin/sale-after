package com.aftersales.infra.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后主单实体。
 *
 * 一个售后主单只处理一种售后类型（after_sales_type）。
 * 多商品通过 after_sales_item 表关联。
 * version 字段用于乐观锁，防止并发状态覆盖。
 */
public class AfterSalesOrder {

    private Long id;
    private String afterSalesNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String afterSalesType; // REFUND_ONLY / RETURN_REFUND / EXCHANGE / COMPENSATION
    private String status; // 售后状态，流转必须经过状态机
    private String reasonCode;
    private String reasonText;
    private BigDecimal applyAmount;
    private BigDecimal approvedAmount;
    private String applicantRemark;
    private Long reviewerId;
    private String reviewRemark;
    private LocalDateTime reviewedAt;
    private LocalDateTime completedAt;
    private Long version; // 乐观锁版本号
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAfterSalesType() { return afterSalesType; }
    public void setAfterSalesType(String afterSalesType) { this.afterSalesType = afterSalesType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }

    public BigDecimal getApplyAmount() { return applyAmount; }
    public void setApplyAmount(BigDecimal applyAmount) { this.applyAmount = applyAmount; }

    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }

    public String getApplicantRemark() { return applicantRemark; }
    public void setApplicantRemark(String applicantRemark) { this.applicantRemark = applicantRemark; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
