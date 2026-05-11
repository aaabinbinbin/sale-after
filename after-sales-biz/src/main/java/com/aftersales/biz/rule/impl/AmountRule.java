package com.aftersales.biz.rule.impl;

import com.aftersales.biz.rule.AfterSaleRule;
import com.aftersales.biz.rule.AfterSaleRuleContext;
import com.aftersales.biz.rule.RuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 金额规则：检查退款/补偿金额是否在允许范围内。
 *
 * - 退款金额不得超过订单实付金额
 * - 自动审批上限为 500 元，超过则需人工审核（WARN，不阻断）
 */
@Component
public class AmountRule implements AfterSaleRule {

    private static final BigDecimal AUTO_APPROVE_LIMIT = new BigDecimal("500");

    @Override
    public String getName() { return "AmountRule"; }

    @Override
    public int getPriority() { return 20; }

    @Override
    public RuleResult evaluate(AfterSaleRuleContext context) {
        BigDecimal applyAmount = context.getApplyAmount();
        if (applyAmount == null || applyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleResult.block(getName(), "申请金额无效: " + applyAmount);
        }

        // 金额上限校验
        if (context.getOrder() != null && context.getOrder().getPaidAmount() != null) {
            BigDecimal paid = context.getOrder().getPaidAmount();
            if (applyAmount.compareTo(paid) > 0) {
                return RuleResult.block(getName(),
                        String.format("申请金额 %s 超过订单实付金额 %s", applyAmount, paid));
            }
        }

        // 自动审批阈值
        if (applyAmount.compareTo(AUTO_APPROVE_LIMIT) > 0) {
            return RuleResult.warn(getName(),
                    String.format("金额 %s 超过自动审批上限 %s，需人工审核", applyAmount, AUTO_APPROVE_LIMIT));
        }

        return RuleResult.pass(getName(),
                "金额 " + applyAmount + " 在自动审批范围内");
    }
}
