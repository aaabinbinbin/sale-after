package com.aftersales.agent.skill;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.biz.service.OrderBizService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单查询技能。
 *
 * 查询订单事实，不修改订单，不推断不存在的订单。
 */
@Component
public class OrderQuerySkill implements AgentSkill {

    private final OrderBizService orderBizService;

    public OrderQuerySkill(OrderBizService orderBizService) {
        this.orderBizService = orderBizService;
    }

    @Override
    public String name() { return "order.query"; }

    @Override
    public boolean supports(AgentContext context, AgentPlanStep step) {
        return context.getOrderNo() != null;
    }

    @Override
    public AgentSkillResult execute(AgentContext context, AgentPlanStep step) {
        try {
            Map<String, Object> detail = orderBizService.getOrderDetail(context.getOrderNo());
            return AgentSkillResult.ok(detail);
        } catch (Exception e) {
            return AgentSkillResult.fail("订单查询失败: " + e.getMessage());
        }
    }
}
