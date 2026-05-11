package com.aftersales.biz.rule;

/**
 * 售后规则接口。
 *
 * 所有业务规则（时效、金额、类目、风控、VIP、物流等）实现此接口。
 * Agent 不直接写业务逻辑，而是调用 RuleEngine 获取决策结果。
 */
public interface AfterSaleRule {

    /** 规则名称 */
    String getName();

    /** 优先级（越小越先执行） */
    int getPriority();

    /** 执行规则评估 */
    RuleResult evaluate(AfterSaleRuleContext context);
}
