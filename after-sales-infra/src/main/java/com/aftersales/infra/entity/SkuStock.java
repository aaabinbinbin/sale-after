package com.aftersales.infra.entity;

import java.time.LocalDateTime;

/**
 * SKU库存实体。
 *
 * MySQL 保存最终库存事实（total_stock / available_stock / sold_stock）。
 * 换货短期锁定库存使用 Redis，不频繁更新此表。
 * version 乐观锁防止并发写库存。
 */
public class SkuStock {

    private Long id;
    private Long skuId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer soldStock;
    private Long version; // 乐观锁版本
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public Integer getTotalStock() { return totalStock; }
    public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }

    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }

    public Integer getSoldStock() { return soldStock; }
    public void setSoldStock(Integer soldStock) { this.soldStock = soldStock; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
