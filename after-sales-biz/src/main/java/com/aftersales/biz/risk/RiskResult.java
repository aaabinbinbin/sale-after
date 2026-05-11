package com.aftersales.biz.risk;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class RiskResult {

    private final int riskScore;
    private final String riskLevel;
    private final List<String> riskReasons;
    private final List<DimensionResult> dimensionResults;

    public RiskResult(int riskScore, String riskLevel,
                      List<String> riskReasons, List<DimensionResult> dimensionResults) {
        this.riskScore = Math.max(0, Math.min(100, riskScore));
        this.riskLevel = riskLevel;
        this.riskReasons = Collections.unmodifiableList(riskReasons);
        this.dimensionResults = Collections.unmodifiableList(dimensionResults);
    }

    public static RiskResult low() {
        return new RiskResult(0, "LOW", List.of("无风险"), List.of());
    }

    public boolean isHighRisk() { return "HIGH".equals(riskLevel); }
    public boolean needManualReview() { return riskScore >= 50; }
}
