package com.aftersales.agent.planner;

import com.aftersales.agent.orchestrator.ExecutionMode;
import java.util.*;

/**
 * Agent 执行计划中的一个步骤。
 *
 * 由 Agent Loop 中的 LLM 生成，每个步骤对应一个 Skill 的调用。
 * executionMode 决定步骤如何被执行。
 */
public class AgentPlanStep {

    private int stepNo;
    private String skill; // Skill 名称，须在 SkillRegistry 中存在
    private ExecutionMode executionMode = ExecutionMode.SEQUENTIAL;
    private List<Integer> dependsOn = new ArrayList<>(); // 依赖的步骤序号
    private boolean required = true;

    public AgentPlanStep() {}

    public AgentPlanStep(int stepNo, String skill, ExecutionMode executionMode) {
        this.stepNo = stepNo;
        this.skill = skill;
        this.executionMode = executionMode;
    }

    public int getStepNo() { return stepNo; }
    public void setStepNo(int stepNo) { this.stepNo = stepNo; }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public ExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(ExecutionMode executionMode) { this.executionMode = executionMode; }

    public List<Integer> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<Integer> dependsOn) { this.dependsOn = dependsOn; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
