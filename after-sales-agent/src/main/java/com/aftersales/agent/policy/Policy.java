package com.aftersales.agent.policy;

/**
 * 策略接口。
 *
 * 所有 Tool 调用、自动退款、自动审核必须经过 PolicyEngine 校验。
 */
public interface Policy {

    /** 策略名称 */
    String getName();

    /** 策略优先级（越小越先执行） */
    int getPriority();

    /** 执行策略检查 */
    PolicyResult check(PolicyContext context);
}
