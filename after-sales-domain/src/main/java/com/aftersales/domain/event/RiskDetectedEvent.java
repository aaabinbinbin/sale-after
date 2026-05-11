package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RiskDetectedEvent {
    private final String afterSalesNo;
    private final String userId;
    private final int riskScore;
    private final String riskLevel;
    private final List<String> riskReasons;
    private final String action;
    @Builder.Default
    private final LocalDateTime detectedAt = LocalDateTime.now();
}
