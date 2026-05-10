package com.aftersales.agent.skill;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.rag.service.RagRetrievalService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * RAG 检索技能。
 *
 * 按需调用，不在普通进度查询时触发。
 */
@Component
public class RagRetrieveSkill implements AgentSkill {

    private final RagRetrievalService ragRetrievalService;

    public RagRetrieveSkill(RagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }

    @Override
    public String name() { return "rag.retrieve"; }

    @Override
    public boolean supports(AgentContext context, AgentPlanStep step) {
        return true; // RAG 总是可用
    }

    @Override
    public AgentSkillResult execute(AgentContext context, AgentPlanStep step) {
        try {
            List<Map<String, Object>> results = ragRetrievalService.search(
                    context.getUserInput(), 5, Map.of());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("hitCount", results.size());
            data.put("results", results);
            return AgentSkillResult.ok(data);
        } catch (Exception e) {
            return AgentSkillResult.fail("RAG检索失败: " + e.getMessage());
        }
    }
}
