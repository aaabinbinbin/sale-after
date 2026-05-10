package com.aftersales.domain.eligibility;

import com.aftersales.common.exception.ErrorCode;

/**
 * 校验订单是否存在。
 */
public class OrderExistsChecker implements EligibilityChecker {

    @Override
    public EligibilityCheckResult check(EligibilityContext context) {
        if (context.getOrder() == null) {
            return EligibilityCheckResult.fail(ErrorCode.ORDER_NOT_FOUND.getCode(), "订单不存在");
        }
        return EligibilityCheckResult.pass();
    }
}
