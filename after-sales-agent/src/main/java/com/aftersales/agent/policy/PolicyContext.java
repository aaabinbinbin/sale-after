package com.aftersales.agent.policy;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
public class PolicyContext {

    private final String userId;
    private final String userRole;
    private final String toolName;
    @Builder.Default
    private final Map<String, Object> toolParams = Collections.emptyMap();
    private final String riskLevel;
    private final BigDecimal amount;

    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
    public void setAttribute(String key, Object value) { attributes.put(key, value); }
}
