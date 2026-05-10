package com.aftersales.infra.entity;

import java.time.LocalDateTime;

/** 售后操作日志实体。操作日志只追加，不更新。 */
public class AfterSalesOperationLog {
    private Long id;
    private Long afterSalesId;
    private String afterSalesNo;
    private Long operatorId;
    private String operatorRole;
    private String operationType;
    private String fromStatus;
    private String toStatus;
    private String operationDetail;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }
    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorRole() { return operatorRole; }
    public void setOperatorRole(String operatorRole) { this.operatorRole = operatorRole; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getOperationDetail() { return operationDetail; }
    public void setOperationDetail(String operationDetail) { this.operationDetail = operationDetail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
