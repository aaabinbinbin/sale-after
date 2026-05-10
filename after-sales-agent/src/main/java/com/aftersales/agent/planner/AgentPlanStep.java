package com.aftersales.agent.planner;

/**
 * Agent 执行计划步骤。
 */
public class AgentPlanStep {

    private int stepNo;
    private String skill; // 技能名称
    private String name; // 中文描述
    private boolean required; // 是否必须

    public int getStepNo() { return stepNo; }
    public void setStepNo(int stepNo) { this.stepNo = stepNo; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
