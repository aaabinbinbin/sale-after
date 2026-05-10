package com.aftersales.domain.eligibility;

import com.aftersales.common.exception.ErrorCode;
import com.aftersales.infra.entity.TradeOrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 校验申请金额不超过可退金额。
 */
public class AmountChecker implements EligibilityChecker {

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
            if (orderItem == null) continue;

            BigDecimal applyAmount = applyItem.getApplyAmount();
            if (applyAmount == null) continue;

            BigDecimal maxRefund = orderItem.getRefundableAmount();
            if (maxRefund != null && applyAmount.compareTo(maxRefund) > 0) {
                return EligibilityCheckResult.fail(ErrorCode.AFTER_SALES_EXCEED_AMOUNT.getCode(),
                        "订单项 " + orderItem.getSkuName() + " 申请金额(" + applyAmount
                        + ")超过可退金额(" + maxRefund + ")");
            }
        }
        return EligibilityCheckResult.pass();
    }
}
