package com.aftersales.domain.eligibility;

import com.aftersales.common.exception.ErrorCode;
import com.aftersales.infra.entity.TradeOrderItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 校验申请数量不超过购买数量。
 */
public class QuantityChecker implements EligibilityChecker {

    @Override
    public EligibilityCheckResult check(EligibilityContext context) {
        List<TradeOrderItem> orderItems = context.getOrderItems();
        if (orderItems == null || context.getApplyItems() == null) {
            return EligibilityCheckResult.pass();
        }

        Map<Long, TradeOrderItem> itemMap = orderItems.stream()
                .collect(Collectors.toMap(TradeOrderItem::getId, i -> i));

        for (EligibilityContext.ApplyItem applyItem : context.getApplyItems()) {
            TradeOrderItem orderItem = itemMap.get(applyItem.getOrderItemId());
            if (orderItem == null) continue; // 前续 Checker 已处理

            if (applyItem.getApplyQuantity() == null || applyItem.getApplyQuantity() <= 0) {
                return EligibilityCheckResult.fail(ErrorCode.PARAM_INVALID.getCode(), "申请数量必须大于0");
            }
            if (applyItem.getApplyQuantity() > orderItem.getQuantity()) {
                return EligibilityCheckResult.fail(ErrorCode.AFTER_SALES_EXCEED_QUANTITY.getCode(),
                        "订单项 " + orderItem.getSkuName() + " 申请数量(" + applyItem.getApplyQuantity()
                        + ")超过购买数量(" + orderItem.getQuantity() + ")");
            }
        }
        return EligibilityCheckResult.pass();
    }
}
