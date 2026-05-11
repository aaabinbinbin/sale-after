package com.aftersales.agent.workflow.definition;

import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowDefinition;
import com.aftersales.agent.workflow.node.*;
import com.aftersales.biz.service.CompensationService;
import com.aftersales.common.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 自动补偿工作流定义。
 *
 * 用于小额自动补偿场景（物流延误、轻微商品瑕疵）：
 * 金额判断 → 执行补偿 → 发布事件
 * 超过阈值走人工审批。
 */
@Component
public class AutoCompensationWorkflow {

    private static final Logger log = LoggerFactory.getLogger(AutoCompensationWorkflow.class);

    private final CompensationService compensationService;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal MAX_AUTO_COMPENSATION = new BigDecimal("50");

    public AutoCompensationWorkflow(CompensationService compensationService,
                                    ApplicationEventPublisher eventPublisher) {
        this.compensationService = compensationService;
        this.eventPublisher = eventPublisher;
    }

    public WorkflowDefinition build(WorkflowContext ctx) {
        return WorkflowDefinition.builder("autoCompensation")
                .startWith("amountCheck")

                .node(new RuleNode("amountCheck", "自动补偿金额判断", c -> {
                    BigDecimal amount = c.getVariable("compensationAmount", BigDecimal.class);
                    if (amount == null) return RuleNode.RuleVerdicts.FAIL.withReason("缺少补偿金额");
                    if (amount.compareTo(MAX_AUTO_COMPENSATION) <= 0) return RuleNode.RuleVerdicts.LOW;
                    return RuleNode.RuleVerdicts.HIGH;
                }))
                .edge("amountCheck", "LOW", "executeCompensation")
                .edge("amountCheck", "HIGH", "manualApproval")

                .node(new ApprovalNode("manualApproval", "高额补偿需人工审批", "CUSTOMER_SERVICE"))
                .edge("manualApproval", "APPROVED", "executeCompensation")
                .edge("manualApproval", "REJECTED", null)

                .node(new ToolNode("executeCompensation", "执行补偿发放", c -> {
                    String afterSalesNo = c.getVariable("afterSalesNo", String.class);
                    String compensationType = c.getVariable("compensationType", String.class);
                    BigDecimal amount = c.getVariable("compensationAmount", BigDecimal.class);
                    if (afterSalesNo == null) return Map.of("error", "缺少 afterSalesNo");
                    String idemKey = "comp_" + afterSalesNo + "_" + IdGenerator.genTraceId();
                    Map<String, Object> cmd = new LinkedHashMap<>();
                    cmd.put("compensationType", compensationType);
                    cmd.put("compensationAmount", amount != null ? amount : BigDecimal.ZERO);
                    cmd.put("version", c.getVariable("version", Long.class));
                    try {
                        Map<String, Object> result = compensationService.grant(afterSalesNo, idemKey, cmd);
                        c.setVariable("compensationResult", result);
                        c.setVariable("afterSalesId", result.get("afterSalesId"));
                        c.setVariable("orderNo", result.get("orderNo"));
                        return result;
                    } catch (Exception e) {
                        log.error("补偿发放失败: {}", e.getMessage());
                        return Map.of("compensationStatus", "FAILED", "error", e.getMessage());
                    }
                }))
                .edge("executeCompensation", "publishEvent")

                .node(new EventNode("publishEvent", "AfterSalesCompletedEvent", eventPublisher,
                        c -> new com.aftersales.domain.event.AfterSalesCompletedEvent(
                                c.getVariable("afterSalesNo", String.class),
                                c.getVariable("afterSalesId", Long.class),
                                "COMPENSATION",
                                c.getVariable("orderNo", String.class))))
                .edge("publishEvent", null)
                .build();
    }
}
