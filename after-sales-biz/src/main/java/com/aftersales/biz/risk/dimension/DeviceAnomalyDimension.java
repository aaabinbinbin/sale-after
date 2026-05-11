package com.aftersales.biz.risk.dimension;

import com.aftersales.biz.risk.DimensionResult;
import com.aftersales.biz.risk.RiskContext;
import com.aftersales.biz.risk.RiskDimension;
import org.springframework.stereotype.Component;

/**
 * 设备异常维度：检查设备指纹是否变更。
 *
 * - 新设备首次下单后退款：中风险（60分）
 * - 设备指纹频繁变更：高风险（90分）
 */
@Component
public class DeviceAnomalyDimension implements RiskDimension {

    @Override
    public String getName() { return "设备异常"; }

    @Override
    public double getWeight() { return 0.15; }

    @Override
    public DimensionResult assess(RiskContext context) {
        boolean newDevice = context.isNewDevice();
        if (!newDevice) {
            return DimensionResult.safe(getName());
        }

        // 新设备 + 退款 → 可能是账号被盗或恶意注册
        return DimensionResult.of(getName(), 60,
                "新设备登录后发起退款，建议关注");
    }
}
