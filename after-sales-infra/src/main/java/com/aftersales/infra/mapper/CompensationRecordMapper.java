package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.CompensationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 补偿记录 Mapper。
 */
@Mapper
public interface CompensationRecordMapper {

    CompensationRecord selectByCompensationNo(@Param("compensationNo") String compensationNo);

    CompensationRecord selectByAfterSalesId(@Param("afterSalesId") Long afterSalesId);

    int insert(CompensationRecord compensationRecord);

    int updateStatus(@Param("id") Long id,
                     @Param("compensationStatus") String compensationStatus,
                     @Param("externalGrantNo") String externalGrantNo,
                     @Param("failureReason") String failureReason);
}
