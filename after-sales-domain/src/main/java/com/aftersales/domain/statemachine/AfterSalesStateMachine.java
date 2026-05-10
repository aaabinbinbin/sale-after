package com.aftersales.domain.statemachine;

import com.aftersales.common.enums.AfterSalesStatus;
import com.aftersales.common.enums.AfterSalesType;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 售后状态机。
 *
 * 所有售后状态流转必须经过此状态机校验。
 * 不允许直接 setStatus 绕过状态机。
 * 不同售后类型对应不同的合法流转路径。
 */
@Component
public class AfterSalesStateMachine {

    // 合法状态流转映射：当前状态 -> 允许的下一个状态集合
    private static final Map<AfterSalesStatus, Set<AfterSalesStatus>> TRANSITIONS = new EnumMap<>(AfterSalesStatus.class);

    static {
        // 创建 -> 待审核 / 已取消
        TRANSITIONS.put(AfterSalesStatus.CREATED,
                Set.of(AfterSalesStatus.PENDING_REVIEW, AfterSalesStatus.CANCELLED));

        // 待审核 -> 审核通过 / 审核拒绝 / 需补充信息
        TRANSITIONS.put(AfterSalesStatus.PENDING_REVIEW,
                Set.of(AfterSalesStatus.APPROVED, AfterSalesStatus.REJECTED, AfterSalesStatus.NEED_MORE_INFO));

        // 需补充信息 -> 待审核（补充后重新审核）
        TRANSITIONS.put(AfterSalesStatus.NEED_MORE_INFO,
                Set.of(AfterSalesStatus.PENDING_REVIEW));

        // 审核通过 -> 等待退货 / 退款处理 / 换货处理 / 补偿处理
        TRANSITIONS.put(AfterSalesStatus.APPROVED,
                Set.of(AfterSalesStatus.WAIT_RETURN_SHIPMENT, AfterSalesStatus.REFUND_PROCESSING,
                       AfterSalesStatus.EXCHANGE_PROCESSING, AfterSalesStatus.COMPENSATION_PROCESSING,
                       AfterSalesStatus.CANCELLED));

        // 等待退货 -> 等待收货
        TRANSITIONS.put(AfterSalesStatus.WAIT_RETURN_SHIPMENT,
                Set.of(AfterSalesStatus.WAIT_RETURN_RECEIVE, AfterSalesStatus.CANCELLED));

        // 等待收货 -> 退款处理 / 换货处理
        TRANSITIONS.put(AfterSalesStatus.WAIT_RETURN_RECEIVE,
                Set.of(AfterSalesStatus.REFUND_PROCESSING, AfterSalesStatus.EXCHANGE_PROCESSING));

        // 退款处理 -> 已完成 / 失败
        TRANSITIONS.put(AfterSalesStatus.REFUND_PROCESSING,
                Set.of(AfterSalesStatus.COMPLETED, AfterSalesStatus.FAILED));

        // 换货处理 -> 已完成 / 失败
        TRANSITIONS.put(AfterSalesStatus.EXCHANGE_PROCESSING,
                Set.of(AfterSalesStatus.COMPLETED, AfterSalesStatus.FAILED));

        // 补偿处理 -> 已完成 / 失败
        TRANSITIONS.put(AfterSalesStatus.COMPENSATION_PROCESSING,
                Set.of(AfterSalesStatus.COMPLETED, AfterSalesStatus.FAILED));
    }

    /**
     * 校验状态流转是否合法。
     *
     * @param currentStatus 当前状态
     * @param targetStatus  目标状态
     * @throws BusinessException 如果流转不合法
     */
    public void checkTransition(AfterSalesStatus currentStatus, AfterSalesStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new BusinessException(ErrorCode.AFTER_SALES_STATUS_INVALID, "状态不能为空");
        }

        // 相同状态不算流转
        if (currentStatus == targetStatus) {
            return;
        }

        // 终态不允许变更
        if (currentStatus.isFinal()) {
            throw new BusinessException(ErrorCode.AFTER_SALES_STATUS_INVALID,
                    "售后单已是终态(" + currentStatus.getDescription() + ")，不允许变更");
        }

        Set<AfterSalesStatus> allowed = TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new BusinessException(ErrorCode.AFTER_SALES_STATUS_INVALID,
                    "非法状态流转: " + currentStatus.getDescription() + " -> " + targetStatus.getDescription());
        }
    }

    /**
     * 获取审核通过后的下一状态。
     *
     * 根据售后类型确定审核通过后进入哪个状态。
     */
    public AfterSalesStatus getPostApproveStatus(AfterSalesType afterSalesType) {
        return switch (afterSalesType) {
            case REFUND_ONLY -> AfterSalesStatus.REFUND_PROCESSING;
            case RETURN_REFUND -> AfterSalesStatus.WAIT_RETURN_SHIPMENT;
            case EXCHANGE -> AfterSalesStatus.EXCHANGE_PROCESSING;
            case COMPENSATION -> AfterSalesStatus.COMPENSATION_PROCESSING;
        };
    }

    /**
     * 校验审核通过流转是否合法。
     */
    public void checkApproveTransition(String currentStatus, String afterSalesType) {
        AfterSalesStatus cur = AfterSalesStatus.fromCode(currentStatus);
        AfterSalesType type = AfterSalesType.fromCode(afterSalesType);
        if (cur == null || type == null) {
            throw new BusinessException(ErrorCode.AFTER_SALES_STATUS_INVALID, "状态或售后类型无效");
        }
        AfterSalesStatus next = getPostApproveStatus(type);
        checkTransition(cur, next);
    }
}
