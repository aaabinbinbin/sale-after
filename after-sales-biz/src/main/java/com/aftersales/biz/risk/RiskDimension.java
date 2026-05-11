package com.aftersales.biz.risk;

/**
 * 风控维度接口。
 *
 * 每个维度独立评估，返回 0-100 的风险分值和风险原因。
 * 所有维度加权汇总后得到用户风险评分。
 */
public interface RiskDimension {

    /** 维度名称 */
    String getName();

    /** 权重（所有维度权重之和为 1.0） */
    double getWeight();

    /** 评估风险，返回 0-100 的分值 */
    DimensionResult assess(RiskContext context);
}
