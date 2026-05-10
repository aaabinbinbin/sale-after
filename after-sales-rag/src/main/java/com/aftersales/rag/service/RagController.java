package com.aftersales.rag.service;

import com.aftersales.common.result.Result;
import com.aftersales.infra.entity.KnowledgeDoc;
import com.aftersales.rag.evaluation.RagEvaluationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG 接口控制器。
 */
@RestController
@RequestMapping("/api")
public class RagController {

    private final KnowledgeDocService knowledgeDocService;
    private final KnowledgeBuildService knowledgeBuildService;
    private final RagRetrievalService ragRetrievalService;
    private final RagEvaluationService ragEvaluationService;

    public RagController(KnowledgeDocService knowledgeDocService,
                          KnowledgeBuildService knowledgeBuildService,
                          RagRetrievalService ragRetrievalService,
                          RagEvaluationService ragEvaluationService) {
        this.knowledgeDocService = knowledgeDocService;
        this.knowledgeBuildService = knowledgeBuildService;
        this.ragRetrievalService = ragRetrievalService;
        this.ragEvaluationService = ragEvaluationService;
    }

    /** RAG 检索 */
    @PostMapping("/rag/search")
    public Result<List<Map<String, Object>>> search(@RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        int topK = body.containsKey("topK") ? toInt(body.get("topK")) : 5;
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) body.get("filters");
        return Result.ok(ragRetrievalService.search(query, topK, filters));
    }

    /** RAG 评估 */
    @PostMapping("/rag/evaluate")
    public Result<Map<String, Object>> evaluate() {
        return Result.ok(ragEvaluationService.evaluate());
    }

    /** 知识文档列表 */
    @GetMapping("/knowledge/docs")
    public Result<List<KnowledgeDoc>> listDocs(@RequestParam(required = false) String docType) {
        if (docType != null) {
            return Result.ok(knowledgeDocService.listByType(docType));
        }
        return Result.ok(knowledgeDocService.listAll());
    }

    /** 创建知识文档 */
    @PostMapping("/knowledge/docs")
    public Result<KnowledgeDoc> createDoc(@RequestBody Map<String, String> body) {
        KnowledgeDoc doc = knowledgeDocService.create(
                body.get("docType"), body.get("title"),
                body.getOrDefault("sourceType", "MANUAL_ENTRY"),
                body.get("sourceId"), body.get("content"));
        return Result.ok(doc);
    }

    /** 从售后单构建知识 */
    @PostMapping("/knowledge/build-tasks")
    public Result<KnowledgeDoc> buildFromAfterSales(@RequestBody Map<String, String> body) {
        String afterSalesNo = body.get("afterSalesNo");
        KnowledgeDoc doc = knowledgeBuildService.buildFromAfterSales(afterSalesNo);
        return Result.ok(doc);
    }

    private int toInt(Object obj) {
        if (obj == null) return 5;
        return obj instanceof Number n ? n.intValue() : Integer.parseInt(obj.toString());
    }
}
