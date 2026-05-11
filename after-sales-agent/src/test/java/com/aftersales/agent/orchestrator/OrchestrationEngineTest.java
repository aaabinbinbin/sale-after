package com.aftersales.agent.orchestrator;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.loader.SkillLoader;
import com.aftersales.agent.planner.AgentPlan;
import com.aftersales.agent.skill.SkillResult;
import com.aftersales.agent.trace.AgentTraceService;
import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrchestrationEngine 单元测试。
 */
class OrchestrationEngineTest {

    private OrchestrationEngine engine;
    private TestSkillLoader skillLoader;
    private AgentTraceService traceService;

    @BeforeEach
    void setUp() {
        skillLoader = new TestSkillLoader();
        // 注入测试 Skill：order-query（LOW 风险）
        SkillDefinition orderQuery = new SkillDefinition();
        orderQuery.setName("order.query");
        orderQuery.setRiskLevel("LOW");
        orderQuery.setRequiredTools(List.of("orderQueryTool"));
        skillLoader.addSkill(orderQuery);

        // 注入测试 Skill：after_sales.application.create（HIGH 风险）
        SkillDefinition createApp = new SkillDefinition();
        createApp.setName("after_sales.application.create");
        createApp.setRiskLevel("HIGH");
        createApp.setRequiredTools(List.of());
        skillLoader.addSkill(createApp);

        traceService = new AgentTraceService(null, null, null);
        engine = new OrchestrationEngine(skillLoader, new ToolRegistry(null, null, null, null), traceService);
    }

    @Test
    void shouldRejectUnknownSkill() {
        var plan = new AgentPlan();
        plan.addStep(1, "nonexistent.skill", "SEQUENTIAL");
        var ctx = new AgentContext();
        ctx.setTraceId("test-trace");

        var result = engine.execute(plan, ctx);
        assertFalse(result.success(), "不存在的 Skill 应导致执行失败");
    }

    @Test
    void shouldRejectConfirmRequiredOnLowRiskSkill() {
        // order.query 是 LOW 风险，不能标记 CONFIRM_REQUIRED
        var plan = new AgentPlan();
        plan.addStep(1, "order.query", "CONFIRM_REQUIRED"); // 不允许
        var ctx = new AgentContext();
        ctx.setTraceId("test-trace");

        var result = engine.execute(plan, ctx);
        assertFalse(result.stepResults().get(0).success(), "LOW 风险 Skill 不能使用 CONFIRM_REQUIRED");
    }

    @Test
    void shouldAllowConfirmRequiredOnHighRiskSkill() {
        var plan = new AgentPlan();
        plan.addStep(1, "after_sales.application.create", "CONFIRM_REQUIRED");
        var ctx = new AgentContext();
        ctx.setTraceId("test-trace");

        var result = engine.execute(plan, ctx);
        assertTrue(result.hasConfirmRequired(), "HIGH 风险 Skill 可以使用 CONFIRM_REQUIRED");
    }

    @Test
    void shouldExecuteSequentialSteps() {
        var plan = new AgentPlan();
        plan.addStep(1, "order.query", "SEQUENTIAL");
        var ctx = new AgentContext();
        ctx.setTraceId("test-trace");

        // ToolRegistry 没有 orderQueryTool，应返回失败
        var result = engine.execute(plan, ctx);
        // 预期失败因为 tool 不存在，但不抛异常
        assertNotNull(result);
    }

    // ====== Test SkillLoader ======
    static class TestSkillLoader extends SkillLoader {
        private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();
        void addSkill(SkillDefinition def) { skills.put(def.getName(), def); }
        @Override public SkillDefinition get(String name) { return skills.get(name); }
    }
}
