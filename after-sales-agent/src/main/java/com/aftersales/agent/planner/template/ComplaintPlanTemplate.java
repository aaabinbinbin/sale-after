package com.aftersales.agent.planner.template;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.orchestrator.ExecutionMode;
import com.aftersales.agent.planner.AgentPlan;
import com.aftersales.agent.planner.AgentPlanStep;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 投诉分析意图的计划模板。
 *
 * 标准骨架：查询订单 → 查询售后进度 → 风险分析 → 生成补偿建议
 */
@Component
public class ComplaintPlanTemplate implements PlanTemplate {

    @Override
    public String supportedIntent() { return "COMPLAINT_ANALYSIS"; }

    @Override
    public AgentPlan buildSkeleton(List<SkillDefinition> skills, AgentContext ctx) {
        AgentPlan plan = new AgentPlan();
        plan.setSummary("投诉分析骨架计划");

        int step = 1;
        addIfAvailable(plan, step++, "order.query", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "progress.query", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "rag.retrieve", ExecutionMode.SEQUENTIAL, skills);
        addIfAvailable(plan, step++, "compensation.suggest", ExecutionMode.SEQUENTIAL, skills);

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
