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
 * 优先使用 Embedding + 向量检索，不可用时降级为 MySQL keyword 匹配。
 * local/dev 的 MySQL fallback 标记 [DEV]，生产路径用 pgvector。
 */
@Service
public class RagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalService.class);

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @Value("${after-sales.rag.mysql-fallback-enabled:true}")
    private boolean mysqlFallbackEnabled;

    public RagRetrievalService(KnowledgeDocMapper knowledgeDocMapper,
                                EmbeddingService embeddingService,
                                VectorStoreService vectorStoreService) {
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 检索相关知识。优先向量检索，降级 keyword。
     */
    public List<Map<String, Object>> search(String query, int topK, Map<String, Object> filters) {
        // 尝试向量检索
        try {
            float[] queryVec = embeddingService.embed(query);
            List<Map<String, Object>> vectorResults = vectorStoreService.search(queryVec, topK * 2, filters);
            if (!vectorResults.isEmpty()) {
                log.info("RAG 向量检索命中 {} 条 query={}", vectorResults.size(), truncate(query, 50));
                return vectorResults.stream().limit(topK).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.info("向量检索不可用: {}", e.getMessage());
        }

        // 降级 MySQL keyword
        if (mysqlFallbackEnabled) {
            log.info("[DEV] RAG 向量库空或不可用，启用 MySQL fallback。该路径仅用于 local/dev。");
            return mysqlKeywordSearch(query, topK, filters);
        }

        return List.of();
    }

    /** MySQL keyword 匹配检索 */
    private List<Map<String, Object>> mysqlKeywordSearch(String query, int topK, Map<String, Object> filters) {
        List<KnowledgeDoc> allDocs = knowledgeDocMapper.selectAll();
        String docTypeFilter = filters != null ? (String) filters.get("docType") : null;

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
        scored.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return scored.stream().limit(topK).collect(Collectors.toList());
    }

    private double keywordScore(String query, String text) {
        if (query == null || text == null) return 0;
        String lq = query.toLowerCase(), lt = text.toLowerCase();
        double score = 0;
        for (String word : lq.split("\\s+")) {
            if (word.length() >= 2 && lt.contains(word)) score += 1.0;
        }
        return score;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
