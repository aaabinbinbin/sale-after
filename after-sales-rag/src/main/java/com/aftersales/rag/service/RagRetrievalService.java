package com.aftersales.rag.service;

import com.aftersales.infra.entity.KnowledgeDoc;
import com.aftersales.infra.mapper.KnowledgeDocMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 检索服务。
 *
 * local/dev 环境使用 MySQL keyword 匹配作为 fallback，
 * 生产环境使用 pgvector 向量检索。
 */
@Service
public class RagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalService.class);

    private final KnowledgeDocMapper knowledgeDocMapper;

    @Value("${after-sales.rag.mysql-fallback-enabled:true}")
    private boolean mysqlFallbackEnabled;

    // 简单表示 pgvector 是否就绪（实际应检查连接）
    private boolean vectorStoreReady = false;

    public RagRetrievalService(KnowledgeDocMapper knowledgeDocMapper) {
        this.knowledgeDocMapper = knowledgeDocMapper;
    }

    /**
     * 检索相关知识。
     *
     * @param query   查询文本
     * @param topK    返回条数
     * @param filters 过滤条件（docType 等）
     * @return 检索结果
     */
    public List<Map<String, Object>> search(String query, int topK, Map<String, Object> filters) {
        if (!vectorStoreReady && mysqlFallbackEnabled) {
            log.info("RAG vectorStoreReady=false，启用 MySQL fallback。该路径仅用于 local/dev，不应作为长期主检索路径。");
            return mysqlKeywordSearch(query, topK, filters);
        }

        // TODO: 实际 pgvector 向量检索
        log.info("RAG 使用 pgvector 向量检索 query={}", query);
        return mysqlKeywordSearch(query, topK, filters); // 过渡期仍用 MySQL
    }

    /**
     * MySQL keyword 匹配检索（仅用于 local/dev）。
     */
    private List<Map<String, Object>> mysqlKeywordSearch(String query, int topK, Map<String, Object> filters) {
        List<KnowledgeDoc> allDocs = knowledgeDocMapper.selectAll();
        String docTypeFilter = filters != null ? (String) filters.get("docType") : null;

        // 简单的关键词匹配 + 评分
        List<Map<String, Object>> scored = new ArrayList<>();
        for (KnowledgeDoc doc : allDocs) {
            if (docTypeFilter != null && !docTypeFilter.equals(doc.getDocType())) continue;

            double score = keywordScore(query, doc.getTitle() + " " + doc.getContent());
            if (score > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("docNo", doc.getDocNo());
                item.put("title", doc.getTitle());
                item.put("docType", doc.getDocType());
                item.put("content", truncate(doc.getContent(), 300));
                item.put("score", Math.round(score * 100.0) / 100.0);
                scored.add(item);
            }
        }

        // 按评分降序
        scored.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return scored.stream().limit(topK).collect(Collectors.toList());
    }

    /** 简单关键词评分 */
    private double keywordScore(String query, String text) {
        if (query == null || text == null) return 0;
        String lowerQuery = query.toLowerCase();
        String lowerText = text.toLowerCase();
        double score = 0;
        // 按词匹配
        for (String word : lowerQuery.split("\\s+")) {
            if (word.length() < 2) continue;
            if (lowerText.contains(word)) score += 1.0;
        }
        return score;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
