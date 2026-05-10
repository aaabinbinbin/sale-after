package com.aftersales.domain.eligibility;

import com.aftersales.common.exception.ErrorCode;
import com.aftersales.infra.entity.TradeOrderItem;
import com.aftersales.domain.eligibility.EligibilityContext.ApplyItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 校验订单项是否属于该订单。
 */
public class OrderItemChecker implements EligibilityChecker {

    @Override
    public EligibilityCheckResult check(EligibilityContext context) {
        List<TradeOrderItem> orderItems = context.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            return EligibilityCheckResult.pass(); // 前续校验已处理
        }

        // 用 Map 加速查找
        Map<Long, TradeOrderItem> itemMap = orderItems.stream()
                .collect(Collectors.toMap(TradeOrderItem::getId, i -> i));

        List<ApplyItem> applyItems = context.getApplyItems();
        if (applyItems == null || applyItems.isEmpty()) {
            return EligibilityCheckResult.fail(ErrorCode.PARAM_INVALID.getCode(), "售后明细项不能为空");
        }

        for (ApplyItem applyItem : applyItems) {
            TradeOrderItem orderItem = itemMap.get(applyItem.getOrderItemId());
            if (orderItem == null) {
                return EligibilityCheckResult.fail(ErrorCode.AFTER_SALES_ITEM_NOT_IN_ORDER.getCode(),
                        "订单项 " + applyItem.getOrderItemId() + " 不属于该订单");
            }
        }
        return EligibilityCheckResult.pass();
    }
}
