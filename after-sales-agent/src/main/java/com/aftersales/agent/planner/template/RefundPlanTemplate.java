package com.aftersales.agent.planner.template;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.orchestrator.ExecutionMode;
import com.aftersales.agent.planner.AgentPlan;
import com.aftersales.agent.planner.AgentPlanStep;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 退款意图的计划模板。
 *
 * 标准骨架：查询订单 → 资格检查 → 金额估算 → 创建售后 → 执行退款
 */
@Component
public class RefundPlanTemplate implements PlanTemplate {

    @Override
    public String supportedIntent() { return "REFUND_ESTIMATION"; }

    @Override
    public AgentPlan buildSkeleton(List<SkillDefinition> skills, AgentContext ctx) {
        AgentPlan plan = new AgentPlan();
        plan.setSummary("退款处理骨架计划");

        int step = 1;
        addIfAvailable(plan, step++, "order.query", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "eligibility.check", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "refund.estimate", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "after.sales.create", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step, "rag.retrieve", ExecutionMode.SEQUENTIAL, skills);

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
