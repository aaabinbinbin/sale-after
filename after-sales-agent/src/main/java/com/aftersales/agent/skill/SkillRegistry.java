package com.aftersales.agent.skill;

import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 技能注册表。
 *
 * 统一管理所有 Agent 技能，AgentFacade 通过注册表获取技能，不直接依赖具体实现。
 */
@Component
public class SkillRegistry {

    private final Map<String, AgentSkill> skills = new LinkedHashMap<>();

    /**
     * 注册技能（由 Spring Bean 自动注入或在构造后手动注册）。
     */
    public void register(AgentSkill skill) {
        skills.put(skill.name(), skill);
    }

    /**
     * 获取技能。
     */
    public AgentSkill get(String name) {
        AgentSkill skill = skills.get(name);
        if (skill == null) {
            throw new BusinessException(ErrorCode.AGENT_SKILL_NOT_FOUND, "技能不存在: " + name);
        }
        return skill;
    }

    /** 获取所有已注册技能名称 */
    public Set<String> skillNames() {
        return Collections.unmodifiableSet(skills.keySet());
    }

    /** 获取所有技能 */
    public Collection<AgentSkill> all() {
        return Collections.unmodifiableCollection(skills.values());
    }
}
