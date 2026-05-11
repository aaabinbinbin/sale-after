package com.aftersales.agent.registry;

import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.loader.SkillLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 技能注册中心 + 分层匹配引擎。
 *
 * 根据意图和用户输入，分层匹配最相关的 Top-K 个 Skill：
 * Layer 1: 关键词匹配（用户输入 vs Skill.keywords）→ 最快，命中率 ~80%
 * Layer 2: Tag 匹配（intent 类型 vs Skill.tags）→ 快，累计命中率 ~95%
 * Layer 3: Embedding 语义匹配 → 留接口，当前未启用
 * Layer 4: LLM 分类 → 留接口，兜底
 *
 * 设计模式：责任链模式——每层是一个 Matcher，任一命中即返回。
 */
@Component
public class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);
    private static final int DEFAULT_TOP_K = 5;

    private final SkillLoader skillLoader;

    public SkillRegistry(SkillLoader skillLoader) {
        this.skillLoader = skillLoader;
    }

    /**
     * 分层匹配：根据意图和用户输入，返回最相关的 Skill 定义列表。
     *
     * @param intent    意图类型代码
     * @param userInput 用户原始输入
     * @return 匹配的 Skill 定义列表（按匹配优先级排序）
     */
    public List<SkillDefinition> match(String intent, String userInput) {
        // Layer 1: 关键词匹配
        List<SkillDefinition> matched = keywordMatch(userInput);
        if (!matched.isEmpty()) {
            log.debug("Skill 匹配 Layer 1 (关键词) 命中 {} 个", matched.size());
            return topK(matched, DEFAULT_TOP_K);
        }

        // Layer 2: Tag 匹配
        matched = tagMatch(intent);
        if (!matched.isEmpty()) {
            log.debug("Skill 匹配 Layer 2 (Tag) 命中 {} 个, intent={}", matched.size(), intent);
            return topK(matched, DEFAULT_TOP_K);
        }

        // Layer 3-4: 留接口，当前降级为返回全部非 HIGH 风险的 Skill
        log.info("Skill 匹配 Layer 1-2 未命中，降级返回全部可用 Skill, intent={}", intent);
        return skillLoader.allSkills().stream()
                .filter(s -> !"HIGH".equals(s.getRiskLevel()))
                .limit(DEFAULT_TOP_K)
                .collect(Collectors.toList());
    }

    /**
     * Layer 1: 关键词匹配。
     * 用户输入中的词 vs Skill.keywords，取交集。
     */
    List<SkillDefinition> keywordMatch(String userInput) {
        if (userInput == null || userInput.isBlank()) return List.of();
        String input = userInput.toLowerCase();

        // 按匹配关键词数量排序
        return skillLoader.allSkills().stream()
                .filter(skill -> {
                    long matchCount = skill.getKeywords().stream()
                            .filter(kw -> input.contains(kw.toLowerCase()))
                            .count();
                    return matchCount > 0; // 至少匹配一个关键词
                })
                .sorted((a, b) -> {
                    long countA = a.getKeywords().stream().filter(kw -> input.contains(kw.toLowerCase())).count();
                    long countB = b.getKeywords().stream().filter(kw -> input.contains(kw.toLowerCase())).count();
                    return Long.compare(countB, countA); // 匹配更多关键词的排在前面
                })
                .collect(Collectors.toList());
    }

    /**
     * Layer 2: Tag 匹配。
     * 意图类型 vs Skill.tags。
     */
    List<SkillDefinition> tagMatch(String intent) {
        if (intent == null || intent.isBlank()) return List.of();

        // 将意图转为小写 tag（如 CREATE_AFTER_SALES_APPLICATION → create, after_sales, application）
        Set<String> intentTags = Arrays.stream(intent.toLowerCase().split("_"))
                .collect(Collectors.toSet());

        return skillLoader.allSkills().stream()
                .filter(skill -> skill.getTags().stream()
                        .map(String::toLowerCase)
                        .anyMatch(intentTags::contains))
                .collect(Collectors.toList());
    }

    /** Top-K 截取 */
    private List<SkillDefinition> topK(List<SkillDefinition> list, int k) {
        return list.stream().limit(k).collect(Collectors.toList());
    }
}
