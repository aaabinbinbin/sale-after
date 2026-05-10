package com.aftersales.infra.mapper;

import com.aftersales.infra.entity.AfterSalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 售后主单 Mapper。
 *
 * 状态更新必须带 version 和 expectedStatus 实现乐观锁。
 */
@Mapper
public interface AfterSalesOrderMapper {

    AfterSalesOrder selectByAfterSalesNo(@Param("afterSalesNo") String afterSalesNo);

    AfterSalesOrder selectById(@Param("id") Long id);

    List<AfterSalesOrder> selectByUserId(@Param("userId") Long userId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    long countByUserId(@Param("userId") Long userId);

    List<AfterSalesOrder> selectByCondition(@Param("status") String status,
                                             @Param("afterSalesType") String afterSalesType,
                                             @Param("orderNo") String orderNo,
                                             @Param("userId") Long userId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    long countByCondition(@Param("status") String status,
                          @Param("afterSalesType") String afterSalesType,
                          @Param("orderNo") String orderNo,
                          @Param("userId") Long userId);

    int insert(AfterSalesOrder afterSalesOrder);

    /**
     * 带乐观锁的状态更新。
     *
     * @return 更新行数，0 表示 version 或 expectedStatus 不匹配（并发冲突）
     */
    int updateStatusWithVersion(@Param("id") Long id,
                                 @Param("newStatus") String newStatus,
                                 @Param("expectedStatus") String expectedStatus,
                                 @Param("version") Long version);

    /** 更新审核信息 */
    int updateReviewInfo(@Param("id") Long id,
                          @Param("reviewerId") Long reviewerId,
                          @Param("reviewRemark") String reviewRemark,
                          @Param("approvedAmount") BigDecimal approvedAmount,
                          @Param("newStatus") String newStatus,
                          @Param("expectedStatus") String expectedStatus,
                          @Param("version") Long version);

    /** 标记售后完成 */
    int updateComplete(@Param("id") Long id,
                       @Param("newStatus") String newStatus,
                       @Param("expectedStatus") String expectedStatus,
                       @Param("version") Long version);
}
