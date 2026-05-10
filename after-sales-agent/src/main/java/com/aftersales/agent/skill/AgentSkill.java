package com.aftersales.agent.skill;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;
import java.util.Map;

/**
 * Agent 技能接口。
 *
 * 每个技能代表一个可被 Agent 调用的业务能力。
 * 技能内部只能调用业务 Service，不能直接操作 Mapper，也不能绕过权限和状态机。
 */
public interface AgentSkill {

    /** 技能名称 */
    String name();

    /** 判断当前上下文是否支持执行该技能 */
    boolean supports(AgentContext context, AgentPlanStep step);

    /** 执行技能，返回结构化结果 */
    AgentSkillResult execute(AgentContext context, AgentPlanStep step);

    /** 技能执行结果 */
    record AgentSkillResult(boolean success, Map<String, Object> data, String error) {
        public static AgentSkillResult ok(Map<String, Object> data) {
            return new AgentSkillResult(true, data, null);
        }
        public static AgentSkillResult fail(String error) {
            return new AgentSkillResult(false, null, error);
        }
    }
}
