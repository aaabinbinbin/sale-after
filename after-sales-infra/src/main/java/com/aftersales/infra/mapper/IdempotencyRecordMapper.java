package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 幂等记录 Mapper。
 */
@Mapper
public interface IdempotencyRecordMapper {

    IdempotencyRecord selectByKey(@Param("idempotencyKey") String idempotencyKey);

    int insert(IdempotencyRecord record);

    int updateSuccess(@Param("id") Long id,
                      @Param("responseBody") String responseBody,
                      @Param("bizId") String bizId,
                      @Param("status") String status);

    int deleteExpired(@Param("now") java.time.LocalDateTime now);
}
