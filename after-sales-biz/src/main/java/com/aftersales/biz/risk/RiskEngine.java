package com.aftersales.biz.risk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 多维风控引擎。
 *
 * 聚合所有 RiskDimension，加权评分后输出 riskScore / riskLevel。
 * 与 Agent 联动：高风险 → 人工审核，低风险 → 自动通过。
 */
@Component
public class RiskEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskEngine.class);

    private final List<RiskDimension> dimensions;

    public RiskEngine(List<RiskDimension> dimensions) {
        this.dimensions = dimensions;
        log.info("RiskEngine 加载 {} 个风控维度: {}", this.dimensions.size(),
                this.dimensions.stream().map(RiskDimension::getName).toList());
    }

    /**
     * 评估风险。
     *
     * @param context 风控上下文
     * @return 汇总风险结果
     */
    public RiskResult assess(RiskContext context) {
        if (dimensions.isEmpty()) {
            return RiskResult.low();
        }

        double totalWeight = 0;
        double weightedScore = 0;
        List<DimensionResult> dimResults = new ArrayList<>();
        List<String> allReasons = new ArrayList<>();

        for (RiskDimension dim : dimensions) {
            DimensionResult result = dim.assess(context);
            dimResults.add(result);

            double weight = dim.getWeight();
            totalWeight += weight;
            weightedScore += result.getScore() * weight;

            if (result.isRisky()) {
                allReasons.addAll(result.getReasons());
            }
        }

        // 归一化（确保权重和为 1）
        int finalScore = totalWeight > 0
                ? (int) Math.round(weightedScore / totalWeight)
                : 0;

        String riskLevel;
        if (finalScore >= 80) {
            riskLevel = "HIGH";
        } else if (finalScore >= 50) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        if (allReasons.isEmpty()) {
            allReasons.add("无异常");
        }

        log.info("RiskEngine 评估完成: userId={}, score={}, level={}, reasons={}",
                context.getUserId(), finalScore, riskLevel, allReasons);

        return new RiskResult(finalScore, riskLevel, allReasons, dimResults);
    }

    /** 获取所有维度（用于动态展示和配置） */
    public List<RiskDimension> getDimensions() { return dimensions; }
}
