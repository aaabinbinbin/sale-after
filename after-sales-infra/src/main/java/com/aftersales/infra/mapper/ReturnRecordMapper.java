package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.ReturnRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 退货记录 Mapper。
 */
@Mapper
public interface ReturnRecordMapper {

    ReturnRecord selectByReturnNo(@Param("returnNo") String returnNo);

    ReturnRecord selectByAfterSalesId(@Param("afterSalesId") Long afterSalesId);

    int insert(ReturnRecord returnRecord);

    int updateLogistics(@Param("id") Long id,
                        @Param("logisticsCompany") String logisticsCompany,
                        @Param("logisticsNo") String logisticsNo,
                        @Param("returnStatus") String returnStatus);

    int updateReceived(@Param("id") Long id,
                       @Param("returnStatus") String returnStatus,
                       @Param("receiverId") Long receiverId,
                       @Param("receiverRemark") String receiverRemark);
}
