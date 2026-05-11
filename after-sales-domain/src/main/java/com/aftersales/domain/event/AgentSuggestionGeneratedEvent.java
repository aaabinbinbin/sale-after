package com.aftersales.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AgentSuggestionGeneratedEvent {
    private final String traceId;
    private final String afterSalesNo;
    private final String intent;
    private final String suggestionType;
    private final String suggestionDetail;
    private final double confidence;
    @Builder.Default
    private final LocalDateTime generatedAt = LocalDateTime.now();
}
