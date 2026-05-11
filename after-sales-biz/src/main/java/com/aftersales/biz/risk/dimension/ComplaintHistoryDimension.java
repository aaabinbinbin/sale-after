package com.aftersales.biz.risk.dimension;

import com.aftersales.biz.risk.DimensionResult;
import com.aftersales.biz.risk.RiskContext;
import com.aftersales.biz.risk.RiskDimension;
import org.springframework.stereotype.Component;

/**
 * 历史投诉维度：检查用户是否有投诉/纠纷记录。
 *
 * 当前为占位实现，后续可接入客服系统数据。
 */
@Component
public class ComplaintHistoryDimension implements RiskDimension {

    @Override
    public String getName() { return "历史投诉"; }

    @Override
    public double getWeight() { return 0.15; }

    @Override
    public DimensionResult assess(RiskContext context) {
        // 占位：后续接入客服系统获取投诉数据
        return DimensionResult.safe(getName());
    }
}
