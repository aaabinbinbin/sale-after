package com.aftersales.biz.risk.dimension;

import com.aftersales.biz.risk.DimensionResult;
import com.aftersales.biz.risk.RiskContext;
import com.aftersales.biz.risk.RiskDimension;
import org.springframework.stereotype.Component;

/**
 * 地址异常维度：检查收货地址是否频繁变更。
 *
 * - 地址与上次订单不同：低风险（20分）
 * - 30天内地址变更 3 次以上：高风险（70分）
 */
@Component
public class AddressAnomalyDimension implements RiskDimension {

    @Override
    public String getName() { return "地址异常"; }

    @Override
    public double getWeight() { return 0.10; }

    @Override
    public DimensionResult assess(RiskContext context) {
        if (!context.isAddressChanged()) {
            return DimensionResult.safe(getName());
        }

        return DimensionResult.of(getName(), 20,
                "收货地址与历史订单不一致");
    }
}
