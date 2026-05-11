package com.aftersales.agent.policy.impl;

import com.aftersales.agent.policy.Policy;
import com.aftersales.agent.policy.PolicyContext;
import com.aftersales.agent.policy.PolicyResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Tool 权限策略：校验用户角色是否有权调用指定 Tool。
 *
 * 不同角色可调用的 Tool 集合不同：
 * - USER：只能查询（query/read）
 * - CUSTOMER_SERVICE：查询 + 审核 + 退款 + 换货
 * - ADMIN：全部权限
 */
@Component
public class ToolPermissionPolicy implements Policy {

    /** 高风险 Tool：只有 ADMIN 可以调用 */
    private static final Set<String> ADMIN_ONLY_TOOLS = Set.of(
            "compensation.grant",
            "refund.execute",
            "refund.forceExecute"
    );

    /** 客服及以上可调用的 Tool */
    private static final Set<String> CS_TOOLS = Set.of(
            "after.sales.approve",
            "after.sales.reject",
            "refund.execute",
            "exchange.lockStock",
            "exchange.ship",
            "return.confirmReceive"
    );

    @Override
    public String getName() { return "ToolPermissionPolicy"; }

    @Override
    public int getPriority() { return 1; } // 最先执行权限检查

    @Override
    public PolicyResult check(PolicyContext context) {
        String role = context.getUserRole();
        String toolName = context.getToolName();

        if (role == null) {
            return PolicyResult.deny(getName(), "未认证用户");
        }

        // ADMIN 全部通过
        if ("ADMIN".equals(role)) {
            return PolicyResult.allow(getName());
        }

        // ADMIN_ONLY 工具检查
        if (ADMIN_ONLY_TOOLS.contains(toolName)) {
            return PolicyResult.deny(getName(),
                    "工具 [" + toolName + "] 仅限 ADMIN 调用，当前角色: " + role);
        }

        // 客服及以上可调用的工具
        if (CS_TOOLS.contains(toolName)) {
            if ("CUSTOMER_SERVICE".equals(role)) {
                return PolicyResult.allow(getName());
            }
            return PolicyResult.deny(getName(),
                    "工具 [" + toolName + "] 需要客服及以上权限，当前角色: " + role);
        }

        // 普通用户可查询
        return PolicyResult.allow(getName());
    }
}
