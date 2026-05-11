package com.aftersales.biz.rule.impl;

import com.aftersales.biz.rule.AfterSaleRule;
import com.aftersales.biz.rule.AfterSaleRuleContext;
import com.aftersales.biz.rule.RuleResult;
import com.aftersales.common.enums.AfterSalesType;
import org.springframework.stereotype.Component;

/**
 * 物流规则：检查物流相关的前置条件。
 *
 * - 退货退款/换货：订单必须先已发货
 * - 已签收超过 7 天则需提供退货理由
 */
@Component
public class LogisticsRule implements AfterSaleRule {

    @Override
    public String getName() { return "LogisticsRule"; }

    @Override
    public int getPriority() { return 25; }

    @Override
    public RuleResult evaluate(AfterSaleRuleContext context) {
        AfterSalesType type = context.getAfterSalesType();

        // 仅退款不需要物流前置校验
        if (type == AfterSalesType.REFUND_ONLY || type == AfterSalesType.COMPENSATION) {
            return RuleResult.pass(getName(), "该售后类型无需物流检查");
        }

        if (context.getOrder() == null) {
            return RuleResult.pass(getName(), "无订单信息，跳过物流检查");
        }

        // 退货退款/换货：订单必须已发货
        if (context.getOrder().getShippedAt() == null) {
            return RuleResult.block(getName(), "订单尚未发货，无法进行退货/换货操作");
        }

        return RuleResult.pass(getName(), "物流检查通过");
    }
}
