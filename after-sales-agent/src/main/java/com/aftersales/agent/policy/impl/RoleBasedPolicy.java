package com.aftersales.agent.policy.impl;

import com.aftersales.agent.policy.Policy;
import com.aftersales.agent.policy.PolicyContext;
import com.aftersales.agent.policy.PolicyResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 角色策略：根据角色限制操作范围。
 *
 * - USER 角色不允许执行写操作
 * - CUSTOMER_SERVICE 有金额限制
 */
@Component
public class RoleBasedPolicy implements Policy {

    /** USER 只读操作白名单 */
    private static final Set<String> READ_ONLY_TOOLS = Set.of(
            "order.query", "orderQueryTool",
            "after.sales.progress", "afterSalesProgressTool",
            "rag.retrieve", "ragRetrieveTool",
            "stock.check", "stockCheckTool",
            "policy.qa", "policyQaTool"
    );

    @Override
    public String getName() { return "RoleBasedPolicy"; }

    @Override
    public int getPriority() { return 5; }

    @Override
    public PolicyResult check(PolicyContext context) {
        String role = context.getUserRole();
        String toolName = context.getToolName();

        if (role == null) {
            return PolicyResult.deny(getName(), "未认证用户");
        }

        if ("ADMIN".equals(role)) {
            return PolicyResult.allow(getName(), "管理员全权限");
        }

        if ("USER".equals(role)) {
            if (!READ_ONLY_TOOLS.contains(toolName)) {
                return PolicyResult.deny(getName(),
                        "USER 角色不允许执行写操作: " + toolName);
            }
            return PolicyResult.allow(getName(), "USER 只读操作");
        }

        // CUSTOMER_SERVICE: 允许写操作但有金额限制
        if ("CUSTOMER_SERVICE".equals(role)) {
            BigDecimal amount = context.getAmount();
            if (amount != null && amount.compareTo(new BigDecimal("2000")) > 0) {
                return PolicyResult.confirmRequired(getName(),
                        "客服处理金额 " + amount + " 超过 2000 元，需主管确认");
            }
            return PolicyResult.allow(getName());
        }

        return PolicyResult.deny(getName(), "未知角色: " + role);
    }
}
