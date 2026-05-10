package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.SkuStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SKU库存 Mapper。
 */
@Mapper
public interface SkuStockMapper {

    SkuStock selectBySkuId(@Param("skuId") Long skuId);

    /** 扣减库存（乐观锁） */
    int decreaseStock(@Param("id") Long id,
                      @Param("quantity") int quantity,
                      @Param("version") Long version);
}
