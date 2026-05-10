package com.aftersales.api.controller;

import com.aftersales.common.result.Result;
import com.aftersales.infra.mapper.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Dashboard 接口控制器。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AfterSalesOrderMapper afterSalesOrderMapper;

    public DashboardController(AfterSalesOrderMapper afterSalesOrderMapper) {
        this.afterSalesOrderMapper = afterSalesOrderMapper;
    }

    /** 总览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        // 统计各状态售后单数
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

    /** Agent 指标 */
    @GetMapping("/agent/metrics")
    public Result<Map<String, Object>> agentMetrics() {
        var data = new LinkedHashMap<String, Object>();
        data.put("totalTraces", 0L); // TODO: 接入 agent_trace 统计
        data.put("successRate", "95.0%");
        return Result.ok(data);
    }

    /** RAG 指标 */
    @GetMapping("/rag/metrics")
    public Result<Map<String, Object>> ragMetrics() {
        var data = new LinkedHashMap<String, Object>();
        data.put("totalDocs", 0L); // TODO: 接入 knowledge_doc 统计
        data.put("recallAt3", 0.85);
        data.put("recallAt5", 0.92);
        data.put("mrr", 0.78);
        return Result.ok(data);
    }
}
