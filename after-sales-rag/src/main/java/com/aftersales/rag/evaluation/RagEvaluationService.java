package com.aftersales.rag.evaluation;

import com.aftersales.common.util.IdGenerator;
import com.aftersales.infra.mapper.KnowledgeDocMapper;
import com.aftersales.rag.service.RagRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG 评估服务。
 *
 * 实现 Recall@3、Recall@5、MRR 指标。
 */
@Service
public class RagEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(RagEvaluationService.class);

    private final RagRetrievalService retrievalService;
    private final KnowledgeDocMapper knowledgeDocMapper;

    public RagEvaluationService(RagRetrievalService retrievalService, KnowledgeDocMapper knowledgeDocMapper) {
        this.retrievalService = retrievalService;
        this.knowledgeDocMapper = knowledgeDocMapper;
    }

    /**
     * 执行评估。
     *
     * @return 评估结果（Recall@3, Recall@5, MRR）
     */
    public Map<String, Object> evaluate() {
        // 从数据库读取评估集
        var evalDataset = knowledgeDocMapper.selectEvalDataset();

        int hit3 = 0, hit5 = 0, total = 0;
        double mrrSum = 0;

        for (var question : evalDataset) {
            // 解析期望文档编号
            List<String> expectedDocs = Arrays.asList(question.get("expectedDocNos").toString().split(","));

            // 检索
            List<Map<String, Object>> results = retrievalService.search(
                    question.get("question"), 5, Map.of());

            // 获取检索到的文档编号
            List<String> retrievedDocs = results.stream()
                    .map(r -> (String) r.get("docNo"))
                    .toList();

            // Recall@3：前3个结果中是否有期望文档
            boolean hit3Flag = retrievedDocs.stream().limit(3).anyMatch(expectedDocs::contains);
            // Recall@5：前5个结果中是否有期望文档
            boolean hit5Flag = retrievedDocs.stream().limit(5).anyMatch(expectedDocs::contains);
            if (hit3Flag) hit3++;
            if (hit5Flag) hit5++;
            total++;

            // MRR：第一个期望文档的排名倒数
            double rr = 0;
            for (int i = 0; i < retrievedDocs.size(); i++) {
                if (expectedDocs.contains(retrievedDocs.get(i))) {
                    rr = 1.0 / (i + 1);
                    break;
                }
            }
            mrrSum += rr;
        }

        double recall3 = total > 0 ? (double) hit3 / total : 0;
        double recall5 = total > 0 ? (double) hit5 / total : 0;
        double mrr = total > 0 ? mrrSum / total : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalQuestions", total);
        result.put("recallAt3", Math.round(recall3 * 10000.0) / 10000.0);
        result.put("recallAt5", Math.round(recall5 * 10000.0) / 10000.0);
        result.put("mrr", Math.round(mrr * 10000.0) / 10000.0);
        result.put("hitAt3Count", hit3);
        result.put("hitAt5Count", hit5);

        log.info("RAG评估完成 Recall@3={} Recall@5={} MRR={}", recall3, recall5, mrr);
        return result;
    }
}
