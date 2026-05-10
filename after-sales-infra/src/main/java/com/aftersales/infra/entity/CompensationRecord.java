package com.aftersales.infra.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 补偿记录实体。
 *
 * external_grant_no 必须保存，用于追踪外部系统发放结果。
 */
public class CompensationRecord {

    private Long id;
    private String compensationNo;
    private Long afterSalesId;
    private String afterSalesNo;
    private String compensationType; // COUPON / POINTS / BALANCE / MANUAL
    private BigDecimal compensationAmount;
    private String compensationStatus;
    private String externalGrantNo; // 外部发放流水号（必须保留）
    private String failureReason;
    private LocalDateTime grantedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompensationNo() { return compensationNo; }
    public void setCompensationNo(String compensationNo) { this.compensationNo = compensationNo; }

    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }

    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }

    public String getCompensationType() { return compensationType; }
    public void setCompensationType(String compensationType) { this.compensationType = compensationType; }

    public BigDecimal getCompensationAmount() { return compensationAmount; }
    public void setCompensationAmount(BigDecimal compensationAmount) { this.compensationAmount = compensationAmount; }

    public String getCompensationStatus() { return compensationStatus; }
    public void setCompensationStatus(String compensationStatus) { this.compensationStatus = compensationStatus; }

    public String getExternalGrantNo() { return externalGrantNo; }
    public void setExternalGrantNo(String externalGrantNo) { this.externalGrantNo = externalGrantNo; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
