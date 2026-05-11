package com.aftersales.api.controller;

import com.aftersales.common.result.Result;
import com.aftersales.infra.mapper.*;
import com.aftersales.rag.evaluation.RagEvaluationService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Dashboard 接口控制器。业务指标 + Agent/RAG 指标。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AfterSalesOrderMapper afterSalesOrderMapper;
    private final AgentTraceMapper agentTraceMapper;
    private final AgentLlmCallMapper agentLlmCallMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final RagEvaluationService ragEvaluationService;

    public DashboardController(AfterSalesOrderMapper afterSalesOrderMapper,
                                AgentTraceMapper agentTraceMapper,
                                AgentLlmCallMapper agentLlmCallMapper,
                                KnowledgeDocMapper knowledgeDocMapper,
                                RagEvaluationService ragEvaluationService) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
        this.agentTraceMapper = agentTraceMapper;
        this.agentLlmCallMapper = agentLlmCallMapper;
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.ragEvaluationService = ragEvaluationService;
    }

    /** 总览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        long totalCount = afterSalesOrderMapper.countByCondition(null, null, null, null);
        long pendingCount = afterSalesOrderMapper.countByCondition("PENDING_REVIEW", null, null, null);
        long completedCount = afterSalesOrderMapper.countByCondition("COMPLETED", null, null, null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalAfterSales", totalCount);
        data.put("pendingReview", pendingCount);
        data.put("completed", completedCount);
        return Result.ok(data);
    }

    /** 售后状态分布 */
    @GetMapping("/after-sales/status-count")
    public Result<Map<String, Object>> statusCount() {
        var data = new LinkedHashMap<String, Object>();
        for (String status : List.of("PENDING_REVIEW", "APPROVED", "REJECTED", "REFUND_PROCESSING",
                "EXCHANGE_PROCESSING", "COMPENSATION_PROCESSING", "COMPLETED")) {
            data.put(status, afterSalesOrderMapper.countByCondition(status, null, null, null));
        }
        return Result.ok(data);
    }

    /** Agent 指标（含 token 消耗） */
    @GetMapping("/agent/metrics")
    public Result<Map<String, Object>> agentMetrics() {
        var data = new LinkedHashMap<String, Object>();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String weekAgo = LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String startDate = weekAgo + " 00:00:00";
        String endDate = today + " 23:59:59";

        try {
            Map<String, Object> stats = agentLlmCallMapper.selectOverviewStats(startDate, endDate);
            data.put("totalLlmCalls", stats.getOrDefault("total_calls", 0L));
            data.put("successCalls", stats.getOrDefault("success_calls", 0L));
            data.put("failedCalls", stats.getOrDefault("failed_calls", 0L));
            data.put("totalTokens", stats.getOrDefault("total_tokens", 0L));
            data.put("avgLatencyMs", Math.round(
                    ((Number) stats.getOrDefault("avg_latency_ms", 0.0)).doubleValue()));

            // 按类型汇总 token
            List<Map<String, Object>> byType = agentLlmCallMapper.selectTokenUsageByType(startDate, endDate);
            data.put("tokenUsageByType", byType);

            // 按日期汇总
            List<Map<String, Object>> byDate = agentLlmCallMapper.selectTokenUsageByDate(startDate, endDate);
            data.put("tokenUsageByDate", byDate);
        } catch (Exception e) {
            data.put("error", "Agent 统计数据暂不可用: " + e.getMessage());
        }

        return Result.ok(data);
    }

    /** RAG 指标（真实评估 + 知识库统计） */
    @GetMapping("/rag/metrics")
    public Result<Map<String, Object>> ragMetrics() {
        var data = new LinkedHashMap<String, Object>();
        try {
            var docs = knowledgeDocMapper.selectAll();
            data.put("totalKnowledgeDocs", (long) docs.size());
        } catch (Exception e) {
            data.put("totalKnowledgeDocs", 0L);
        }
        // 调用评估服务获取真实指标
        try {
            Map<String, Object> evalResult = ragEvaluationService.evaluate();
            data.put("recallAt3", evalResult.get("recallAt3"));
            data.put("recallAt5", evalResult.get("recallAt5"));
            data.put("mrr", evalResult.get("mrr"));
            data.put("totalQuestions", evalResult.get("totalQuestions"));
        } catch (Exception e) {
            data.put("recallAt3", "N/A");
            data.put("recallAt5", "N/A");
            data.put("mrr", "N/A");
        }
        return Result.ok(data);
    }
}
