package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RefundCreatedEvent {
    private final String afterSalesNo;
    private final Long afterSalesId;
    private final String refundNo;
    private final BigDecimal refundAmount;
    private final String refundChannel;
    @Builder.Default
    private final LocalDateTime createdAt = LocalDateTime.now();
}
