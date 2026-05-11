package com.aftersales.biz.rule.impl;

import com.aftersales.biz.rule.AfterSaleRule;
import com.aftersales.biz.rule.AfterSaleRuleContext;
import com.aftersales.biz.rule.RuleResult;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 类目规则：检查特殊类目是否允许售后。
 *
 * 某些商品类目（如虚拟商品、生鲜、定制商品等）不允许退货退款。
 */
@Component
public class CategoryRule implements AfterSaleRule {

    /** 禁止售后的类目 ID 集合（应从数据库或配置中心加载） */
    private static final Set<Long> BLOCKED_CATEGORY_IDS = Set.of(
            9999L // 虚拟商品示例
    );

    /** 仅支持退款的类目 */
    private static final Set<Long> REFUND_ONLY_CATEGORY_IDS = Set.of(
            8888L // 生鲜类目示例
    );

    @Override
    public String getName() { return "CategoryRule"; }

    @Override
    public int getPriority() { return 15; }

    @Override
    public RuleResult evaluate(AfterSaleRuleContext context) {
        Long categoryId = context.getAttribute("categoryId");
        if (categoryId == null) {
            return RuleResult.pass(getName(), "无类目信息，跳过类目检查");
        }

        if (BLOCKED_CATEGORY_IDS.contains(categoryId)) {
            return RuleResult.block(getName(),
                    "该类目不支持售后（类目ID: " + categoryId + "）");
        }

        if (REFUND_ONLY_CATEGORY_IDS.contains(categoryId)) {
            String type = context.getAfterSalesType().name();
            if (!"REFUND_ONLY".equals(type) && !"COMPENSATION".equals(type)) {
                return RuleResult.warn(getName(),
                        "该类目仅支持退款/补偿，不支持换货或退货退款（类目ID: " + categoryId + "）");
            }
        }

        return RuleResult.pass(getName(), "类目检查通过");
    }
}
