package com.aftersales.rag.service;

import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.entity.KnowledgeDoc;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 知识构建服务。
 *
 * 售后完成后执行完整流水线：
 * 生成案例文档 → 切片 → Embedding → 写入向量库
 */
@Service
public class KnowledgeBuildService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBuildService.class);

    private final KnowledgeDocService knowledgeDocService;
    private final KnowledgeChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final AfterSalesOrderMapper afterSalesOrderMapper;

    public KnowledgeBuildService(KnowledgeDocService knowledgeDocService,
                                  KnowledgeChunkService chunkService,
                                  EmbeddingService embeddingService,
                                  VectorStoreService vectorStoreService,
                                  AfterSalesOrderMapper afterSalesOrderMapper) {
        this.knowledgeDocService = knowledgeDocService;
        this.chunkService = chunkService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.afterSalesOrderMapper = afterSalesOrderMapper;
    }

    /**
     * 从售后单构建知识文档（完整流水线：文档 → 切片 → Embedding → 向量库）。
     */
    public KnowledgeDoc buildFromAfterSales(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) {
            log.warn("售后单不存在，跳过知识构建 afterSalesNo={}", afterSalesNo);
            return null;
        }

        // Step 1: 生成案例内容
        String content = generateCaseContent(asOrder);
        KnowledgeDoc doc = knowledgeDocService.create(
                "CASE",
                asOrder.getAfterSalesType() + "售后案例-" + afterSalesNo,
                "AFTER_SALES_CASE",
                afterSalesNo,
                content);
        log.info("知识文档已生成 docNo={}", doc.getDocNo());

        // Step 2: 切片
        List<Map<String, Object>> chunks = chunkService.chunkDocument(doc);
        log.info("知识切片完成 docNo={} chunkCount={}", doc.getDocNo(), chunks.size());

        // Step 3: Embedding + 写入向量库
        for (Map<String, Object> chunk : chunks) {
            try {
                String chunkContent = (String) chunk.get("content");
                int chunkIndex = (int) chunk.get("chunkIndex");
                String chunkNo = doc.getDocNo() + "_CHUNK_" + chunkIndex;

                float[] vector = embeddingService.embed(chunkContent);

                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("docNo", doc.getDocNo());
                metadata.put("docType", "CASE");
                metadata.put("afterSalesType", asOrder.getAfterSalesType());
                metadata.put("chunkIndex", chunkIndex);
                metadata.put("sourceId", afterSalesNo);

                vectorStoreService.store(chunkNo, vector, metadata);
            } catch (Exception e) {
                log.warn("向量存储失败 chunk={}: {}", chunk.get("chunkIndex"), e.getMessage());
            }
        }

        log.info("知识构建完整流水线完成 afterSalesNo={} docNo={} chunks={}",
                afterSalesNo, doc.getDocNo(), chunks.size());
        return doc;
    }

    /** 生成案例文档内容 */
    private String generateCaseContent(AfterSalesOrder order) {
        return String.format("""
                标题：%s 售后案例
                背景：用户%s于订单%s发起售后申请
                售后类型：%s
                原因：%s
                申请金额：%s
                审核金额：%s
                最终状态：%s
                经验总结：根据售后政策处理""",
                order.getAfterSalesType(),
                order.getUserId(),
                order.getOrderNo(),
                order.getAfterSalesType(),
                order.getReasonText() != null ? order.getReasonText() : "未提供",
                order.getApplyAmount(),
                order.getApprovedAmount(),
                order.getStatus()
        );
    }
}
