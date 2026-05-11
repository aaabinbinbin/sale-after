package com.aftersales.biz.rule.impl;

import com.aftersales.biz.rule.AfterSaleRule;
import com.aftersales.biz.rule.AfterSaleRuleContext;
import com.aftersales.biz.rule.RuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * VIP 规则：VIP 用户享有更宽松的售后政策。
 *
 * - VIP 用户自动审批金额上限提升到 1000 元
 * - SVIP 用户无金额限制，自动通过
 * - VIP 用户售后期限适当放宽
 */
@Component
public class VIPRule implements AfterSaleRule {

    private static final BigDecimal VIP_AUTO_LIMIT = new BigDecimal("1000");
    private static final Set<String> VIP_LEVELS = Set.of("VIP", "SVIP", "GOLD", "PLATINUM");

    @Override
    public String getName() { return "VIPRule"; }

    @Override
    public int getPriority() { return 5; } // 最先执行，为后续规则设定 VIP 状态

    @Override
    public RuleResult evaluate(AfterSaleRuleContext context) {
        String vipLevel = context.getVipLevel();
        if (vipLevel == null || !VIP_LEVELS.contains(vipLevel.toUpperCase())) {
            return RuleResult.pass(getName(), "普通用户");
        }

        // VIP 标记注入上下文，供后续规则使用
        context.setAttribute("isVIP", true);
        context.setAttribute("vipLevel", vipLevel.toUpperCase());

        // SVIP 无金额限制
        if ("SVIP".equalsIgnoreCase(vipLevel) || "PLATINUM".equalsIgnoreCase(vipLevel)) {
            context.setAttribute("autoApproveUnlimited", true);
            return RuleResult.pass(getName(), "SVIP用户，无限额限制");
        }

        // VIP 提高自动审批上限
        context.setAttribute("autoApproveLimit", VIP_AUTO_LIMIT);
        return RuleResult.pass(getName(),
                "VIP用户，自动审批上限提升至 " + VIP_AUTO_LIMIT + " 元");
    }
}
