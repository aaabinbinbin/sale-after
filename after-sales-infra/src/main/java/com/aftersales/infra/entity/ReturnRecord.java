package com.aftersales.infra.entity;

import java.time.LocalDateTime;

/** 退货记录实体 */
public class ReturnRecord {
    private Long id;
    private String returnNo;
    private Long afterSalesId;
    private String afterSalesNo;
    private String logisticsCompany;
    private String logisticsNo;
    private String returnStatus;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    private Long receiverId;
    private String receiverRemark;
    private LocalDateTime createdAt;
    private Long version; // 乐观锁版本
    private LocalDateTime updatedAt;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReturnNo() { return returnNo; }
    public void setReturnNo(String returnNo) { this.returnNo = returnNo; }
    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }
    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }
    public String getLogisticsCompany() { return logisticsCompany; }
    public void setLogisticsCompany(String logisticsCompany) { this.logisticsCompany = logisticsCompany; }
    public String getLogisticsNo() { return logisticsNo; }
    public void setLogisticsNo(String logisticsNo) { this.logisticsNo = logisticsNo; }
    public String getReturnStatus() { return returnStatus; }
    public void setReturnStatus(String returnStatus) { this.returnStatus = returnStatus; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getReceiverRemark() { return receiverRemark; }
    public void setReceiverRemark(String receiverRemark) { this.receiverRemark = receiverRemark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
