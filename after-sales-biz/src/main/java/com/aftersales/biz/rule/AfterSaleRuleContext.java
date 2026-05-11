package com.aftersales.biz.rule;

import com.aftersales.common.enums.AfterSalesType;
import com.aftersales.infra.entity.AfterSalesOrder;
import com.aftersales.infra.entity.TradeOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class AfterSaleRuleContext {

    private final TradeOrder order;
    private final AfterSalesOrder afterSalesOrder;
    private final String userId;
    private final AfterSalesType afterSalesType;
    private final BigDecimal applyAmount;
    private final String reasonCode;
    @Builder.Default
    private final LocalDateTime now = LocalDateTime.now();

    @Builder.Default
    private int historicalAfterSalesCount = 0;
    @Builder.Default
    private int historicalRefundCount = 0;
    @Builder.Default
    private String vipLevel = null;
    @Builder.Default
    private int userRiskScore = 0;

    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
    public void setAttribute(String key, Object value) { attributes.put(key, value); }
}
