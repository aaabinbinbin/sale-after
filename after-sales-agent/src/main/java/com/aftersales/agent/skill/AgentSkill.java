package com.aftersales.agent.skill;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;
import java.util.Map;

/**
 * Agent 技能接口。
 *
 * 每个 Skill 代表一个可被 Agent 调用的业务能力。
 * 行为描述放在 prompts/skills/*.md 中，此接口只定义执行契约。
 *
 * 约束（来自 CLAUDE.md）：
 * - Skill 调用业务 Service，不直接操作 Mapper
 * - 不能绕过权限和状态机执行高风险动作
 */
public interface AgentSkill {

    /** 返回技能名称，对应 md 文件名（不含 .md）。 */
    String name();

    /** 判断当前上下文是否支持执行该技能。 */
    boolean supports(AgentContext context, AgentPlanStep step);

    /** 执行技能，返回结构化结果。 */
    SkillResult execute(AgentContext context, AgentPlanStep step);

    /** 技能执行结果 */
    record SkillResult(boolean success, Map<String, Object> data, String error) {
        public static SkillResult ok(Map<String, Object> data) {
            return new SkillResult(true, data, null);
        }
        public static SkillResult fail(String error) {
            return new SkillResult(false, null, error);
        }
    }
}
