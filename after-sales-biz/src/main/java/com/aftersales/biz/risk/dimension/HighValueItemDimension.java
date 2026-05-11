package com.aftersales.biz.risk.dimension;

import com.aftersales.biz.risk.DimensionResult;
import com.aftersales.biz.risk.RiskContext;
import com.aftersales.biz.risk.RiskDimension;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 高价值商品维度：高价值商品的售后风险更高。
 *
 * - 单价 > 3000：高风险（70分）
 * - 单价 > 1000：中风险（35分）
 */
@Component
public class HighValueItemDimension implements RiskDimension {

    @Override
    public String getName() { return "高价值商品"; }

    @Override
    public double getWeight() { return 0.15; }

    @Override
    public DimensionResult assess(RiskContext context) {
        if (!context.isHighValueItem()) {
            return DimensionResult.safe(getName());
        }

        BigDecimal price = context.getItemUnitPrice();
        if (price != null && price.compareTo(new BigDecimal("3000")) > 0) {
            return DimensionResult.of(getName(), 70,
                    "高价值商品售后（单价:" + price + "），需重点审核");
        }

        return DimensionResult.of(getName(), 35,
                "中高价值商品售后，建议核实");
    }
}
