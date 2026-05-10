package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.TradeOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单项 Mapper。
 */
@Mapper
public interface TradeOrderItemMapper {

    List<TradeOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    List<TradeOrderItem> selectByOrderNo(@Param("orderNo") String orderNo);

    TradeOrderItem selectById(@Param("id") Long id);

    int updateAfterSalesStatus(@Param("id") Long id,
                               @Param("afterSalesStatus") String afterSalesStatus,
                               @Param("expectedStatus") String expectedStatus);

    int updateRefundableAmount(@Param("id") Long id,
                               @Param("refundableAmount") java.math.BigDecimal refundableAmount);
}
