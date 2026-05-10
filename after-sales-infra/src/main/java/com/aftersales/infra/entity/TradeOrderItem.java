package com.aftersales.infra.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体。
 *
 * 保存商品名称/SKU名称快照，避免商品改名影响历史订单展示。
 * after_sales_status 用于快速判断是否已有售后处理中（不替代售后表查询）。
 */
public class TradeOrderItem {

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long productId;
    private Long skuId;
    private String productName; // 下单时商品名称快照
    private String skuName; // 下单时SKU名称快照
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal paidAmount; // 该订单项实付金额
    private BigDecimal refundableAmount; // 当前可退金额
    private String afterSalesStatus; // NONE / PROCESSING / DONE
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getRefundableAmount() { return refundableAmount; }
    public void setRefundableAmount(BigDecimal refundableAmount) { this.refundableAmount = refundableAmount; }

    public String getAfterSalesStatus() { return afterSalesStatus; }
    public void setAfterSalesStatus(String afterSalesStatus) { this.afterSalesStatus = afterSalesStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
