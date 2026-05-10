package com.aftersales.infra.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后明细项实体。
 *
 * 一个售后主单对应多个售后明细项，每个明细项关联一个订单项。
 * 实现"用户一次退多个商品"的业务需求。
 */
public class AfterSalesItem {

    private Long id;
    private Long afterSalesId;
    private String afterSalesNo;
    private Long orderItemId; // 关联原订单项
    private Long productId;
    private Long skuId;
    private String productName; // 商品名称快照
    private String skuName; // SKU名称快照
    private Integer applyQuantity;
    private Integer approvedQuantity;
    private BigDecimal refundableAmount; // 可退金额
    private BigDecimal applyAmount;
    private BigDecimal approvedAmount;
    private Long exchangeSkuId; // 换货目标SKU ID
    private String itemStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }

    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }

    public Integer getApplyQuantity() { return applyQuantity; }
    public void setApplyQuantity(Integer applyQuantity) { this.applyQuantity = applyQuantity; }

    public Integer getApprovedQuantity() { return approvedQuantity; }
    public void setApprovedQuantity(Integer approvedQuantity) { this.approvedQuantity = approvedQuantity; }

    public BigDecimal getRefundableAmount() { return refundableAmount; }
    public void setRefundableAmount(BigDecimal refundableAmount) { this.refundableAmount = refundableAmount; }

    public BigDecimal getApplyAmount() { return applyAmount; }
    public void setApplyAmount(BigDecimal applyAmount) { this.applyAmount = applyAmount; }

    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }

    public Long getExchangeSkuId() { return exchangeSkuId; }
    public void setExchangeSkuId(Long exchangeSkuId) { this.exchangeSkuId = exchangeSkuId; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
