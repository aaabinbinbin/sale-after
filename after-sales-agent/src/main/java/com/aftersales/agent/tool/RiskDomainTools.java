package com.aftersales.agent.tool;

import com.aftersales.biz.risk.RiskContext;
import com.aftersales.biz.risk.RiskEngine;
import com.aftersales.biz.risk.RiskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 风控领域工具聚合。
 */
@Component
public class RiskDomainTools {

    private static final Logger log = LoggerFactory.getLogger(RiskDomainTools.class);

    private final RiskEngine riskEngine;

    public RiskDomainTools(RiskEngine riskEngine) {
        this.riskEngine = riskEngine;
    }

    /**
     * 对售后请求进行快速风险评估。
     */
    public Map<String, Object> assessAfterSalesRisk(String userId, String afterSalesNo,
                                                     String orderNo, BigDecimal amount) {
        RiskContext ctx = RiskContext.builder()
                .userId(userId).afterSalesNo(afterSalesNo)
                .orderNo(orderNo).applyAmount(amount).build();
        RiskResult result = riskEngine.assess(ctx);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("riskScore", result.getRiskScore());
        summary.put("riskLevel", result.getRiskLevel());
        summary.put("riskReasons", result.getRiskReasons());
        summary.put("needManualReview", result.needManualReview());
        summary.put("isHighRisk", result.isHighRisk());
        log.info("RiskDomainTools: 风险评估完成 userId={} score={} level={}",
                userId, result.getRiskScore(), result.getRiskLevel());
        return summary;
    }
}
