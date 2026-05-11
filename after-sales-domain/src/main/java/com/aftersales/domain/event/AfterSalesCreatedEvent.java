package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AfterSalesCreatedEvent {
    private final String afterSalesNo;
    private final Long afterSalesId;
    private final String afterSalesType;
    private final String userId;
    private final String orderNo;
    @Builder.Default
    private final LocalDateTime createdAt = LocalDateTime.now();
}
