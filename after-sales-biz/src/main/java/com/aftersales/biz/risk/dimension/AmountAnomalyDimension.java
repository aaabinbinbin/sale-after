package com.aftersales.biz.risk.dimension;

import com.aftersales.biz.risk.DimensionResult;
import com.aftersales.biz.risk.RiskContext;
import com.aftersales.biz.risk.RiskDimension;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 金额异常维度：检查申请金额是否异常。
 *
 * - 单笔 > 5000：高风险（80分）
 * - 单笔 > 1000：中风险（40分）
 * - 30天累计退款 > 10000：高风险（90分）
 */
@Component
public class AmountAnomalyDimension implements RiskDimension {

    private static final BigDecimal SINGLE_HIGH = new BigDecimal("5000");
    private static final BigDecimal SINGLE_MEDIUM = new BigDecimal("1000");
    private static final int TOTAL_REFUND_HIGH = 10000;

    @Override
    public String getName() { return "金额异常"; }

    @Override
    public double getWeight() { return 0.20; }

    @Override
    public DimensionResult assess(RiskContext context) {
        BigDecimal amount = context.getApplyAmount();
        int score = 0;

        if (amount != null) {
            if (amount.compareTo(SINGLE_HIGH) > 0) {
                score = Math.max(score, 80);
            } else if (amount.compareTo(SINGLE_MEDIUM) > 0) {
                score = Math.max(score, 40);
            }
        }

        if (context.getTotalRefundAmount() > TOTAL_REFUND_HIGH) {
            score = Math.max(score, 90);
        }

        if (score == 0) return DimensionResult.safe(getName());
        return DimensionResult.of(getName(), score,
                score >= 80 ? "金额异常偏高（单笔:" + amount + " 累计:" + context.getTotalRefundAmount() + "）"
                        : "金额略高于正常水平");
    }
}
