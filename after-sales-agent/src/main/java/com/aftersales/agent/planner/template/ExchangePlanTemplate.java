package com.aftersales.agent.planner.template;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.orchestrator.ExecutionMode;
import com.aftersales.agent.planner.AgentPlan;
import com.aftersales.agent.planner.AgentPlanStep;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 换货意图的计划模板。
 *
 * 标准骨架：查询订单 → 资格检查 → 库存检查 → 创建换货申请
 */
@Component
public class ExchangePlanTemplate implements PlanTemplate {

    @Override
    public String supportedIntent() { return "EXCHANGE_STOCK_CHECK"; }

    @Override
    public AgentPlan buildSkeleton(List<SkillDefinition> skills, AgentContext ctx) {
        AgentPlan plan = new AgentPlan();
        plan.setSummary("换货处理骨架计划");

        int step = 1;
        addIfAvailable(plan, step++, "order.query", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "eligibility.check", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "stock.check", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "after.sales.create", ExecutionMode.SEQUENTIAL, skills);

        return plan;
    }

    private void addIfAvailable(AgentPlan plan, int stepNo, String skillName,
                                 ExecutionMode mode, List<SkillDefinition> skills) {
        boolean exists = skills.stream().anyMatch(s -> s.getName().equals(skillName));
        if (exists) {
            plan.addStep(new AgentPlanStep(stepNo, skillName, mode));
        }
    }
}
