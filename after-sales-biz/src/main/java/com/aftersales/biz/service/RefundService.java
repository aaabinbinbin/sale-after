package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.enums.RefundStatus;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.domain.event.AfterSalesCompletedEvent;
import com.aftersales.domain.statemachine.AfterSalesStateMachine;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.entity.RefundRecord;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import com.aftersales.infra.mapper.RefundRecordMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 退款服务。退款是高风险动作，必须幂等、防重复、校验金额上限。
 *
 * 正确流程：当前状态 → REFUND_PROCESSING(更新DB+乐观锁) → 执行退款 → COMPLETED
 */
@Service
public class RefundService {

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final AfterSalesStateMachine stateMachine;
    private final OperationLogService operationLogService;
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher eventPublisher;

    public RefundService(AfterSalesOrderMapper afterSalesOrderMapper,
                          RefundRecordMapper refundRecordMapper,
                          AfterSalesStateMachine stateMachine,
                          OperationLogService operationLogService,
                          IdempotencyService idempotencyService,
                          ApplicationEventPublisher eventPublisher) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.stateMachine = stateMachine;
        this.operationLogService = operationLogService;
        this.idempotencyService = idempotencyService;
        this.eventPublisher = eventPublisher;
    }

    /** 执行退款 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeRefund(String afterSalesNo, String idempotencyKey, Map<String, Object> command) {
        // 1. 幂等
        String historyResp = idempotencyService.checkOrRecord(idempotencyKey, command, "EXECUTE_REFUND");
        if (historyResp != null) return JsonUtils.fromJson(historyResp, Map.class);

        // 2. 权限
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role))
            throw new BusinessException(ErrorCode.FORBIDDEN);

        // 3. 查询售后单
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);

        BigDecimal refundAmount = toBigDecimal(command.get("refundAmount"));
        Long version = toLong(command.get("version"));

        // 4. 防重复退款
        RefundRecord existingRefund = refundRecordMapper.selectByAfterSalesId(asOrder.getId());
        if (existingRefund != null && RefundStatus.SUCCESS.getCode().equals(existingRefund.getRefundStatus()))
            throw new BusinessException(ErrorCode.REFUND_ALREADY_EXECUTED);

        // 5. 金额校验
        BigDecimal maxRefund = asOrder.getApprovedAmount() != null
                && asOrder.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0
                ? asOrder.getApprovedAmount() : asOrder.getApplyAmount();
        if (refundAmount.compareTo(maxRefund) > 0)
            throw new BusinessException(ErrorCode.REFUND_EXCEED_AMOUNT);

        // 6. 状态机 + 更新到 REFUND_PROCESSING(乐观锁)
        String oldStatus = asOrder.getStatus();
        stateMachine.checkTransition(AfterSalesStatus.fromCode(oldStatus), AfterSalesStatus.REFUND_PROCESSING);
        int rows = afterSalesOrderMapper.updateStatusWithVersion(asOrder.getId(),
                AfterSalesStatus.REFUND_PROCESSING.getCode(), oldStatus, version);
        if (rows == 0) throw new BusinessException(ErrorCode.AFTER_SALES_VERSION_CONFLICT);
        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "START_REFUND", oldStatus, AfterSalesStatus.REFUND_PROCESSING.getCode(), "开始退款");

        // 7. 创建或更新退款记录
        String refundNo = IdGenerator.genRefundNo();
        Long refundRecordId;
        if (existingRefund == null) {
            RefundRecord rr = new RefundRecord();
            rr.setRefundNo(refundNo);
            rr.setAfterSalesId(asOrder.getId());
            rr.setAfterSalesNo(asOrder.getAfterSalesNo());
            rr.setOrderId(asOrder.getOrderId());
            rr.setOrderNo(asOrder.getOrderNo());
            rr.setRefundAmount(refundAmount);
            rr.setRefundStatus(RefundStatus.PROCESSING.getCode());
            rr.setRefundChannel("ORIGINAL");
            refundRecordMapper.insert(rr);
            refundRecordId = rr.getId(); // MyBatis 回填自增 ID
        } else {
            refundRecordId = existingRefund.getId();
        }

        // 8. 模拟外部退款
        String externalRefundNo = "EXT_REF_" + System.currentTimeMillis();
        refundRecordMapper.updateStatus(refundRecordId, RefundStatus.SUCCESS.getCode(), externalRefundNo, null);

        // 9. 完成
        stateMachine.checkTransition(AfterSalesStatus.REFUND_PROCESSING, AfterSalesStatus.COMPLETED);
        afterSalesOrderMapper.updateComplete(asOrder.getId(),
                AfterSalesStatus.COMPLETED.getCode(), AfterSalesStatus.REFUND_PROCESSING.getCode(), version + 1);
        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "REFUND_SUCCESS", AfterSalesStatus.REFUND_PROCESSING.getCode(), AfterSalesStatus.COMPLETED.getCode(),
                "退款成功 金额:" + refundAmount);

        // 10. 发布售后完成事件 → 触发异步知识构建
        eventPublisher.publishEvent(new AfterSalesCompletedEvent(
                asOrder.getAfterSalesNo(), asOrder.getId(), asOrder.getAfterSalesType(), asOrder.getOrderNo()));

        // 11. 幂等完成
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("afterSalesNo", asOrder.getAfterSalesNo());
        result.put("refundNo", existingRefund != null ? existingRefund.getRefundNo() : refundNo);
        result.put("refundAmount", refundAmount);
        result.put("status", AfterSalesStatus.COMPLETED.getCode());
        idempotencyService.markSuccess(idempotencyKey, JsonUtils.toJson(result), afterSalesNo);
        return result;
    }

    public RefundRecord getRefund(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        return refundRecordMapper.selectByAfterSalesId(asOrder.getId());
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
