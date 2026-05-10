package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.ExchangeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 换货记录 Mapper。
 */
@Mapper
public interface ExchangeRecordMapper {

    ExchangeRecord selectByExchangeNo(@Param("exchangeNo") String exchangeNo);

    ExchangeRecord selectByAfterSalesId(@Param("afterSalesId") Long afterSalesId);

    int insert(ExchangeRecord exchangeRecord);

    int updateStockLocked(@Param("id") Long id, @Param("stockLocked") Boolean stockLocked);

    int updateShipped(@Param("id") Long id,
                      @Param("exchangeStatus") String exchangeStatus,
                      @Param("outboundLogisticsCompany") String outboundLogisticsCompany,
                      @Param("outboundLogisticsNo") String outboundLogisticsNo);
}
