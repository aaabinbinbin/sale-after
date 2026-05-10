package com.aftersales.domain.eligibility;

import com.aftersales.common.exception.ErrorCode;

/**
 * 校验订单是否属于当前用户。
 */
public class OrderOwnerChecker implements EligibilityChecker {

    @Override
    public EligibilityCheckResult check(EligibilityContext context) {
        if (context.getOrder() == null) {
            return EligibilityCheckResult.pass(); // 订单存在性由前一个 Checker 负责
        }
        if (!context.getOrder().getUserId().equals(context.getUserId())) {
            return EligibilityCheckResult.fail(ErrorCode.ORDER_NOT_BELONG_TO_USER.getCode(), "订单不属于当前用户");
        }
        return EligibilityCheckResult.pass();
    }
}
