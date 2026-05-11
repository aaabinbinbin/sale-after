package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AfterSalesRejectedEvent {
    private final String afterSalesNo;
    private final Long afterSalesId;
    private final String reviewerId;
    private final String rejectReason;
    @Builder.Default
    private final LocalDateTime rejectedAt = LocalDateTime.now();
}
