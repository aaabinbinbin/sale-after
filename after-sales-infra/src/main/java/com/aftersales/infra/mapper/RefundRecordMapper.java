package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 退款记录 Mapper。
 */
@Mapper
public interface RefundRecordMapper {

    RefundRecord selectByRefundNo(@Param("refundNo") String refundNo);

    RefundRecord selectByAfterSalesId(@Param("afterSalesId") Long afterSalesId);

    int insert(RefundRecord refundRecord);

    int updateStatus(@Param("id") Long id,
                     @Param("refundStatus") String refundStatus,
                     @Param("externalRefundNo") String externalRefundNo,
                     @Param("failureReason") String failureReason);
}
