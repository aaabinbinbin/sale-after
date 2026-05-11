package com.aftersales.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Embedding 服务。
 *
 * 优先使用 Spring AI EmbeddingModel 调百炼 qwen3-vl-rerank。
 * API 不可用时降级为模拟向量，日志标注 [DEV]。
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int MOCK_DIMENSION = 128;

    private final EmbeddingModel embeddingModel;
    private volatile boolean realApiAvailable = true;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 生成文本向量。API 可用时调真实接口，不可用时降级模拟。
     */
    public float[] embed(String text) {
        if (realApiAvailable && embeddingModel != null) {
            try {
                EmbeddingResponse response = embeddingModel.call(
                        new EmbeddingRequest(List.of(text), null));
                if (response != null && !response.getResults().isEmpty()) {
                    Object output = response.getResults().get(0).getOutput();
                    if (output instanceof float[] fa) return fa;
                    if (output instanceof List<?> list) {
                        float[] vec = new float[list.size()];
                        for (int i = 0; i < vec.length; i++)
                            vec[i] = ((Number) list.get(i)).floatValue();
                        return vec;
                    }
                }
            } catch (Exception e) {
                log.warn("[DEV] Embedding API 调用失败，降级为模拟向量: {}", e.getMessage());
                realApiAvailable = false;
            }
        }
        log.info("[DEV] RAG 使用模拟向量，仅用于 local/dev，不应作为生产主检索路径");
        return mockEmbed(text);
    }

    /** 批量生成向量 */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    /** 模拟向量：基于文本 hash 的确定性伪随机向量，归一化 */
    private float[] mockEmbed(String text) {
        float[] vec = new float[MOCK_DIMENSION];
        int hash = text.hashCode();
        Random rnd = new Random(hash);
        float norm = 0;
        for (int i = 0; i < MOCK_DIMENSION; i++) {
            vec[i] = (rnd.nextFloat() - 0.5f) * 2;
            norm += vec[i] * vec[i];
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < MOCK_DIMENSION; i++) vec[i] /= norm;
        return vec;
    }
}
