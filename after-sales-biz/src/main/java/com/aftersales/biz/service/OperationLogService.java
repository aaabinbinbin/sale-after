package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.infra.entity.AfterSalesOperationLog;
import com.aftersales.infra.mapper.AfterSalesOperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 操作日志服务。
 *
 * 关键状态变化时自动记录操作日志，只追加不更新。
 */
@Service
public class OperationLogService {

    private final AfterSalesOperationLogMapper logMapper;

    public OperationLogService(AfterSalesOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    /**
     * 记录操作日志。
     *
     * @param afterSalesId    售后主单 ID
     * @param afterSalesNo    售后单号
     * @param operationType   操作类型
     * @param fromStatus      操作前状态
     * @param toStatus        操作后状态
     * @param detail          操作详情
     */
    public void record(Long afterSalesId, String afterSalesNo, String operationType,
                       String fromStatus, String toStatus, Object detail) {
        AfterSalesOperationLog log = new AfterSalesOperationLog();
        log.setAfterSalesId(afterSalesId);
        log.setAfterSalesNo(afterSalesNo);
        log.setOperatorId(UserContext.getUserId());
        log.setOperatorRole(UserContext.getRole());
        log.setOperationType(operationType);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperationDetail(detail instanceof String ? (String) detail : JsonUtils.toJson(detail));
        logMapper.insert(log);
    }

    /** 查询售后操作日志 */
    public List<AfterSalesOperationLog> listByAfterSalesId(Long afterSalesId) {
        return logMapper.selectByAfterSalesId(afterSalesId);
    }
}
