package com.aftersales.biz.risk;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class RiskContext {

    private final String userId;
    private final String afterSalesNo;
    private final String orderNo;
    private final BigDecimal applyAmount;

    @Builder.Default
    private int recentAfterSalesCount = 0;
    @Builder.Default
    private int recentRefundSuccessCount = 0;
    @Builder.Default
    private int totalRefundAmount = 0;

    @Builder.Default
    private String deviceFingerprint = null;
    @Builder.Default
    private String ipAddress = null;
    @Builder.Default
    private String shippingAddress = null;
    @Builder.Default
    private boolean addressChanged = false;
    @Builder.Default
    private boolean newDevice = false;

    @Builder.Default
    private Long productCategoryId = null;
    @Builder.Default
    private BigDecimal itemUnitPrice = null;
    @Builder.Default
    private boolean isHighValueItem = false;

    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
    public void setAttribute(String key, Object value) { attributes.put(key, value); }
}
