package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AfterSalesApprovedEvent {
    private final String afterSalesNo;
    private final Long afterSalesId;
    private final String afterSalesType;
    private final String reviewerId;
    private final BigDecimal approvedAmount;
    private final String nextStatus;
    @Builder.Default
    private final LocalDateTime approvedAt = LocalDateTime.now();
}
