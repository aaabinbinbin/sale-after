package com.aftersales.agent.tool;

import com.aftersales.biz.service.AfterSalesApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 售后领域工具聚合。
 *
 * 提供统一的售后进度查询接口。
 */
@Component
public class AfterSaleDomainTools {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleDomainTools.class);

    private final AfterSalesApplicationService afterSalesService;

    public AfterSaleDomainTools(AfterSalesApplicationService afterSalesService) {
        this.afterSalesService = afterSalesService;
    }

    /**
     * 获取售后进度摘要。
     *
     * @param afterSalesNo 售后单号
     * @return 聚合结果，包含状态、审核信息、退款/换货/补偿进度
     */
    public Map<String, Object> getProgress(String afterSalesNo) {
        Map<String, Object> progress = new LinkedHashMap<>();
        try {
            Map<String, Object> detail = afterSalesService.getDetail(afterSalesNo);
            progress.put("afterSalesNo", afterSalesNo);
            progress.put("status", detail.get("status"));
            progress.put("statusDescription", detail.get("statusDescription"));
            progress.put("type", detail.get("afterSalesType"));
            progress.put("applyAmount", detail.get("applyAmount"));
            progress.put("approvedAmount", detail.get("approvedAmount"));
            progress.put("reviewRemark", detail.get("reviewRemark"));
            progress.put("items", detail.get("items"));
            progress.put("refund", detail.get("refund"));
            progress.put("exchange", detail.get("exchange"));
            progress.put("returnInfo", detail.get("returnInfo"));
            progress.put("compensation", detail.get("compensation"));
            progress.put("operationLogs", detail.get("operationLogs"));
            log.info("AfterSaleDomainTools: 售后进度构建完成 afterSalesNo={}", afterSalesNo);
        } catch (Exception e) {
            log.error("售后进度构建失败 afterSalesNo={}: {}", afterSalesNo, e.getMessage());
            progress.put("error", e.getMessage());
        }
        return progress;
    }
}
