package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class InventoryReleasedEvent {
    private final String afterSalesNo;
    private final Long skuId;
    private final int releaseQuantity;
    private final String releaseReason;
    @Builder.Default
    private final LocalDateTime releasedAt = LocalDateTime.now();
}
