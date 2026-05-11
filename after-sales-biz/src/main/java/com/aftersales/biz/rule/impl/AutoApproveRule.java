package com.aftersales.biz.rule.impl;

import com.aftersales.biz.rule.AfterSaleRule;
import com.aftersales.biz.rule.AfterSaleRuleContext;
import com.aftersales.biz.rule.RuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 自动审批规则：判断售后申请是否符合自动审批条件。
 *
 * 条件：
 * 1. 金额 ≤ 自动审批上限（默认500，VIP → 1000，SVIP → 无限制）
 * 2. 无风控警告
 * 3. 时效内
 *
 * 此规则汇总前序规则结果，输出最终 AUTO/MANUAL 判定。
 */
@Component
public class AutoApproveRule implements AfterSaleRule {

    private static final BigDecimal DEFAULT_AUTO_LIMIT = new BigDecimal("500");

    @Override
    public String getName() { return "AutoApproveRule"; }

    @Override
    public int getPriority() { return 100; } // 最后执行，汇总前序规则

    @Override
    public RuleResult evaluate(AfterSaleRuleContext context) {
        BigDecimal applyAmount = context.getApplyAmount();

        // 检查是否有前置阻断
        Boolean hasBlock = context.getAttribute("hasBlock");
        if (Boolean.TRUE.equals(hasBlock)) {
            return RuleResult.warn(getName(), "存在阻断规则，需人工审核");
        }

        // 检查用户风险评分
        int riskScore = context.getUserRiskScore();
        if (riskScore >= 80) {
            return RuleResult.warn(getName(), "用户风险评分过高(" + riskScore + ")，需人工审核");
        }

        // 金额判断
        if (applyAmount == null) {
            return RuleResult.warn(getName(), "无申请金额，需人工审核");
        }

        Boolean unlimited = context.getAttribute("autoApproveUnlimited");
        if (Boolean.TRUE.equals(unlimited)) {
            return RuleResult.pass(getName(), "SVIP用户自动审批（无限额）");
        }

        BigDecimal limit = context.getAttribute("autoApproveLimit");
        if (limit == null) limit = DEFAULT_AUTO_LIMIT;

        if (applyAmount.compareTo(limit) <= 0) {
            return RuleResult.pass(getName(),
                    String.format("自动审批通过（金额 %s ≤ 上限 %s）", applyAmount, limit));
        }

        return RuleResult.warn(getName(),
                String.format("金额 %s 超过自动审批上限 %s，需人工审核", applyAmount, limit));
    }
}
