package com.aftersales.infra.entity;

import java.time.LocalDateTime;

/** 换货记录实体。库存锁定细节在 Redis 处理。 */
public class ExchangeRecord {
    private Long id;
    private String exchangeNo;
    private Long afterSalesId;
    private String afterSalesNo;
    private String exchangeStatus;
    private String outboundLogisticsCompany;
    private String outboundLogisticsNo;
    private Boolean stockLocked; // 是否已锁定库存
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private Long version; // 乐观锁版本
    private LocalDateTime updatedAt;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExchangeNo() { return exchangeNo; }
    public void setExchangeNo(String exchangeNo) { this.exchangeNo = exchangeNo; }
    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }
    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }
    public String getExchangeStatus() { return exchangeStatus; }
    public void setExchangeStatus(String exchangeStatus) { this.exchangeStatus = exchangeStatus; }
    public String getOutboundLogisticsCompany() { return outboundLogisticsCompany; }
    public void setOutboundLogisticsCompany(String outboundLogisticsCompany) { this.outboundLogisticsCompany = outboundLogisticsCompany; }
    public String getOutboundLogisticsNo() { return outboundLogisticsNo; }
    public void setOutboundLogisticsNo(String outboundLogisticsNo) { this.outboundLogisticsNo = outboundLogisticsNo; }
    public Boolean getStockLocked() { return stockLocked; }
    public void setStockLocked(Boolean stockLocked) { this.stockLocked = stockLocked; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
