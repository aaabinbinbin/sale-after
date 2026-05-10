package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.AfterSalesOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 售后操作日志 Mapper。只追加，不更新。
 */
@Mapper
public interface AfterSalesOperationLogMapper {

    int insert(AfterSalesOperationLog log);

    List<AfterSalesOperationLog> selectByAfterSalesId(@Param("afterSalesId") Long afterSalesId);

    List<AfterSalesOperationLog> selectByAfterSalesNo(@Param("afterSalesNo") String afterSalesNo);
}
