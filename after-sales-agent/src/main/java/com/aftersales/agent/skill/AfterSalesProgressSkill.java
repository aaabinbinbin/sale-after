package com.aftersales.agent.skill;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.biz.service.AfterSalesApplicationService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 售后进度查询技能。
 */
@Component
public class AfterSalesProgressSkill implements AgentSkill {

    private final AfterSalesApplicationService applicationService;

    public AfterSalesProgressSkill(AfterSalesApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public String name() { return "after_sales.progress.query"; }

    @Override
    public boolean supports(AgentContext context, AgentPlanStep step) {
        return context.getAfterSalesNo() != null;
    }

    @Override
    public AgentSkillResult execute(AgentContext context, AgentPlanStep step) {
        try {
            Map<String, Object> detail = applicationService.getDetail(context.getAfterSalesNo());
            return AgentSkillResult.ok(detail);
        } catch (Exception e) {
            return AgentSkillResult.fail("售后查询失败: " + e.getMessage());
        }
    }
}
