package com.aftersales.agent.policy.impl;

import com.aftersales.agent.policy.Policy;
import com.aftersales.agent.policy.PolicyContext;
import com.aftersales.agent.policy.PolicyResult;
import org.springframework.stereotype.Component;

/**
 * 风险阈值策略：根据风控等级决定操作权限。
 *
 * - LOW 风险：全部放行
 * - MEDIUM 风险：写操作需确认
 * - HIGH 风险：全部写操作拒绝，需转人工
 */
@Component
public class RiskThresholdPolicy implements Policy {

    @Override
    public String getName() { return "RiskThresholdPolicy"; }

    @Override
    public int getPriority() { return 20; }

    @Override
    public PolicyResult check(PolicyContext context) {
        String riskLevel = context.getRiskLevel();
        if (riskLevel == null) return PolicyResult.allow(getName(), "无风险等级信息");

        return switch (riskLevel.toUpperCase()) {
            case "HIGH" -> PolicyResult.deny(getName(),
                    "风险等级 HIGH，Agent 不可自动执行，转人工处理");
            case "MEDIUM" -> PolicyResult.confirmRequired(getName(),
                    "风险等级 MEDIUM，需人工确认后执行");
            default -> PolicyResult.allow(getName(), "风险等级 LOW，允许执行");
        };
    }
}
