package com.aftersales.agent.planner;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.common.enums.AgentIntent;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent 计划器。
 *
 * 根据识别到的意图生成执行步骤，不同意图对应不同步骤组合。
 */
@Component
public class AgentPlanner {

    /**
     * 根据意图构建执行计划。
     */
    public AgentPlan buildPlan(AgentContext context, String intentCode, boolean needRag, boolean needTool) {
        AgentIntent intent = AgentIntent.fromCode(intentCode);
        AgentPlan plan = new AgentPlan();
        plan.setPlanId("plan-" + context.getTraceId());
        plan.setIntent(intentCode);
        plan.setSteps(new ArrayList<>());

        switch (intent) {
            case AFTER_SALES_POLICY_QA -> {
                // 政策问答只需要 RAG
                addStep(plan, 1, "rag.retrieve", "检索售后政策", needRag);
            }
            case ORDER_AFTER_SALES_ELIGIBILITY -> {
                addStep(plan, 1, "order.query", "查询订单信息", true);
                addStep(plan, 2, "after_sales.eligibility.check", "判断售后资格", true);
            }
            case CREATE_AFTER_SALES_APPLICATION -> {
                addStep(plan, 1, "order.query", "查询订单信息", true);
                addStep(plan, 2, "after_sales.eligibility.check", "判断售后资格", true);
                addStep(plan, 3, "rag.retrieve", "检索售后政策", needRag);
                addStep(plan, 4, "after_sales.application.draft", "生成售后申请草稿", true);
            }
            case QUERY_AFTER_SALES_PROGRESS -> {
                addStep(plan, 1, "after_sales.progress.query", "查询售后进度", true);
            }
            case REFUND_ESTIMATION -> {
                addStep(plan, 1, "order.query", "查询订单信息", true);
                addStep(plan, 2, "refund.estimate", "估算退款金额", true);
            }
            case EXCHANGE_STOCK_CHECK -> {
                addStep(plan, 1, "order.query", "查询订单信息", true);
                addStep(plan, 2, "exchange.stock.check", "检查换货库存", true);
            }
            case CUSTOMER_SERVICE_ASSISTANT -> {
                addStep(plan, 1, "order.query", "查询订单", needTool);
                addStep(plan, 2, "after_sales.progress.query", "查询售后详情", needTool);
                addStep(plan, 3, "rag.retrieve", "检索政策和案例", needRag);
            }
            case COMPLAINT_ANALYSIS -> {
                addStep(plan, 1, "order.query", "查询订单", true);
                addStep(plan, 2, "rag.retrieve", "检索历史案例", needRag);
            }
            default -> {
                // UNKNOWN：尝试用 RAG 回答
                addStep(plan, 1, "rag.retrieve", "检索相关知识", needRag);
            }
        }

        if (plan.getSteps().isEmpty()) {
            // 最少保证一个兜底步骤
            addStep(plan, 1, "rag.retrieve", "检索相关知识", true);
        }

        return plan;
    }

    private void addStep(AgentPlan plan, int stepNo, String skill, String name, boolean required) {
        if (!required) return; // 非必须步骤按需跳过
        AgentPlanStep step = new AgentPlanStep();
        step.setStepNo(stepNo);
        step.setSkill(skill);
        step.setName(name);
        step.setRequired(required);
        plan.getSteps().add(step);
    }

    /** 执行计划 */
    public static class AgentPlan {
        private String planId;
        private String intent;
        private List<AgentPlanStep> steps;
        private String status = "WAIT_EXECUTE";

        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public List<AgentPlanStep> getSteps() { return steps; }
        public void setSteps(List<AgentPlanStep> steps) { this.steps = steps; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
