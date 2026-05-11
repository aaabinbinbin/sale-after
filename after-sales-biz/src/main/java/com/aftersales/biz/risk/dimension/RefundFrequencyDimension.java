package com.aftersales.biz.risk.dimension;

import com.aftersales.biz.risk.DimensionResult;
import com.aftersales.biz.risk.RiskContext;
import com.aftersales.biz.risk.RiskDimension;
import org.springframework.stereotype.Component;

/**
 * 退款频率维度：分析 30 天内退款频率。
 *
 * - 0-1 次：正常（0分）
 * - 2-3 次：轻微异常（30分）
 * - 4-5 次：中度异常（60分）
 * - 5 次以上：高度异常（90分）
 */
@Component
public class RefundFrequencyDimension implements RiskDimension {

    @Override
    public String getName() { return "退款频率"; }

    @Override
    public double getWeight() { return 0.25; }

    @Override
    public DimensionResult assess(RiskContext context) {
        int count = context.getRecentAfterSalesCount();

        if (count <= 1) {
            return DimensionResult.safe(getName());
        }
        if (count <= 3) {
            return DimensionResult.of(getName(), 30,
                    "30天内售后 " + count + " 次，略高于正常水平");
        }
        if (count <= 5) {
            return DimensionResult.of(getName(), 60,
                    "30天内售后 " + count + " 次，频率偏高");
        }
        return DimensionResult.of(getName(), 90,
                "30天内售后 " + count + " 次，高度异常");
    }
}
