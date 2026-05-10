package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.domain.statemachine.AfterSalesStateMachine;
import com.aftersales.infra.entity.AfterSalesItem;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.mapper.AfterSalesItemMapper;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 售后审核服务。
 *
 * 负责审核通过、审核拒绝、要求补充材料的用例编排。
 * 审核是高风险动作，需要客服/管理员权限，需要乐观锁和操作日志。
 */
@Service
public class AfterSalesReviewService {

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final AfterSalesItemMapper afterSalesItemMapper;
    private final AfterSalesStateMachine stateMachine;
    private final OperationLogService operationLogService;

    public AfterSalesReviewService(AfterSalesOrderMapper afterSalesOrderMapper,
                                    AfterSalesItemMapper afterSalesItemMapper,
                                    AfterSalesStateMachine stateMachine,
                                    OperationLogService operationLogService) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.afterSalesItemMapper = afterSalesItemMapper;
        this.stateMachine = stateMachine;
        this.operationLogService = operationLogService;
    }

    /**
     * 审核通过。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(String afterSalesNo, Map<String, Object> command) {
        // 权限检查：只有客服/管理员可以审核
        checkServiceRole();

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) {
            throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        }

        Long version = toLong(command.get("version"));
        BigDecimal approvedAmount = toBigDecimal(command.get("approvedAmount"));
        String reviewRemark = (String) command.get("reviewRemark");

        // 状态机校验审核通过流转
        stateMachine.checkApproveTransition(asOrder.getStatus(), asOrder.getAfterSalesType());
        AfterSalesStatus nextStatus = stateMachine.getPostApproveStatus(
                com.aftersales.common.enums.AfterSalesType.fromCode(asOrder.getAfterSalesType()));

        // 乐观锁更新审核信息
        int rows = afterSalesOrderMapper.updateReviewInfo(asOrder.getId(),
                UserContext.getUserId(), reviewRemark, approvedAmount,
                nextStatus.getCode(), asOrder.getStatus(), version);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.AFTER_SALES_VERSION_CONFLICT);
        }

        // 更新明细项审核信息
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> approvedItems = (List<Map<String, Object>>) command.get("approvedItems");
        if (approvedItems != null) {
            for (Map<String, Object> item : approvedItems) {
                Long orderItemId = toLong(item.get("orderItemId"));
                List<AfterSalesItem> items = afterSalesItemMapper.selectByAfterSalesId(asOrder.getId());
                for (AfterSalesItem asItem : items) {
                    if (asItem.getOrderItemId().equals(orderItemId)) {
                        afterSalesItemMapper.updateApprovedInfo(asItem.getId(),
                                toInt(item.get("approvedQuantity")),
                                toBigDecimal(item.get("approvedAmount")),
                                "APPROVED");
                    }
                }
            }
        }

        // 记录操作日志
        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "APPROVE", asOrder.getStatus(), nextStatus.getCode(),
                "审核通过，备注：" + reviewRemark);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("afterSalesNo", asOrder.getAfterSalesNo());
        result.put("status", nextStatus.getCode());
        return result;
    }

    /**
     * 审核拒绝。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(String afterSalesNo, Map<String, Object> command) {
        checkServiceRole();

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) {
            throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        }

        Long version = toLong(command.get("version"));
        String reviewRemark = (String) command.get("reviewRemark");

        stateMachine.checkTransition(AfterSalesStatus.fromCode(asOrder.getStatus()), AfterSalesStatus.REJECTED);

        int rows = afterSalesOrderMapper.updateStatusWithVersion(asOrder.getId(),
                AfterSalesStatus.REJECTED.getCode(), asOrder.getStatus(), version);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.AFTER_SALES_VERSION_CONFLICT);
        }

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "REJECT", asOrder.getStatus(), AfterSalesStatus.REJECTED.getCode(),
                "审核拒绝，备注：" + reviewRemark);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("afterSalesNo", asOrder.getAfterSalesNo());
        result.put("status", AfterSalesStatus.REJECTED.getCode());
        return result;
    }

    /**
     * 要求补充材料。
     */
    @Transactional(rollbackFor = Exception.class)
    public void needMoreInfo(String afterSalesNo, Map<String, Object> command) {
        checkServiceRole();

        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) {
            throw new BusinessException(ErrorCode.AFTER_SALES_NOT_FOUND);
        }

        stateMachine.checkTransition(AfterSalesStatus.fromCode(asOrder.getStatus()), AfterSalesStatus.NEED_MORE_INFO);

        afterSalesOrderMapper.updateStatusWithVersion(asOrder.getId(),
                AfterSalesStatus.NEED_MORE_INFO.getCode(), asOrder.getStatus(), asOrder.getVersion());

        operationLogService.record(asOrder.getId(), asOrder.getAfterSalesNo(),
                "NEED_MORE_INFO", asOrder.getStatus(), AfterSalesStatus.NEED_MORE_INFO.getCode(),
                "要求补充材料：" + command.get("reviewRemark"));
    }

    private void checkServiceRole() {
        String role = UserContext.getRole();
        if (!"CUSTOMER_SERVICE".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有客服或管理员可以审核售后");
        }
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        return obj instanceof Number n ? n.longValue() : Long.valueOf(obj.toString());
    }

    private Integer toInt(Object obj) {
        if (obj == null) return 0;
        return obj instanceof Number n ? n.intValue() : Integer.valueOf(obj.toString());
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        return obj instanceof BigDecimal bd ? bd : new BigDecimal(obj.toString());
    }
}
