package com.aftersales.biz.risk;

import lombok.Getter;

import java.util.List;

@Getter
public class DimensionResult {

    private final String dimensionName;
    private final int score; // 0-100
    private final List<String> reasons;

    public DimensionResult(String dimensionName, int score, List<String> reasons) {
        this.dimensionName = dimensionName;
        this.score = Math.max(0, Math.min(100, score));
        this.reasons = reasons;
    }

    public static DimensionResult safe(String dimensionName) {
        return new DimensionResult(dimensionName, 0, List.of());
    }

    public static DimensionResult of(String name, int score, String reason) {
        return new DimensionResult(name, score, List.of(reason));
    }

    public boolean isRisky() { return score >= 50; }
}
