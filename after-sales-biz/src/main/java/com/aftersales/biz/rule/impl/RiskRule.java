package com.aftersales.biz.rule.impl;

import com.aftersales.biz.rule.AfterSaleRule;
import com.aftersales.biz.rule.AfterSaleRuleContext;
import com.aftersales.biz.rule.RuleResult;
import org.springframework.stereotype.Component;

/**
 * 风控规则：基于用户历史行为判断售后风险。
 *
 * - 短时间内高频售后 → 警告
 * - 退款成功率异常高 → 阻断
 */
@Component
public class RiskRule implements AfterSaleRule {

    /** 30 天内售后次数上限 */
    private static final int MAX_AFTER_SALES_IN_30_DAYS = 5;
    /** 总退款成功率超过此阈值则可疑 */
    private static final double MAX_REFUND_SUCCESS_RATE = 0.8;

    @Override
    public String getName() { return "RiskRule"; }

    @Override
    public int getPriority() { return 30; }

    @Override
    public RuleResult evaluate(AfterSaleRuleContext context) {
        int totalCount = context.getHistoricalAfterSalesCount();
        int refundCount = context.getHistoricalRefundCount();

        // 高频售后检查
        if (totalCount > MAX_AFTER_SALES_IN_30_DAYS) {
            return RuleResult.warn(getName(),
                    String.format("30天内售后 %d 次，超过上限 %d 次，建议人工审核", totalCount, MAX_AFTER_SALES_IN_30_DAYS));
        }

        // 退款成功率异常检查
        if (totalCount > 3 && refundCount > 0) {
            double rate = (double) refundCount / totalCount;
            if (rate > MAX_REFUND_SUCCESS_RATE) {
                return RuleResult.warn(getName(),
                        String.format("退款成功率 %.0f%% 异常偏高，需关注", rate * 100));
            }
        }

        return RuleResult.pass(getName(),
                String.format("风控检查通过（30天售后%d次，退款成功%d次）", totalCount, refundCount));
    }
}
