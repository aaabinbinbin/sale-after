package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.enums.CompensationStatus;
import com.aftersales.common.enums.CompensationType;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.domain.statemachine.AfterSalesStateMachine;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.entity.CompensationRecord;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import com.aftersales.infra.mapper.CompensationRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 补偿服务。
 *
 * 补偿是高风险动作，必须幂等，必须保存 external_grant_no。
 * 流程：当前状态 → COMPENSATION_PROCESSING（更新DB）→ 发放 → COMPLETED（更新DB）
 */
@Service
public class CompensationService {

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final CompensationRecordMapper compensationRecordMapper;
    private final AfterSalesStateMachine stateMachine;
    private final OperationLogService operationLogService;
    private final IdempotencyService idempotencyService;

    public CompensationService(AfterSalesOrderMapper afterSalesOrderMapper,
                                CompensationRecordMapper compensationRecordMapper,
                                AfterSalesStateMachine stateMachine,
                                OperationLogService operationLogService,
                                IdempotencyService idempotencyService) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.compensationRecordMapper = compensationRecordMapper;
        this.stateMachine = stateMachine;
        this.operationLogService = operationLogService;
        this.idempotencyService = idempotencyService;
    }

    /** 发放补偿 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> grant(String afterSalesNo, String idempotencyKey, Map<String, Object> command) {
        // 1. 幂等
        String historyResp = idempotencyService.checkOrRecord(idempotencyKey, command, "GRANT_COMPENSATION");
        if (historyResp != null) return JsonUtils.fromJson(historyResp, Map.class);

        // 2. 权限
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role)) throw new BusinessException(ErrorCode.FORBIDDEN);

        // 3. 查询
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        // 4. 防重复
        CompensationRecord existing = compensationRecordMapper.selectByAfterSalesId(asOrder.getId());
        if (existing != null && CompensationStatus.SUCCESS.getCode().equals(existing.getCompensationStatus())) {
            throw new BusinessException(ErrorCode.COMPENSATION_ALREADY_GRANTED);
        }

        String compensationType = (String) command.get("compensationType");
        BigDecimal amount = toBigDecimal(command.get("compensationAmount"));
        Long version = toLong(command.get("version"));

        // 5. 状态机校验 + 更新到 COMPENSATION_PROCESSING
        String oldStatus = asOrder.getStatus();
        stateMachine.checkTransition(AfterSalesStatus.fromCode(oldStatus), AfterSalesStatus.COMPENSATION_PROCESSING);
        int rows = afterSalesOrderMapper.updateStatusWithVersion(asOrder.getId(),
                AfterSalesStatus.COMPENSATION_PROCESSING.getCode(), oldStatus, version);
        if (rows == 0) throw new BusinessException(ErrorCode.AFTER_SALES_VERSION_CONFLICT);
        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "START_COMPENSATION", oldStatus, AfterSalesStatus.COMPENSATION_PROCESSING.getCode(),
                "开始补偿处理：" + compensationType + " " + amount);

        // 6. 模拟外部发放，生成 external_grant_no
        CompensationType ct = CompensationType.fromCode(compensationType);
        String externalGrantNo = (ct != null ? ct.name() : compensationType) + "_GRANT_" + System.currentTimeMillis();

        // 7. 创建补偿记录
        CompensationRecord cr;
        if (existing != null) {
            cr = existing;
        } else {
            cr = new CompensationRecord();
            cr.setCompensationNo(IdGenerator.genCompensationNo());
            cr.setAfterSalesId(asOrder.getId());
            cr.setAfterSalesNo(asOrder.getAfterSalesNo());
            cr.setCompensationType(compensationType);
            cr.setCompensationAmount(amount);
            cr.setCompensationStatus(CompensationStatus.PROCESSING.getCode());
            compensationRecordMapper.insert(cr);
        }
        compensationRecordMapper.updateStatus(cr.getId(), CompensationStatus.SUCCESS.getCode(), externalGrantNo, null);

        // 8. 完成
        stateMachine.checkTransition(AfterSalesStatus.COMPENSATION_PROCESSING, AfterSalesStatus.COMPLETED);
        afterSalesOrderMapper.updateComplete(asOrder.getId(),
                AfterSalesStatus.COMPLETED.getCode(), AfterSalesStatus.COMPENSATION_PROCESSING.getCode(), version + 1);

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "COMPENSATION_GRANTED", AfterSalesStatus.COMPENSATION_PROCESSING.getCode(),
                AfterSalesStatus.COMPLETED.getCode(),
                "补偿发放成功：" + compensationType + " 金额:" + amount + " 外部流水:" + externalGrantNo);

        // 9. 返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("afterSalesNo", asOrder.getAfterSalesNo());
        result.put("compensationNo", cr.getCompensationNo());
        result.put("externalGrantNo", externalGrantNo);
        result.put("status", AfterSalesStatus.COMPLETED.getCode());
        String respJson = JsonUtils.toJson(result);
        idempotencyService.markSuccess(idempotencyKey, respJson, afterSalesNo);
        return result;
    }

    public CompensationRecord getCompensation(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        return compensationRecordMapper.selectByAfterSalesId(asOrder.getId());
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        return obj instanceof Number n ? n.longValue() : Long.valueOf(obj.toString());
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        return obj instanceof BigDecimal bd ? bd : new BigDecimal(obj.toString());
    }
}
