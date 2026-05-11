package com.aftersales.agent.planner.template;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.planner.AgentPlan;

import java.util.List;

/**
 * 计划模板接口。
 *
 * 每种意图对应一个模板，定义标准的步骤骨架。
 * LLM 在骨架基础上进行调整（增删步骤、调整参数），而非从零生成计划。
 */
public interface PlanTemplate {

    /** 模板支持的意图类型 */
    String supportedIntent();

    /** 构建骨架计划 */
    AgentPlan buildSkeleton(List<SkillDefinition> availableSkills, AgentContext ctx);
}
