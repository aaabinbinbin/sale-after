package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 支付记录 Mapper。
 */
@Mapper
public interface PaymentRecordMapper {

    List<PaymentRecord> selectByOrderId(@Param("orderId") Long orderId);

    List<PaymentRecord> selectByOrderNo(@Param("orderNo") String orderNo);

    PaymentRecord selectByPaymentNo(@Param("paymentNo") String paymentNo);
}
