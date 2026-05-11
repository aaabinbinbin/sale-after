package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.enums.AfterSalesType;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.domain.eligibility.*;
import com.aftersales.domain.event.AfterSalesCreatedEvent;
import com.aftersales.domain.statemachine.AfterSalesStateMachine;
import com.aftersales.infra.entity.*;
import com.aftersales.infra.mapper.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 售后申请应用服务。
 *
 * 负责售后申请创建的完整用例编排：幂等校验、订单归属校验、
 * 售后资格校验、主单与明细创建、凭证绑定、订单项状态更新、操作日志记录。
 */
@Service
public class AfterSalesApplicationService {

    private final IdempotencyService idempotencyService;
    private final AfterSalesEligibilityService eligibilityService;
    private final AfterSalesStateMachine stateMachine;
    private final OperationLogService operationLogService;

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final AfterSalesItemMapper afterSalesItemMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AfterSalesApplicationService(IdempotencyService idempotencyService,
                                         AfterSalesEligibilityService eligibilityService,
                                         AfterSalesStateMachine stateMachine,
                                         OperationLogService operationLogService,
                                         TradeOrderMapper tradeOrderMapper,
                                         TradeOrderItemMapper tradeOrderItemMapper,
                                         AfterSalesOrderMapper afterSalesOrderMapper,
                                         AfterSalesItemMapper afterSalesItemMapper,
                                         ApplicationEventPublisher eventPublisher) {
        this.idempotencyService = idempotencyService;
        this.eligibilityService = eligibilityService;
        this.stateMachine = stateMachine;
        this.operationLogService = operationLogService;
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderItemMapper = tradeOrderItemMapper;
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.afterSalesItemMapper = afterSalesItemMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建售后申请。
     *
     * @param idempotencyKey 幂等 Key（请求头）
     * @param command        申请命令
     * @return 售后创建结果（afterSalesNo, status）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createApplication(String idempotencyKey, Map<String, Object> command) {
        // 1. 幂等校验
        String historyResp = idempotencyService.checkOrRecord(idempotencyKey, command, "CREATE_AFTER_SALES");
        if (historyResp != null) {
            return JsonUtils.fromJson(historyResp, Map.class);
        }

        String orderNo = (String) command.get("orderNo");
        String afterSalesType = (String) command.get("afterSalesType");
        String reasonCode = (String) command.get("reasonCode");
        String reasonText = (String) command.get("reasonText");
        String remark = (String) command.get("remark");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) command.get("items");

        // 2. 查询订单
        TradeOrder order = tradeOrderMapper.selectByOrderNo(orderNo);
        List<TradeOrderItem> orderItems = tradeOrderItemMapper.selectByOrderId(order != null ? order.getId() : null);

        // 3. 构建校验上下文
        EligibilityContext ctx = new EligibilityContext();
        ctx.setUserId(UserContext.getUserId());
        ctx.setOrderNo(orderNo);
        ctx.setOrder(order);
        ctx.setOrderItems(orderItems);
        ctx.setAfterSalesType(afterSalesType);
        ctx.setReasonCode(reasonCode);

        List<EligibilityContext.ApplyItem> applyItems = new ArrayList<>();
        BigDecimal totalApplyAmount = BigDecimal.ZERO;
        if (items != null) {
            for (Map<String, Object> item : items) {
                EligibilityContext.ApplyItem ai = new EligibilityContext.ApplyItem();
                Long orderItemId = toLong(item.get("orderItemId"));
                ai.setOrderItemId(orderItemId);
                ai.setApplyQuantity(toInt(item.get("applyQuantity")));
                BigDecimal applyAmount = toBigDecimal(item.get("applyAmount"));
                ai.setApplyAmount(applyAmount);
                totalApplyAmount = totalApplyAmount.add(applyAmount != null ? applyAmount : BigDecimal.ZERO);
                applyItems.add(ai);
            }
        }
        ctx.setApplyItems(applyItems);

        // 4. 执行资格校验
        EligibilityCheckResult checkResult = eligibilityService.check(ctx);
        if (!checkResult.isPassed()) {
            throw new BusinessException(checkResult.getErrorCode(), checkResult.getErrorMessage());
        }

        // 5. 创建售后主单
        AfterSalesOrder asOrder = new AfterSalesOrder();
        asOrder.setAfterSalesNo(IdGenerator.genAfterSalesNo());
        asOrder.setOrderId(order.getId());
        asOrder.setOrderNo(order.getOrderNo());
        asOrder.setUserId(UserContext.getUserId());
        asOrder.setAfterSalesType(afterSalesType);
        asOrder.setStatus(AfterSalesStatus.CREATED.getCode());
        asOrder.setReasonCode(reasonCode);
        asOrder.setReasonText(reasonText);
        asOrder.setApplyAmount(totalApplyAmount);
        asOrder.setApprovedAmount(BigDecimal.ZERO);
        asOrder.setApplicantRemark(remark);
        afterSalesOrderMapper.insert(asOrder);

        // 6. 创建售后明细项
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                EligibilityContext.ApplyItem ai = applyItems.get(i);
                TradeOrderItem orderItem = orderItems.stream()
                        .filter(oi -> oi.getId().equals(ai.getOrderItemId()))
                        .findFirst().orElse(null);

                AfterSalesItem asItem = new AfterSalesItem();
                asItem.setAfterSalesId(asOrder.getId());
                asItem.setAfterSalesNo(asOrder.getAfterSalesNo());
                asItem.setOrderItemId(ai.getOrderItemId());
                asItem.setProductId(orderItem != null ? orderItem.getProductId() : null);
                asItem.setSkuId(orderItem != null ? orderItem.getSkuId() : null);
                asItem.setProductName(orderItem != null ? orderItem.getProductName() : null);
                asItem.setSkuName(orderItem != null ? orderItem.getSkuName() : null);
                asItem.setApplyQuantity(ai.getApplyQuantity());
                asItem.setApprovedQuantity(0);
                asItem.setRefundableAmount(orderItem != null ? orderItem.getRefundableAmount() : BigDecimal.ZERO);
                asItem.setApplyAmount(ai.getApplyAmount() != null ? ai.getApplyAmount() : BigDecimal.ZERO);
                asItem.setApprovedAmount(BigDecimal.ZERO);
                asItem.setItemStatus("PENDING");
                afterSalesItemMapper.insert(asItem);
            }
        }

        // 7. 状态机流转：CREATED -> PENDING_REVIEW
        stateMachine.checkTransition(AfterSalesStatus.CREATED, AfterSalesStatus.PENDING_REVIEW);
        afterSalesOrderMapper.updateStatusWithVersion(asOrder.getId(), AfterSalesStatus.PENDING_REVIEW.getCode(),
                AfterSalesStatus.CREATED.getCode(), 0L);

        // 8. 更新订单项售后状态为 PROCESSING
        if (items != null) {
            for (Map<String, Object> item : items) {
                Long orderItemId = toLong(item.get("orderItemId"));
                tradeOrderItemMapper.updateAfterSalesStatus(orderItemId, "PROCESSING", "NONE");
            }
        }

        // 9. 记录操作日志
        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "CREATE_APPLICATION",
                AfterSalesStatus.CREATED.getCode(),
                AfterSalesStatus.PENDING_REVIEW.getCode(),
                "创建售后申请，类型：" + afterSalesType);

        // 10. 保存幂等成功
        Map<String, Object> respData = new LinkedHashMap<>();
        respData.put("afterSalesNo", asOrder.getAfterSalesNo());
        respData.put("status", AfterSalesStatus.PENDING_REVIEW.getCode());
        String respJson = JsonUtils.toJson(respData);
        idempotencyService.markSuccess(idempotencyKey, respJson, asOrder.getAfterSalesNo());

        // 11. 发布售后创建事件
        eventPublisher.publishEvent(AfterSalesCreatedEvent.builder()
                .afterSalesNo(asOrder.getAfterSalesNo())
                .afterSalesId(asOrder.getId())
                .afterSalesType(afterSalesType)
                .userId(String.valueOf(UserContext.getUserId()))
                .orderNo(orderNo)
                .build());

        return respData;
    }

    /** 查询售后详情 */
    public Map<String, Object> getDetail(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) {
            throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        }
        // 权限：普通用户只能看自己的售后
        String role = UserContext.getRole();
        if ("USER".equals(role) && !asOrder.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<AfterSalesItem> items = afterSalesItemMapper.selectByAfterSalesId(asOrder.getId());
        List<AfterSalesOperationLog> logs = operationLogService.listByAfterSalesId(asOrder.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", asOrder);
        result.put("items", items);
        result.put("operationLogs", logs);
        return result;
    }

    /** 查询售后列表 */
    public Map<String, Object> listAfterSales(String status, String afterSalesType,
                                               String orderNo, int pageNum, int pageSize) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();
        int offset = (pageNum - 1) * pageSize;

        // 普通用户只能看自己的售后
        Long filterUserId = "USER".equals(role) ? userId : null;
        List<AfterSalesOrder> list = afterSalesOrderMapper.selectByCondition(
                status, afterSalesType, orderNo, filterUserId, offset, pageSize);
        long total = afterSalesOrderMapper.countByCondition(status, afterSalesType, orderNo, filterUserId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("pages", pageSize > 0 ? (total + pageSize - 1) / pageSize : 0);
        return result;
    }

    /** 取消售后申请 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelApplication(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) {
            throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        }
        if (!asOrder.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        stateMachine.checkTransition(AfterSalesStatus.fromCode(asOrder.getStatus()), AfterSalesStatus.CANCELLED);
        afterSalesOrderMapper.updateStatusWithVersion(asOrder.getId(), AfterSalesStatus.CANCELLED.getCode(),
                asOrder.getStatus(), asOrder.getVersion());

        // 恢复订单项售后状态
        List<AfterSalesItem> items = afterSalesItemMapper.selectByAfterSalesId(asOrder.getId());
        for (AfterSalesItem item : items) {
            tradeOrderItemMapper.updateAfterSalesStatus(item.getOrderItemId(), "NONE", "PROCESSING");
        }

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "CANCEL", asOrder.getStatus(), AfterSalesStatus.CANCELLED.getCode(), "用户取消售后申请");
    }

    // ========== 辅助方法 ==========

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        return Long.valueOf(obj.toString());
    }

    private Integer toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.intValue();
        return Integer.valueOf(obj.toString());
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        return new BigDecimal(obj.toString());
    }
}
