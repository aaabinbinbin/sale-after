package com.aftersales.domain.eligibility;

import com.aftersales.common.exception.ErrorCode;
import com.aftersales.infra.entity.TradeOrderItem;

import java.util.List;

/**
 * 校验订单项是否已有售后处理中。
 *
 * 注意：本 Checker 只检查订单项维度的 NONE->PROCESSING 状态，
 * 实际的重复售后检测需要查询 after_sales_item 表，由业务服务完成。
 */
public class DuplicateAfterSalesChecker implements EligibilityChecker {

    @Override
    public EligibilityCheckResult check(EligibilityContext context) {
        List<TradeOrderItem> orderItems = context.getOrderItems();
        if (orderItems == null || context.getApplyItems() == null) {
            return EligibilityCheckResult.pass();
        }

        // 检查申请的订单项是否有售后在处理中
        for (TradeOrderItem orderItem : orderItems) {
            boolean isTarget = context.getApplyItems().stream()
                    .anyMatch(a -> a.getOrderItemId().equals(orderItem.getId()));

            if (isTarget && "PROCESSING".equals(orderItem.getAfterSalesStatus())) {
                return EligibilityCheckResult.fail(ErrorCode.AFTER_SALES_DUPLICATE.getCode(),
                        "订单项 " + orderItem.getSkuName() + " 已有售后处理中");
            }
        }
        return EligibilityCheckResult.pass();
    }
}
