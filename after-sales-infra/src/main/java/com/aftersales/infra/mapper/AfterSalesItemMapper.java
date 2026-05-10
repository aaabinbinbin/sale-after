package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.AfterSalesItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 售后明细项 Mapper。
 */
@Mapper
public interface AfterSalesItemMapper {

    List<AfterSalesItem> selectByAfterSalesId(@Param("afterSalesId") Long afterSalesId);

    List<AfterSalesItem> selectByAfterSalesNo(@Param("afterSalesNo") String afterSalesNo);

    int insert(AfterSalesItem afterSalesItem);

    int updateApprovedInfo(@Param("id") Long id,
                            @Param("approvedQuantity") Integer approvedQuantity,
                            @Param("approvedAmount") java.math.BigDecimal approvedAmount,
                            @Param("itemStatus") String itemStatus);
}
