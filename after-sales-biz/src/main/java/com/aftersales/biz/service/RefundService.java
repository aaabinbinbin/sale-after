package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.enums.RefundStatus;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.domain.statemachine.AfterSalesStateMachine;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.entity.RefundRecord;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import com.aftersales.infra.mapper.RefundRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 退款服务。
 *
 * 退款是高风险动作，必须幂等、必须防重复退款、必须校验金额上限。
 */
@Service
public class RefundService {

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final AfterSalesStateMachine stateMachine;
    private final OperationLogService operationLogService;
    private final IdempotencyService idempotencyService;

    public RefundService(AfterSalesOrderMapper afterSalesOrderMapper,
                          RefundRecordMapper refundRecordMapper,
                          AfterSalesStateMachine stateMachine,
                          OperationLogService operationLogService,
                          IdempotencyService idempotencyService) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.stateMachine = stateMachine;
        this.operationLogService = operationLogService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * 执行退款。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeRefund(String afterSalesNo, String idempotencyKey, Map<String, Object> command) {
        // 幂等校验
        String historyResp = idempotencyService.checkOrRecord(idempotencyKey, command, "EXECUTE_REFUND");
        if (historyResp != null) {
            return JsonUtils.fromJson(historyResp, Map.class);
        }

        // 权限：只有客服/管理员可以执行退款
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) {
            throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        }

        BigDecimal refundAmount = toBigDecimal(command.get("refundAmount"));
        Long version = toLong(command.get("version"));

        // 检查是否已有退款记录
        RefundRecord existing = refundRecordMapper.selectByAfterSalesId(asOrder.getId());
        if (existing != null && RefundStatus.SUCCESS.getCode().equals(existing.getRefundStatus())) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_EXECUTED);
        }

        // 校验退款金额不超过订单实付
        if (refundAmount.compareTo(asOrder.getApprovedAmount().compareTo(BigDecimal.ZERO) > 0
                ? asOrder.getApprovedAmount() : asOrder.getApplyAmount()) > 0) {
            throw new BusinessException(ErrorCode.REFUND_EXCEED_AMOUNT);
        }

        // 校验状态
        stateMachine.checkTransition(AfterSalesStatus.fromCode(asOrder.getStatus()), AfterSalesStatus.REFUND_PROCESSING);

        // 创建或更新退款记录
        String refundNo = IdGenerator.genRefundNo();
        if (existing == null) {
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
        }

        // 模拟外部退款成功
        String externalRefundNo = "EXT_REF_" + System.currentTimeMillis();
        if (existing != null) {
            refundRecordMapper.updateStatus(existing.getId(), RefundStatus.SUCCESS.getCode(), externalRefundNo, null);
        }

        // 状态流转：-> COMPLETED
        stateMachine.checkTransition(AfterSalesStatus.REFUND_PROCESSING, AfterSalesStatus.COMPLETED);
        afterSalesOrderMapper.updateComplete(asOrder.getId(),
                AfterSalesStatus.COMPLETED.getCode(), AfterSalesStatus.REFUND_PROCESSING.getCode(), version);

        // 记录操作日志
        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "EXECUTE_REFUND", AfterSalesStatus.REFUND_PROCESSING.getCode(),
                AfterSalesStatus.COMPLETED.getCode(),
                "退款金额：" + refundAmount + "，外部流水：" + externalRefundNo);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("afterSalesNo", asOrder.getAfterSalesNo());
        result.put("refundNo", refundNo);
        result.put("refundAmount", refundAmount);
        result.put("status", AfterSalesStatus.COMPLETED.getCode());

        String respJson = JsonUtils.toJson(result);
        idempotencyService.markSuccess(idempotencyKey, respJson, afterSalesNo);

        return result;
    }

    /** 查询退款记录 */
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
