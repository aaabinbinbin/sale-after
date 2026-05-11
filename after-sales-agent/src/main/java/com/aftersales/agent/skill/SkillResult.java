package com.aftersales.agent.skill;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;

import java.util.Map;

/**
 * 技能执行结果，与 AgentSkill.SkillResult 等价，作为顶级类便于外部引用。
 */
public class SkillResult {

    private final boolean success;
    private final Map<String, Object> data;
    private final String error;

    private SkillResult(boolean success, Map<String, Object> data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static SkillResult ok(Map<String, Object> data) {
        return new SkillResult(true, data, null);
    }

    public static SkillResult fail(String error) {
        return new SkillResult(false, null, error);
    }

    public boolean isSuccess() { return success; }
    public Map<String, Object> getData() { return data; }
    public String getError() { return error; }
}
