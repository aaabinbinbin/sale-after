package com.aftersales.agent.workflow.definition;

import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowDefinition;
import com.aftersales.agent.workflow.node.*;
import com.aftersales.biz.service.AfterSalesApplicationService;
import com.aftersales.biz.service.RefundService;
import com.aftersales.common.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 退款工作流定义。
 *
 * 流程：资格校验 → 金额判断 → (高金额走 LLM 决策) → 执行退款 → 发布事件
 */
@Component
public class RefundWorkflow {

    private static final Logger log = LoggerFactory.getLogger(RefundWorkflow.class);

    private final AfterSalesApplicationService afterSalesService;
    private final RefundService refundService;
    private final ChatClient chatClient;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal AUTO_APPROVE_MAX = new BigDecimal("500");

    public RefundWorkflow(AfterSalesApplicationService afterSalesService,
                          RefundService refundService,
                          ChatClient chatClient,
                          ApplicationEventPublisher eventPublisher) {
        this.afterSalesService = afterSalesService;
        this.refundService = refundService;
        this.chatClient = chatClient;
        this.eventPublisher = eventPublisher;
    }

    public WorkflowDefinition build(WorkflowContext ctx) {
        String wfId = ctx.getWorkflowId();
        return WorkflowDefinition.builder("refund")
                .startWith("amountCheck")
                .node(new RuleNode("amountCheck", "退款金额判断", c -> {
                    BigDecimal amount = c.getVariable("applyAmount", BigDecimal.class);
                    if (amount == null) return RuleNode.RuleVerdicts.FAIL.withReason("缺少退款金额");
                    if (amount.compareTo(AUTO_APPROVE_MAX) <= 0) return RuleNode.RuleVerdicts.LOW;
                    return RuleNode.RuleVerdicts.HIGH;
                }))
                .edge("amountCheck", "LOW", "executeRefund")
                .edge("amountCheck", "HIGH", "llmDecision")

                .node(new LLMNode("llmDecision", "LLM 退款决策", chatClient,
                        """
                        你是售后退款审核助手。根据退款信息判定是否批准退款。
                        判定标准：
                        - 金额 > 1000 且非 VIP → 建议人工审核
                        - 退款理由不充分 → 建议拒绝
                        - 其他正常情况 → 建议批准
                        输出：{"decision":"APPROVE|REJECT|MANUAL_REVIEW","reason":"理由"}
                        只输出JSON。""",
                        c -> String.format("afterSalesNo=%s, applyAmount=%s元, userId=%s",
                                c.getVariable("afterSalesNo"), c.getVariable("applyAmount"), c.getUserId()),
                        "APPROVE", "REJECT", "MANUAL_REVIEW"))
                .edge("llmDecision", "APPROVE", "executeRefund")
                .edge("llmDecision", "REJECT", null)
                .edge("llmDecision", "MANUAL_REVIEW", "manualApproval")

                .node(new ApprovalNode("manualApproval", "高额退款需人工审批", "CUSTOMER_SERVICE"))
                .edge("manualApproval", "APPROVED", "executeRefund")
                .edge("manualApproval", "REJECTED", null)

                .node(new ToolNode("executeRefund", "执行退款", c -> {
                    String afterSalesNo = c.getVariable("afterSalesNo", String.class);
                    BigDecimal amount = c.getVariable("applyAmount", BigDecimal.class);
                    if (afterSalesNo == null) return Map.of("error", "缺少 afterSalesNo");
                    String idemKey = "refund_" + afterSalesNo + "_" + IdGenerator.genTraceId();
                    Map<String, Object> cmd = new LinkedHashMap<>();
                    cmd.put("refundAmount", amount != null ? amount : BigDecimal.ZERO);
                    cmd.put("version", c.getVariable("version", Long.class));
                    try {
                        Map<String, Object> result = refundService.executeRefund(afterSalesNo, idemKey, cmd);
                        c.setVariable("refundResult", result);
                        c.setVariable("afterSalesId", result.get("afterSalesId"));
                        c.setVariable("orderNo", result.get("orderNo"));
                        return result;
                    } catch (Exception e) {
                        log.error("退款执行失败: {}", e.getMessage());
                        return Map.of("refundStatus", "FAILED", "error", e.getMessage());
                    }
                }))
                .edge("executeRefund", "publishEvent")

                .node(new EventNode("publishEvent", "AfterSalesCompletedEvent", eventPublisher,
                        c -> {
                            String afterSalesNo = c.getVariable("afterSalesNo", String.class);
                            Long afterSalesId = c.getVariable("afterSalesId", Long.class);
                            String orderNo = c.getVariable("orderNo", String.class);
                            return new com.aftersales.domain.event.AfterSalesCompletedEvent(
                                    afterSalesNo, afterSalesId, "REFUND_ONLY", orderNo);
                        }))
                .edge("publishEvent", null)
                .build();
    }
}
