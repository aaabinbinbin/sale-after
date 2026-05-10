package com.aftersales.domain.eligibility;

import com.aftersales.common.enums.OrderStatus;
import com.aftersales.common.exception.ErrorCode;

/**
 * 校验订单状态是否允许售后。
 */
public class OrderStatusChecker implements EligibilityChecker {

    @Override
    public EligibilityCheckResult check(EligibilityContext context) {
        if (context.getOrder() == null) {
            return EligibilityCheckResult.pass();
        }
        OrderStatus status = OrderStatus.fromCode(context.getOrder().getOrderStatus());
        if (status == null || !status.allowsAfterSales()) {
            return EligibilityCheckResult.fail(ErrorCode.ORDER_STATUS_NOT_ALLOW_AFTER_SALES.getCode(),
                    "订单状态(" + context.getOrder().getOrderStatus() + ")不允许售后");
        }
        return EligibilityCheckResult.pass();
    }
}
