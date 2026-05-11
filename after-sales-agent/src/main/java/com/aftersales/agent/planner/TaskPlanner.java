package com.aftersales.agent.planner;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.orchestrator.ExecutionMode;
import com.aftersales.agent.planner.template.PlanTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务规划器。
 *
 * 增强版 Planning：
 * 1. 优先匹配 PlanTemplate（意图 → 骨架计划）
 * 2. LLM 在骨架上调整（而非从零生成）
 * 3. PlanValidator 校验合法性
 * 4. 失败时有 fallback 路径
 */
@Component
public class TaskPlanner {

    private static final Logger log = LoggerFactory.getLogger(TaskPlanner.class);

    private final Map<String, PlanTemplate> templates;
    private final PlanValidator validator;

    public TaskPlanner(List<PlanTemplate> templateList, PlanValidator validator) {
        this.templates = templateList.stream()
                .collect(Collectors.toMap(PlanTemplate::supportedIntent, t -> t, (a, b) -> a));
        this.validator = validator;
        log.info("TaskPlanner 加载 {} 个计划模板: {}", this.templates.size(), this.templates.keySet());
    }

    /**
     * 基于意图生成骨架计划。
     *
     * @param intent  意图类型
     * @param skills  可用 Skill 列表
     * @param ctx     Agent 上下文
     * @return 骨架 AgentPlan（LLM 可在此基础上调整）
     */
    public AgentPlan buildSkeleton(String intent, List<SkillDefinition> skills, AgentContext ctx) {
        PlanTemplate template = templates.get(intent);
        if (template != null) {
            AgentPlan plan = template.buildSkeleton(skills, ctx);
            plan.setPlanId("plan-" + UUID.randomUUID().toString().substring(0, 8));
            plan.setIntent(intent);

            // 校验
            List<String> errors = validator.validate(plan, skills);
            if (!errors.isEmpty()) {
                log.warn("PlanTemplate [{}] 生成计划存在校验问题: {}", intent, errors);
                // 自动修复：移除无效步骤
                plan.getSteps().removeIf(s -> {
                    boolean invalid = skills.stream().noneMatch(d -> d.getName().equals(s.getSkill()));
                    if (invalid) log.warn("移除无效步骤: {}", s.getSkill());
                    return invalid;
                });
            }

            log.info("TaskPlanner 基于模板 [{}] 生成计划, {} 步", intent, plan.getSteps().size());
            return plan;
        }

        // 无模板时生成默认计划
        log.info("TaskPlanner 无模板匹配 intent={}，使用默认计划", intent);
        return buildDefaultPlan(intent, skills);
    }

    /** 默认计划：将所有匹配 Skill 串行排列 */
    private AgentPlan buildDefaultPlan(String intent, List<SkillDefinition> skills) {
        AgentPlan plan = new AgentPlan();
        plan.setPlanId("plan-" + UUID.randomUUID().toString().substring(0, 8));
        plan.setIntent(intent);
        plan.setSummary("默认执行计划 (intent=" + intent + ")");

        int stepNo = 1;
        for (SkillDefinition skill : skills) {
            ExecutionMode mode = "HIGH".equals(skill.getRiskLevel())
                    ? ExecutionMode.CONFIRM_REQUIRED : ExecutionMode.SEQUENTIAL;
            plan.getSteps().add(new AgentPlanStep(stepNo++, skill.getName(), mode));
        }
        return plan;
    }

    /** 获取所有已注册模板 */
    public Map<String, PlanTemplate> getTemplates() { return Collections.unmodifiableMap(templates); }
}
