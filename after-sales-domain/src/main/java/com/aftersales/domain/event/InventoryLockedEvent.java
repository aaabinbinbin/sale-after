package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class InventoryLockedEvent {
    private final String afterSalesNo;
    private final Long skuId;
    private final int lockQuantity;
    private final int availableAfterLock;
    @Builder.Default
    private final LocalDateTime lockedAt = LocalDateTime.now();
}
