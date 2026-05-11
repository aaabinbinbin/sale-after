package com.aftersales.biz.rule.impl;

import com.aftersales.biz.rule.AfterSaleRule;
import com.aftersales.biz.rule.AfterSaleRuleContext;
import com.aftersales.biz.rule.RuleResult;
import com.aftersales.common.enums.AfterSalesType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 时效规则：检查是否在允许的售后期限内。
 *
 * 默认规则：
 * - 退货退款/换货：订单完成后 7 天内
 * - 仅退款：订单完成后 15 天内（含质量问题场景）
 * - 补偿：订单完成后 30 天内
 */
@Component
public class TimeLimitRule implements AfterSaleRule {

    @Override
    public String getName() { return "TimeLimitRule"; }

    @Override
    public int getPriority() { return 10; }

    @Override
    public RuleResult evaluate(AfterSaleRuleContext context) {
        if (context.getOrder() == null || context.getOrder().getFinishedAt() == null) {
            return RuleResult.pass(getName(), "无订单完成时间，跳过时效检查");
        }

        LocalDateTime finishedAt = context.getOrder().getFinishedAt();
        long daysSince = ChronoUnit.DAYS.between(finishedAt, context.getNow());

        AfterSalesType type = context.getAfterSalesType();
        int maxDays = switch (type) {
            case RETURN_REFUND, EXCHANGE -> 7;
            case REFUND_ONLY -> 15;
            case COMPENSATION -> 30;
        };

        if (daysSince > maxDays) {
            return RuleResult.block(getName(),
                    String.format("已超过售后期限 %d 天（允许 %d 天）", daysSince, maxDays));
        }

        return RuleResult.pass(getName(),
                String.format("在售后期限内（%d/%d 天）", daysSince, maxDays));
    }
}
