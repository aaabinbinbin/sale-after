package com.aftersales.domain.eligibility;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 售后资格校验服务。
 *
 * 按责任链顺序执行所有 Checker，任一失败即停止并返回失败结果。
 * 该服务同时被业务层和 Agent 资格判断 Skill 复用。
 */
@Service
public class AfterSalesEligibilityService {

    private final List<EligibilityChecker> checkers;

    /**
     * 初始化责任链。
     *
     * 校验顺序：订单存在 -> 订单归属 -> 订单状态 -> 订单项归属 -> 数量校验 -> 金额校验 -> 窗口期 -> 重复售后
     */
    public AfterSalesEligibilityService() {
        this.checkers = new ArrayList<>();
        this.checkers.add(new OrderExistsChecker());
        this.checkers.add(new OrderOwnerChecker());
        this.checkers.add(new OrderStatusChecker());
        this.checkers.add(new OrderItemChecker());
        this.checkers.add(new QuantityChecker());
        this.checkers.add(new AmountChecker());
        this.checkers.add(new AfterSalesWindowChecker());
        this.checkers.add(new DuplicateAfterSalesChecker());
    }

    /**
     * 执行售后资格校验。
     *
     * @param context 校验上下文（包含订单、订单项、申请明细等）
     * @return 校验结果，通过返回 pass()，失败返回第一条失败原因
     */
    public EligibilityCheckResult check(EligibilityContext context) {
        for (EligibilityChecker checker : checkers) {
            EligibilityCheckResult result = checker.check(context);
            if (!result.isPassed()) {
                return result; // 快速失败，返回第一条不通过的校验结果
            }
        }
        return EligibilityCheckResult.pass();
    }
}
