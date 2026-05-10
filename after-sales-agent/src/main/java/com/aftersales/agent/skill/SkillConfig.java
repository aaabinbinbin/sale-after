package com.aftersales.agent.skill;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 技能自动注册配置。
 *
 * 在应用启动后自动将所有 AgentSkill Bean 注册到 SkillRegistry。
 */
@Component
public class SkillConfig {

    private final SkillRegistry skillRegistry;
    private final List<AgentSkill> skills;

    public SkillConfig(SkillRegistry skillRegistry, List<AgentSkill> skills) {
        this.skillRegistry = skillRegistry;
        this.skills = skills;
    }

    @PostConstruct
    public void registerAll() {
        for (AgentSkill skill : skills) {
            skillRegistry.register(skill);
        }
    }
}
