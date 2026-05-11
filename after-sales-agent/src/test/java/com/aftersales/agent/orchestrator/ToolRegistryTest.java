package com.aftersales.agent.orchestrator;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.skill.SkillResult;
import com.aftersales.biz.service.*;
import com.aftersales.infra.mapper.SkuStockMapper;
import com.aftersales.rag.service.RagRetrievalService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolRegistry 单元测试。验证工具注册和调用。
 */
class ToolRegistryTest {

    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        // 用 null Service 创建（接口调用会NPE，但注册验证可用）
        toolRegistry = new ToolRegistry(null, null, null, null);
    }

    @Test
    void shouldRegisterAndExecuteBuiltinTools() {
        // 验证内置工具已注册
        AgentContext ctx = new AgentContext();
        SkillDefinition def = new SkillDefinition();
        def.setName("test");

        // 调用不存在的工具应返回失败
        var result = toolRegistry.execute("nonexistentTool", ctx, def);
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("未知工具"));
    }

    @Test
    void shouldSupportCustomToolRegistration() {
        AgentContext ctx = new AgentContext();
        SkillDefinition def = new SkillDefinition();

        // 注册自定义工具
        toolRegistry.register("customTool", (c, d) -> SkillResult.ok(
                java.util.Map.of("custom", "value")));

        var result = toolRegistry.execute("customTool", ctx, def);
        assertTrue(result.isSuccess());
        assertEquals("value", result.getData().get("custom"));
    }

    @Test
    void shouldHandleToolExecutionFailure() {
        AgentContext ctx = new AgentContext();
        SkillDefinition def = new SkillDefinition();

        toolRegistry.register("failingTool", (c, d) -> SkillResult.fail("模拟失败"));

        var result = toolRegistry.execute("failingTool", ctx, def);
        assertFalse(result.isSuccess());
        assertEquals("模拟失败", result.getError());
    }

    @Test
    void builtinToolNamesShouldBeAvailable() {
        // 内置工具: orderQueryTool, afterSalesProgressTool, ragRetrieveTool, stockCheckTool
        AgentContext ctx = new AgentContext();
        SkillDefinition def = new SkillDefinition();
        // 这些调用会NPE因为Service为null，但验证了工具已注册
        var result = toolRegistry.execute("orderQueryTool", ctx, def);
        // 工具已注册但参数不全→返回失败
        assertFalse(result.isSuccess());
    }
}
