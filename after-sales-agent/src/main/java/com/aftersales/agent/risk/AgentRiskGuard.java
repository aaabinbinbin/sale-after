package com.aftersales.agent.risk;

import com.aftersales.agent.planner.AgentPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Agent 风险守卫 + 人工升级判定。
 *
 * 综合置信度、金额、策略冲突三维度判断 Agent 能否自主执行。
 *
 * Agent 自主执行条件（全部满足）：
 *   1. 置信度 ≥ 0.7
 *   2. 金额在阈值内（单笔 ≤ 500 元）
 *   3. 无策略冲突
 *   4. 计划不含 COMFIRM_REQUIRED 步骤
 *
 * 升级人工条件（任一触发）：置信度低、金额超限、策略冲突、主动要求、需确认步骤。
 */
@Component
public class AgentRiskGuard {

    private static final Logger log = LoggerFactory.getLogger(AgentRiskGuard.class);
    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("500");

    /**
     * 评估当前计划是否需要人工介入。
     */
    public AssessmentResult assess(AgentPlan plan, double confidence, BigDecimal amount, boolean hasConflict) {
        List<String> reasons = new ArrayList<>();

        if (confidence < 0.7) reasons.add("置信度不足 (" + String.format("%.2f", confidence) + " < 0.7)");
        if (amount != null && amount.compareTo(AMOUNT_THRESHOLD) > 0)
            reasons.add("金额超阈值 (" + amount + " > " + AMOUNT_THRESHOLD + ")");
        if (hasConflict) reasons.add("策略冲突");
        if (plan != null && plan.hasConfirmStep()) reasons.add("含需确认步骤");

        if (reasons.isEmpty()) {
            log.info("Agent 自主执行");
            return AssessmentResult.autoExecute();
        }
        log.info("升级人工: {}", reasons);
        return AssessmentResult.escalateToHuman(String.join("; ", reasons));
    }

    /** 简化评估 */
    public AssessmentResult assessQuick(AgentPlan plan, double confidence) {
        return assess(plan, confidence, null, false);
    }

    public static class AssessmentResult {
        private final boolean autoExecute;
        private final String reason;

        private AssessmentResult(boolean autoExecute, String reason) {
            this.autoExecute = autoExecute; this.reason = reason;
        }
        public static AssessmentResult autoExecute() { return new AssessmentResult(true, null); }
        public static AssessmentResult escalateToHuman(String reason) { return new AssessmentResult(false, reason); }
        public boolean canAutoExecute() { return autoExecute; }
        public String getReason() { return reason; }
    }
}
