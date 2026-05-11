package com.aftersales.agent.memory;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemoryEntry {

    private final String id;
    private final MemoryType type;
    private final String key;
    private final String content;
    @Builder.Default
    private final LocalDateTime createdAt = LocalDateTime.now();
    private final LocalDateTime expiresAt;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
