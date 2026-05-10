package com.aftersales.domain.eligibility;

import com.aftersales.common.exception.ErrorCode;
import com.aftersales.infra.entity.TradeOrder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 校验是否在售后窗口期内。
 *
 * 默认签收后 7 天内可申请售后（质量问题 15 天）。
 */
public class AfterSalesWindowChecker implements EligibilityChecker {

    // 默认售后窗口期（天）
    private static final int DEFAULT_WINDOW_DAYS = 7;
    // 质量问题窗口期（天）
    private static final int QUALITY_WINDOW_DAYS = 15;

    @Override
    public EligibilityCheckResult check(EligibilityContext context) {
        TradeOrder order = context.getOrder();
        if (order == null || order.getFinishedAt() == null) {
            return EligibilityCheckResult.pass(); // 没有完成时间的订单，由订单状态控制
        }

        int windowDays = "QUALITY_PROBLEM".equals(context.getReasonCode())
                ? QUALITY_WINDOW_DAYS : DEFAULT_WINDOW_DAYS;

        LocalDateTime deadline = order.getFinishedAt().plus(windowDays, ChronoUnit.DAYS);
        if (LocalDateTime.now().isAfter(deadline)) {
            return EligibilityCheckResult.fail(ErrorCode.AFTER_SALES_OUT_OF_WINDOW.getCode(),
                    "已超出" + windowDays + "天售后申请窗口期");
        }
        return EligibilityCheckResult.pass();
    }
}
