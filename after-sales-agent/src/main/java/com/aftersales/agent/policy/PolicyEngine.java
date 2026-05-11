package com.aftersales.agent.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 策略引擎。
 *
 * 所有 Agent 的 Tool 调用、自动退款、自动审核必须经过此引擎校验。
 * 防止 LLM 越权、危险操作、Prompt 注入、误退款。
 */
@Component
public class PolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(PolicyEngine.class);

    private final List<Policy> policies;

    public PolicyEngine(List<Policy> policies) {
        this.policies = policies.stream()
                .sorted(Comparator.comparingInt(Policy::getPriority))
                .toList();
        log.info("PolicyEngine 加载 {} 条策略: {}", this.policies.size(),
                this.policies.stream().map(Policy::getName).toList());
    }

    /**
     * 检查是否允许执行。
     *
     * @return DENY 优先于 CONFIRM_REQUIRED 优先于 ALLOW
     */
    public PolicyResult check(PolicyContext context) {
        PolicyResult worstResult = PolicyResult.allow("DEFAULT");

        for (Policy policy : policies) {
            PolicyResult result = policy.check(context);

            if (result.isDenied()) {
                log.warn("Policy [{}] 拒绝: {}", policy.getName(), result.getReason());
                return result; // DENY 直接返回，不可继续
            }

            if (result.isConfirmRequired()) {
                log.info("Policy [{}] 要求确认: {}", policy.getName(), result.getReason());
                worstResult = result; // 记录但继续执行后续策略（可能有更强的拒绝）
            }
        }

        return worstResult;
    }

    /** 获取所有已注册策略 */
    public List<Policy> getPolicies() { return policies; }
}
