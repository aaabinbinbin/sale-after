package com.aftersales.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量存储服务。
 *
 * local/dev：使用内存 Map 模拟向量存储（可检索，可验证流程）。
 * 生产环境：使用 Spring AI PgVectorStore。
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    // 内存向量存储（key=vectorId, value={vector, metadata}）
    private final Map<String, StoredVector> store = new ConcurrentHashMap<>();

    @Value("${after-sales.rag.mysql-fallback-enabled:true}")
    private boolean devMode;

    /**
     * 存储向量。
     *
     * @param vectorId 向量ID（通常与 chunkNo 对应）
     * @param vector   向量数据
     * @param metadata 元数据（docNo, docType, content 摘要等）
     */
    public void store(String vectorId, float[] vector, Map<String, Object> metadata) {
        StoredVector sv = new StoredVector();
        sv.vectorId = vectorId;
        sv.vector = vector;
        sv.metadata = metadata;
        store.put(vectorId, sv);
        if (devMode) {
            log.info("向量存储(内存/dev) vectorId={} dimension={}", vectorId, vector.length);
        }
    }

    /**
     * 批量存储。
     */
    public void storeBatch(List<String> vectorIds, List<float[]> vectors, List<Map<String, Object>> metadataList) {
        for (int i = 0; i < vectorIds.size(); i++) {
            store(vectorIds.get(i), vectors.get(i), metadataList.get(i));
        }
        log.info("批量向量存储完成 count={}", vectorIds.size());
    }

    /**
     * 向量相似度检索（余弦相似度）。
     *
     * @param queryVector 查询向量
     * @param topK        返回数量
     * @param filters     元数据过滤条件
     * @return 最相似的 topK 个结果
     */
    public List<Map<String, Object>> search(float[] queryVector, int topK, Map<String, Object> filters) {
        // 计算所有向量的余弦相似度
        List<ScoredResult> scored = new ArrayList<>();
        for (StoredVector sv : store.values()) {
            // 元数据过滤
            if (filters != null && !filters.isEmpty()) {
                String docTypeFilter = (String) filters.get("docType");
                if (docTypeFilter != null && sv.metadata != null
                        && !docTypeFilter.equals(sv.metadata.get("docType"))) {
                    continue;
                }
            }
            double similarity = cosineSimilarity(queryVector, sv.vector);
            scored.add(new ScoredResult(sv, similarity));
        }

        // 按相似度降序
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 构建结果
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            ScoredResult sr = scored.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("vectorId", sr.vector.vectorId);
            item.put("score", Math.round(sr.score * 10000.0) / 10000.0);
            if (sr.vector.metadata != null) {
                item.putAll(sr.vector.metadata);
            }
            results.add(item);
        }
        return results;
    }

    /** 余弦相似度 */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** 存储的向量 */
    private static class StoredVector {
        String vectorId;
        float[] vector;
        Map<String, Object> metadata;
    }

    /** 评分结果 */
    private static class ScoredResult {
        StoredVector vector;
        double score;
        ScoredResult(StoredVector v, double s) { this.vector = v; this.score = s; }
    }
}
