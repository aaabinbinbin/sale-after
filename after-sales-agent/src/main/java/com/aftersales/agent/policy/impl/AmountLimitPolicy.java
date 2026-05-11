package com.aftersales.agent.policy.impl;

import com.aftersales.agent.policy.Policy;
import com.aftersales.agent.policy.PolicyContext;
import com.aftersales.agent.policy.PolicyResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 金额上限策略：防止 LLM 误操作大额退款。
 *
 * - 自动执行上限 500 元
 * - 超过 500 元必须人工确认
 * - 超过 5000 元直接拒绝（不可自动）
 */
@Component
public class AmountLimitPolicy implements Policy {

    private static final BigDecimal CONFIRM_THRESHOLD = new BigDecimal("500");
    private static final BigDecimal DENY_THRESHOLD = new BigDecimal("5000");

    @Override
    public String getName() { return "AmountLimitPolicy"; }

    @Override
    public int getPriority() { return 10; }

    @Override
    public PolicyResult check(PolicyContext context) {
        BigDecimal amount = context.getAmount();
        if (amount == null) {
            return PolicyResult.allow(getName(), "无金额信息");
        }

        if (amount.compareTo(DENY_THRESHOLD) > 0) {
            return PolicyResult.deny(getName(),
                    String.format("金额 %s 超过自动处理上限 %s，必须人工处理", amount, DENY_THRESHOLD));
        }

        if (amount.compareTo(CONFIRM_THRESHOLD) > 0) {
            return PolicyResult.confirmRequired(getName(),
                    String.format("金额 %s 超过自动审批上限 %s，需人工确认", amount, CONFIRM_THRESHOLD));
        }

        return PolicyResult.allow(getName(),
                "金额 " + amount + " 在安全范围内");
    }
}
