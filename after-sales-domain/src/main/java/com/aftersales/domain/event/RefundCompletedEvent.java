package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RefundCompletedEvent {
    private final String afterSalesNo;
    private final Long afterSalesId;
    private final String refundNo;
    private final String externalRefundNo;
    private final BigDecimal refundAmount;
    @Builder.Default
    private final LocalDateTime completedAt = LocalDateTime.now();
}
