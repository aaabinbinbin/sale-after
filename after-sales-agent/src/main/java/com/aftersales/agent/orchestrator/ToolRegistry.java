package com.aftersales.agent.orchestrator;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.skill.SkillResult;
import com.aftersales.biz.service.*;
import com.aftersales.infra.mapper.SkuStockMapper;
import com.aftersales.rag.service.RagRetrievalService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiFunction;

/**
 * 工具注册表。
 *
 * 维护逻辑工具名 → Java Service 方法 的映射。
 * Skill.md 中声明 requiredTools: [orderQueryTool]，执行时通过此注册表找到对应方法。
 *
 * 设计模式：策略模式 + 注册表模式——工具是策略，注册表是查找入口。
 */
@Component
public class ToolRegistry {

    /** 逻辑工具名 → 执行函数 */
    private final Map<String, BiFunction<AgentContext, SkillDefinition, SkillResult>> registry = new LinkedHashMap<>();

    private final OrderBizService orderBizService;
    private final AfterSalesApplicationService afterSalesApplicationService;
    private final RagRetrievalService ragRetrievalService;
    private final SkuStockMapper skuStockMapper;

    public ToolRegistry(OrderBizService orderBizService,
                         AfterSalesApplicationService afterSalesApplicationService,
                         RagRetrievalService ragRetrievalService,
                         SkuStockMapper skuStockMapper) {
        this.orderBizService = orderBizService;
        this.afterSalesApplicationService = afterSalesApplicationService;
        this.ragRetrievalService = ragRetrievalService;
        this.skuStockMapper = skuStockMapper;
        registerDefaults();
    }

    /** 注册所有内置工具映射 */
    private void registerDefaults() {
        // 订单查询
        registry.put("orderQueryTool", (ctx, def) -> {
            String orderNo = ctx.getOrderNo();
            if (orderNo == null) return SkillResult.fail("缺少 orderNo");
            try {
                Map<String, Object> detail = orderBizService.getOrderDetail(orderNo);
                return SkillResult.ok(detail);
            } catch (Exception e) {
                return SkillResult.fail("订单查询失败: " + e.getMessage());
            }
        });

        // 售后进度查询
        registry.put("afterSalesProgressTool", (ctx, def) -> {
            String asNo = ctx.getAfterSalesNo();
            if (asNo == null) return SkillResult.fail("缺少 afterSalesNo");
            try {
                Map<String, Object> detail = afterSalesApplicationService.getDetail(asNo);
                return SkillResult.ok(detail);
            } catch (Exception e) {
                return SkillResult.fail("售后查询失败: " + e.getMessage());
            }
        });

        // RAG 检索
        registry.put("ragRetrieveTool", (ctx, def) -> {
            try {
                List<Map<String, Object>> results = ragRetrievalService.search(
                        ctx.getUserInput(), 5, Map.of());
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("hitCount", results.size());
                data.put("results", results);
                return SkillResult.ok(data);
            } catch (Exception e) {
                return SkillResult.fail("RAG 检索失败: " + e.getMessage());
            }
        });

        // 库存检查
        registry.put("stockCheckTool", (ctx, def) -> {
            Object skuIdObj = ctx.getExtra("targetSkuId");
            Long skuId = toLong(skuIdObj);
            if (skuId == null) return SkillResult.fail("缺少 targetSkuId");
            var stock = skuStockMapper.selectBySkuId(skuId);
            if (stock == null) return SkillResult.fail("SKU 不存在: " + skuId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("skuId", skuId);
            data.put("availableStock", stock.getAvailableStock());
            data.put("canExchange", stock.getAvailableStock() > 0);
            return SkillResult.ok(data);
        });
    }

    /**
     * 执行工具调用。
     *
     * @param toolName 逻辑工具名（如 "orderQueryTool"）
     * @param ctx      Agent 上下文
     * @param def      Skill 定义（用于获取 prompt 等元数据）
     * @return 执行结果
     */
    public SkillResult execute(String toolName, AgentContext ctx, SkillDefinition def) {
        BiFunction<AgentContext, SkillDefinition, SkillResult> tool = registry.get(toolName);
        if (tool == null) {
            return SkillResult.fail("未知工具: " + toolName);
        }
        return tool.apply(ctx, def);
    }

    /** 注册自定义工具（扩展点） */
    public void register(String toolName, BiFunction<AgentContext, SkillDefinition, SkillResult> tool) {
        registry.put(toolName, tool);
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        return obj != null ? Long.valueOf(obj.toString()) : null;
    }
}
