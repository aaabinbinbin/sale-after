package com.aftersales.agent.risk;

import com.aftersales.agent.planner.AgentPlan;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.agent.orchestrator.ExecutionMode;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentRiskGuard 单元测试。
 */
class AgentRiskGuardTest {

    private AgentRiskGuard guard;

    @BeforeEach
    void setUp() { guard = new AgentRiskGuard(); }

    @Test
    void shouldAutoExecuteWhenConfidenceHighAndNoConflict() {
        var plan = new AgentPlan();
        var result = guard.assess(plan, 0.9, null, false);
        assertTrue(result.canAutoExecute(), "高置信度 + 无冲突 → Agent 自主执行");
    }

    @Test
    void shouldEscalateWhenConfidenceLow() {
        var plan = new AgentPlan();
        var result = guard.assess(plan, 0.5, null, false);
        assertFalse(result.canAutoExecute(), "低置信度 → 升级人工");
        assertTrue(result.getReason().contains("置信度"));
    }

    @Test
    void shouldEscalateWhenAmountExceeded() {
        var plan = new AgentPlan();
        var result = guard.assess(plan, 0.9, new BigDecimal("600"), false);
        assertFalse(result.canAutoExecute(), "金额超阈值 → 升级人工");
        assertTrue(result.getReason().contains("金额"));
    }

    @Test
    void shouldAutoExecuteWhenAmountWithinThreshold() {
        var plan = new AgentPlan();
        var result = guard.assess(plan, 0.9, new BigDecimal("400"), false);
        assertTrue(result.canAutoExecute(), "金额在阈值内 → Agent 自主执行");
    }

    @Test
    void shouldEscalateWhenConflictExists() {
        var plan = new AgentPlan();
        var result = guard.assess(plan, 0.9, null, true);
        assertFalse(result.canAutoExecute(), "策略冲突 → 升级人工");
        assertTrue(result.getReason().contains("策略冲突"));
    }

    @Test
    void shouldEscalateWhenPlanHasConfirmStep() {
        var plan = new AgentPlan();
        plan.addStep(1, "after_sales.application.create", "CONFIRM_REQUIRED");
        var result = guard.assessQuick(plan, 0.9);
        assertFalse(result.canAutoExecute(), "含 CONFIRM_REQUIRED 步骤 → 升级人工");
    }

    @Test
    void shouldEscalateWhenMultipleReasons() {
        var plan = new AgentPlan();
        plan.addStep(1, "refund.execute", "CONFIRM_REQUIRED");
        var result = guard.assess(plan, 0.5, new BigDecimal("1000"), true);
        assertFalse(result.canAutoExecute());
        // reason 应包含多个原因
        assertTrue(result.getReason().contains(";"));
    }
}
