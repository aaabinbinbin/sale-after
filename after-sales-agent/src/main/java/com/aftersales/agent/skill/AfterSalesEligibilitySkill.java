package com.aftersales.agent.skill;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.domain.eligibility.*;
import com.aftersales.infra.entity.TradeOrder;
import com.aftersales.infra.entity.TradeOrderItem;
import com.aftersales.infra.mapper.TradeOrderItemMapper;
import com.aftersales.infra.mapper.TradeOrderMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 售后资格判断技能。
 *
 * 复用业务 EligibilityService，Agent 不做重复校验。
 */
@Component
public class AfterSalesEligibilitySkill implements AgentSkill {

    private final AfterSalesEligibilityService eligibilityService;
    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;

    public AfterSalesEligibilitySkill(AfterSalesEligibilityService eligibilityService,
                                       TradeOrderMapper tradeOrderMapper,
                                       TradeOrderItemMapper tradeOrderItemMapper) {
        this.eligibilityService = eligibilityService;
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderItemMapper = tradeOrderItemMapper;
    }

    @Override
    public String name() { return "after_sales.eligibility.check"; }

    @Override
    public boolean supports(AgentContext context, AgentPlanStep step) {
        return context.getOrderNo() != null;
    }

    @Override
    public AgentSkillResult execute(AgentContext context, AgentPlanStep step) {
        TradeOrder order = tradeOrderMapper.selectByOrderNo(context.getOrderNo());
        List<TradeOrderItem> items = order != null ? tradeOrderItemMapper.selectByOrderId(order.getId()) : List.of();

        EligibilityContext ctx = new EligibilityContext();
        ctx.setUserId(context.getUserId());
        ctx.setOrderNo(context.getOrderNo());
        ctx.setOrder(order);
        ctx.setOrderItems(items);

        EligibilityCheckResult result = eligibilityService.check(ctx);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("eligible", result.isPassed());
        data.put("message", result.isPassed() ? "允许售后" : result.getErrorMessage());
        data.put("orderNo", context.getOrderNo());
        return AgentSkillResult.ok(data);
    }
}
