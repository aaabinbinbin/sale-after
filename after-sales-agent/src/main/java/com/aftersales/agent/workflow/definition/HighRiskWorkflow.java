package com.aftersales.agent.workflow.definition;

import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowDefinition;
import com.aftersales.agent.workflow.node.*;
import com.aftersales.biz.service.AfterSalesApplicationService;
import com.aftersales.biz.service.AfterSalesReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 高风险售后工作流定义。
 *
 * 用于风控检测到高风险时的升级处理流程：
 * 风险分析 → LLM 深度研判 → 人工审批 → 执行/拒绝 → 发布事件
 */
@Component
public class HighRiskWorkflow {

    private static final Logger log = LoggerFactory.getLogger(HighRiskWorkflow.class);

    private final AfterSalesApplicationService afterSalesService;
    private final AfterSalesReviewService reviewService;
    private final ChatClient chatClient;
    private final ApplicationEventPublisher eventPublisher;

    public HighRiskWorkflow(AfterSalesApplicationService afterSalesService,
                            AfterSalesReviewService reviewService,
                            ChatClient chatClient,
                            ApplicationEventPublisher eventPublisher) {
        this.afterSalesService = afterSalesService;
        this.reviewService = reviewService;
        this.chatClient = chatClient;
        this.eventPublisher = eventPublisher;
    }

    public WorkflowDefinition build(WorkflowContext ctx) {
        return WorkflowDefinition.builder("highRisk")
                .startWith("riskAnalysis")

                .node(new RuleNode("riskAnalysis", "风险汇总分析", c -> {
                    Integer riskScore = c.getVariable("riskScore", Integer.class);
                    if (riskScore != null && riskScore >= 80) {
                        c.setVariable("riskVerdict", "CRITICAL");
                        return RuleNode.RuleVerdicts.HIGH;
                    } else if (riskScore != null && riskScore >= 50) {
                        c.setVariable("riskVerdict", "MEDIUM");
                        return RuleNode.RuleVerdicts.HIGH;
                    }
                    c.setVariable("riskVerdict", "LOW");
                    return RuleNode.RuleVerdicts.LOW;
                }))
                .edge("riskAnalysis", "HIGH", "llmDeepAnalysis")
                .edge("riskAnalysis", "LOW", "manualApproval")

                .node(new LLMNode("llmDeepAnalysis", "LLM 高风险深度研判", chatClient,
                        """
                        你是售后风控专家。对高风险售后申请进行深度研判。
                        判断维度：
                        1. 用户行为模式是否异常
                        2. 售后理由是否合理
                        3. 金额是否与商品价值匹配
                        4. 是否存在欺诈嫌疑
                        输出：{"decision":"APPROVE|REJECT|ESCALATE","reason":"...","confidence":0.0~1.0}
                        只输出JSON。""",
                        c -> String.format("用户=%s, 售后单=%s, 金额=%s元, 风险因素=%s",
                                c.getUserId(),
                                c.getVariable("afterSalesNo", String.class),
                                c.getVariable("applyAmount", BigDecimal.class),
                                c.getVariable("riskReasons", Map.class)),
                        "APPROVE", "REJECT", "ESCALATE"))
                .edge("llmDeepAnalysis", "APPROVE", "manualApproval")
                .edge("llmDeepAnalysis", "REJECT", "rejectAfterSale")
                .edge("llmDeepAnalysis", "ESCALATE", "manualApproval")

                .node(new ApprovalNode("manualApproval", "高风险售后需客服主管审批", "ADMIN"))
                .edge("manualApproval", "APPROVED", "executeAction")
                .edge("manualApproval", "REJECTED", "rejectAfterSale")

                .node(new ToolNode("rejectAfterSale", "拒绝售后申请", c -> {
                    String afterSalesNo = c.getVariable("afterSalesNo", String.class);
                    Map<String, Object> cmd = new LinkedHashMap<>();
                    cmd.put("reviewRemark", "风控拦截：高风险售后申请");
                    reviewService.reject(afterSalesNo, cmd);
                    return Map.of("reviewStatus", "REJECTED", "reason", "风控拦截");
                }))
                .edge("rejectAfterSale", "publishEvent")

                .node(new ToolNode("executeAction", "执行审批通过后的操作", c -> {
                    String afterSalesNo = c.getVariable("afterSalesNo", String.class);
                    Map<String, Object> detail = afterSalesService.getDetail(afterSalesNo);
                    log.info("高风险售后审批通过: {}", afterSalesNo);
                    return Map.of("actionExecuted", true, "afterSalesNo", afterSalesNo, "detail", detail);
                }))
                .edge("executeAction", "publishEvent")

                .node(new EventNode("publishEvent", "AfterSalesCompletedEvent", eventPublisher,
                        c -> new com.aftersales.domain.event.AfterSalesCompletedEvent(
                                c.getVariable("afterSalesNo", String.class),
                                c.getVariable("afterSalesId", Long.class),
                                String.valueOf(c.getVariable("afterSalesType", Object.class)),
                                c.getVariable("orderNo", String.class))))
                .edge("publishEvent", null)
                .build();
    }
}
