package com.aftersales.agent.workflow.definition;

import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowDefinition;
import com.aftersales.agent.workflow.node.*;
import com.aftersales.biz.service.ExchangeService;
import com.aftersales.infra.entity.SkuStock;
import com.aftersales.infra.mapper.SkuStockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 换货工作流定义。
 *
 * 流程：库存检查 → 锁定库存 → (退货判断) → 收货确认 → 发货 → 发布事件
 */
@Component
public class ExchangeWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ExchangeWorkflow.class);

    private final ExchangeService exchangeService;
    private final SkuStockMapper skuStockMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ExchangeWorkflow(ExchangeService exchangeService,
                            SkuStockMapper skuStockMapper,
                            ApplicationEventPublisher eventPublisher) {
        this.exchangeService = exchangeService;
        this.skuStockMapper = skuStockMapper;
        this.eventPublisher = eventPublisher;
    }

    public WorkflowDefinition build(WorkflowContext ctx) {
        return WorkflowDefinition.builder("exchange")
                .startWith("stockCheck")
                .node(new RuleNode("stockCheck", "换货库存检查", c -> {
                    Long exchangeSkuId = c.getVariable("exchangeSkuId", Long.class);
                    if (exchangeSkuId == null)
                        return RuleNode.RuleVerdicts.FAIL.withReason("缺少换货 SKU");
                    SkuStock stock = skuStockMapper.selectBySkuId(exchangeSkuId);
                    if (stock == null) return RuleNode.RuleVerdicts.FAIL.withReason("SKU 不存在");
                    if (stock.getAvailableStock() <= 0) return RuleNode.RuleVerdicts.FAIL.withReason("库存不足");
                    c.setVariable("availableStock", stock.getAvailableStock());
                    return RuleNode.RuleVerdicts.PASS;
                }))
                .edge("stockCheck", "PASS", "lockStock")
                .edge("stockCheck", "FAIL", null)

                .node(new ToolNode("lockStock", "锁定换货库存", c -> {
                    String afterSalesNo = c.getVariable("afterSalesNo", String.class);
                    try {
                        exchangeService.lockStock(afterSalesNo);
                        return Map.of("stockLocked", true);
                    } catch (Exception e) {
                        return Map.of("stockLocked", false, "error", e.getMessage());
                    }
                }))
                .edge("lockStock", "needReturnCheck")

                .node(new RuleNode("needReturnCheck", "判断是否需先退货", c -> {
                    Boolean needReturn = c.getVariable("needReturn", Boolean.class);
                    return Boolean.TRUE.equals(needReturn) ? RuleNode.RuleVerdicts.YES : RuleNode.RuleVerdicts.NO;
                }))
                .edge("needReturnCheck", "YES", "waitReturnReceive")
                .edge("needReturnCheck", "NO", "shipExchange")

                .node(new DelayNode("waitReturnReceive", "等待买家退货并确认收货", 0,
                        "等待退货物流，客服确认收货后继续"))
                .edge("waitReturnReceive", "shipExchange")

                .node(new ToolNode("shipExchange", "换货发货", c -> {
                    String afterSalesNo = c.getVariable("afterSalesNo", String.class);
                    Map<String, Object> cmd = new LinkedHashMap<>();
                    cmd.put("logisticsCompany", c.getVariable("logisticsCompany"));
                    cmd.put("logisticsNo", c.getVariable("logisticsNo"));
                    try {
                        exchangeService.ship(afterSalesNo, cmd);
                        return Map.of("exchangeStatus", "SHIPPED");
                    } catch (Exception e) {
                        return Map.of("exchangeStatus", "FAILED", "error", e.getMessage());
                    }
                }))
                .edge("shipExchange", "publishEvent")

                .node(new EventNode("publishEvent", "AfterSalesCompletedEvent", eventPublisher,
                        c -> new com.aftersales.domain.event.AfterSalesCompletedEvent(
                                c.getVariable("afterSalesNo", String.class),
                                c.getVariable("afterSalesId", Long.class),
                                "EXCHANGE",
                                c.getVariable("orderNo", String.class))))
                .edge("publishEvent", null)
                .build();
    }
}
