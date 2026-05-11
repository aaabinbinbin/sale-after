package com.aftersales.biz.listener;

import com.aftersales.biz.service.OperationLogService;
import com.aftersales.domain.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 领域事件统一监听器。
 *
 * 订阅所有领域事件，驱动：
 * - 审计日志
 * - 风控记录
 * - MQ 异步任务
 * - 后续扩展（通知、埋点等）
 */
@Component
public class DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventListener.class);

    private final OperationLogService operationLogService;

    public DomainEventListener(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @EventListener
    public void onAfterSalesCreated(AfterSalesCreatedEvent event) {
        log.info("售后创建事件: afterSalesNo={} type={} userId={}",
                event.getAfterSalesNo(), event.getAfterSalesType(), event.getUserId());
        // 后续可扩展：发送 MQ 通知、预热风控数据
    }

    @EventListener
    public void onAfterSalesApproved(AfterSalesApprovedEvent event) {
        log.info("售后审核通过事件: afterSalesNo={} reviewer={} amount={}",
                event.getAfterSalesNo(), event.getReviewerId(), event.getApprovedAmount());
        // 后续可扩展：自动触发后续流程、发送通知
    }

    @EventListener
    public void onAfterSalesRejected(AfterSalesRejectedEvent event) {
        log.info("售后审核拒绝事件: afterSalesNo={} reason={}",
                event.getAfterSalesNo(), event.getRejectReason());
        // 后续可扩展：发送拒绝通知
    }

    @EventListener
    public void onRefundCreated(RefundCreatedEvent event) {
        log.info("退款创建事件: refundNo={} amount={}",
                event.getRefundNo(), event.getRefundAmount());
        // 后续可扩展：风控监控、财务记录
    }

    @EventListener
    public void onRefundCompleted(RefundCompletedEvent event) {
        log.info("退款完成事件: refundNo={} externalRefundNo={} amount={}",
                event.getRefundNo(), event.getExternalRefundNo(), event.getRefundAmount());
        // 后续可扩展：通知用户、知识构建
    }

    @EventListener
    public void onInventoryLocked(InventoryLockedEvent event) {
        log.info("库存锁定事件: afterSalesNo={} skuId={} quantity={} available={}",
                event.getAfterSalesNo(), event.getSkuId(),
                event.getLockQuantity(), event.getAvailableAfterLock());
    }

    @EventListener
    public void onInventoryReleased(InventoryReleasedEvent event) {
        log.info("库存释放事件: afterSalesNo={} skuId={} quantity={} reason={}",
                event.getAfterSalesNo(), event.getSkuId(),
                event.getReleaseQuantity(), event.getReleaseReason());
    }

    @EventListener
    public void onRiskDetected(RiskDetectedEvent event) {
        log.warn("风控检测事件: afterSalesNo={} userId={} score={} level={} reasons={}",
                event.getAfterSalesNo(), event.getUserId(),
                event.getRiskScore(), event.getRiskLevel(), event.getRiskReasons());
        // 记录风控审计日志
        operationLogService.recordRiskIntercept(
                event.getUserId(), event.getAfterSalesNo(),
                event.getRiskLevel(), event.getRiskScore(), event.getRiskReasons());
    }

    @EventListener
    public void onAgentSuggestionGenerated(AgentSuggestionGeneratedEvent event) {
        log.info("Agent 建议生成事件: traceId={} type={} confidence={}",
                event.getTraceId(), event.getSuggestionType(), event.getConfidence());
        // 后续可扩展：Agent 准确率统计、模型评估数据收集
    }
}
