package com.aftersales.rag.service;

import com.aftersales.common.util.IdGenerator;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.entity.KnowledgeDoc;
import com.aftersales.infra.mapper.AfterSalesOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 知识构建服务。
 *
 * 售后完成后生成案例知识文档。
 */
@Service
public class KnowledgeBuildService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBuildService.class);

    private final KnowledgeDocService knowledgeDocService;
    private final AfterSalesOrderMapper afterSalesOrderMapper;

    public KnowledgeBuildService(KnowledgeDocService knowledgeDocService,
                                  AfterSalesOrderMapper afterSalesOrderMapper) {
        this.knowledgeDocService = knowledgeDocService;
        this.afterSalesOrderMapper = afterSalesOrderMapper;
    }

    /**
     * 从售后单构建知识文档。
     *
     * @param afterSalesNo 售后单号
     */
    public KnowledgeDoc buildFromAfterSales(String afterSalesNo) {
        AfterSalesOrder asOrder = afterSalesOrderMapper.selectByAfterSalesNo(afterSalesNo);
        if (asOrder == null) {
            log.warn("售后单不存在，跳过知识构建 afterSalesNo={}", afterSalesNo);
            return null;
        }

        // 生成案例内容
        String content = String.format("""
                标题：%s 售后案例
                售后类型：%s
                原因：%s
                申请金额：%s
                审核金额：%s
                最终状态：%s
                """,
                asOrder.getAfterSalesType(),
                asOrder.getAfterSalesType(),
                asOrder.getReasonText(),
                asOrder.getApplyAmount(),
                asOrder.getApprovedAmount(),
                asOrder.getStatus()
        );

        KnowledgeDoc doc = knowledgeDocService.create(
                "CASE",
                asOrder.getAfterSalesType() + "售后案例-" + afterSalesNo,
                "AFTER_SALES_CASE",
                afterSalesNo,
                content
        );

        log.info("知识构建完成 afterSalesNo={} docNo={}", afterSalesNo, doc.getDocNo());
        return doc;
    }
}
