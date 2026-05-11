package com.aftersales.agent.planner;

import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.orchestrator.ExecutionMode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 计划校验器。
 *
 * 检查：
 * 1. 步骤 Skill 名称是否在白名单内
 * 2. 依赖是否合法（dependsOn 引用的步骤号必须存在且序号更小）
 * 3. 循环检测
 * 4. CONFIRM_REQUIRED 只能用于 HIGH 风险 Skill
 */
@Component
public class PlanValidator {

    /**
     * 校验计划。
     *
     * @return 错误信息列表，空列表表示通过
     */
    public List<String> validate(AgentPlan plan, List<SkillDefinition> availableSkills) {
        List<String> errors = new ArrayList<>();
        Set<String> skillNames = new HashSet<>();
        for (SkillDefinition s : availableSkills) skillNames.add(s.getName());

        Set<Integer> stepNos = new HashSet<>();
        for (AgentPlanStep step : plan.getSteps()) {
            stepNos.add(step.getStepNo());
        }

        for (AgentPlanStep step : plan.getSteps()) {
            // 1. 白名单检查
            if (!skillNames.contains(step.getSkill())) {
                errors.add("步骤 " + step.getStepNo() + " Skill [" + step.getSkill() + "] 不在可用列表");
            }

            // 2. 依赖检查
            if (step.getDependsOn() != null) {
                for (int depNo : step.getDependsOn()) {
                    if (!stepNos.contains(depNo)) {
                        errors.add("步骤 " + step.getStepNo() + " 依赖步骤 " + depNo + " 不存在");
                    }
                    if (depNo >= step.getStepNo()) {
                        errors.add("步骤 " + step.getStepNo() + " 依赖步骤 " + depNo + " 序号必须更小");
                    }
                }
            }

            // 3. CONFIRM_REQUIRED 风险检查
            if (step.getExecutionMode() == ExecutionMode.CONFIRM_REQUIRED) {
                SkillDefinition def = availableSkills.stream()
                        .filter(s -> s.getName().equals(step.getSkill())).findFirst().orElse(null);
                if (def != null && !"HIGH".equals(def.getRiskLevel())) {
                    errors.add("步骤 " + step.getStepNo() + " CONFIRM_REQUIRED 只能用于 HIGH 风险 Skill");
                }
            }
        }

        // 4. 循环检测
        if (hasCycle(plan)) {
            errors.add("计划存在循环依赖");
        }

        return errors;
    }

    /** 简单循环检测：dependsOn 引用了间接形成环的步骤 */
    private boolean hasCycle(AgentPlan plan) {
        // 对每个步骤做 DFS，检查是否回到自身
        for (AgentPlanStep step : plan.getSteps()) {
            Set<Integer> visited = new HashSet<>();
            if (dfsCycle(step.getStepNo(), plan, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsCycle(int stepNo, AgentPlan plan, Set<Integer> visited) {
        if (!visited.add(stepNo)) return true; // 已访问过 → 环
        AgentPlanStep step = plan.getSteps().stream()
                .filter(s -> s.getStepNo() == stepNo).findFirst().orElse(null);
        if (step == null || step.getDependsOn() == null) return false;
        for (int dep : step.getDependsOn()) {
            if (dfsCycle(dep, plan, new HashSet<>(visited))) return true;
        }
        return false;
    }
}
