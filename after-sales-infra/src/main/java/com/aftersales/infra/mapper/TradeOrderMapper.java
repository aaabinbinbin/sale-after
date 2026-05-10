package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 交易订单 Mapper。
 */
@Mapper
public interface TradeOrderMapper {

    TradeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    TradeOrder selectById(@Param("id") Long id);

    List<TradeOrder> selectByUserId(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    long countByUserId(@Param("userId") Long userId);

    List<TradeOrder> selectAll(@Param("offset") int offset, @Param("limit") int limit);

    long countAll();

    int insert(TradeOrder tradeOrder);

    int updateStatus(@Param("id") Long id, @Param("orderStatus") String orderStatus);
}
