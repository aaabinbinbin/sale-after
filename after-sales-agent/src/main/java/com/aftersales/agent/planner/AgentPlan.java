package com.aftersales.agent.planner;

import java.util.*;

/**
 * Agent 执行计划。
 *
 * 由 Agent Loop 中的 LLM 生成，包含一组有序的执行步骤。
 * 和 doc 05 的设计一致：planId + intent + steps + status。
 */
public class AgentPlan {

    /** 计划唯一标识 */
    private String planId;

    /** 意图类型 */
    private String intent;

    /** 计划摘要（LLM 生成，用于日志和 debug） */
    private String summary;

    /** 执行步骤列表 */
    private List<AgentPlanStep> steps = new ArrayList<>();

    /** 计划状态 */
    private String status = "WAIT_EXECUTE";

    /** 创建时间 */
    private long createdAt = System.currentTimeMillis();

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<AgentPlanStep> getSteps() { return steps; }
    public void setSteps(List<AgentPlanStep> steps) { this.steps = steps; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** 快捷方法：添加一个步骤 */
    public void addStep(int stepNo, String skill, String mode) {
        AgentPlanStep step = new AgentPlanStep();
        step.setStepNo(stepNo);
        step.setSkill(skill);
        step.setExecutionMode(com.aftersales.agent.orchestrator.ExecutionMode.valueOf(mode));
        this.steps.add(step);
    }

    /** 添加一个步骤对象 */
    public void addStep(AgentPlanStep step) {
        this.steps.add(step);
    }

    /** 判断计划是否包含需要确认的步骤 */
    public boolean hasConfirmStep() {
        return steps.stream().anyMatch(s ->
                s.getExecutionMode() == com.aftersales.agent.orchestrator.ExecutionMode.CONFIRM_REQUIRED);
    }
}
