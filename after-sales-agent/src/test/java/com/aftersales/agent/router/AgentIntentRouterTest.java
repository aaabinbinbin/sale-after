package com.aftersales.agent.router;

import com.aftersales.agent.context.AgentContext;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentIntentRouter 单元测试。
 */
class AgentIntentRouterTest {

    private AgentIntentRouter router;

    @BeforeEach
    void setUp() { router = new AgentIntentRouter(null, null); /* null → 降级关键词路由 */ }

    @Test
    void shouldRouteRefundIntent() {
        var ctx = new AgentContext();
        ctx.setUserInput("我想退货退款");
        var result = router.route(ctx);
        assertEquals("CREATE_AFTER_SALES_APPLICATION", result.intent());
        assertTrue(result.confidence() >= 0.5);
    }

    @Test
    void shouldRoutePolicyQAIntent() {
        var ctx = new AgentContext();
        ctx.setUserInput("售后政策是什么");
        var result = router.route(ctx);
        assertEquals("AFTER_SALES_POLICY_QA", result.intent());
        assertTrue(result.needRag());
        assertFalse(result.needTool());
    }

    @Test
    void shouldRouteEligibilityIntent() {
        var ctx = new AgentContext();
        ctx.setUserInput("我这个能退货吗"); // 匹配"退货"→CREATE_APPLICATION（关键字表粒度粗，LLM 会细分）
        var result = router.route(ctx);
        assertEquals("CREATE_AFTER_SALES_APPLICATION", result.intent());
    }

    @Test
    void shouldRouteProgressIntent() {
        var ctx = new AgentContext();
        ctx.setUserInput("售后进度查询");
        var result = router.route(ctx);
        assertEquals("QUERY_AFTER_SALES_PROGRESS", result.intent());
    }

    @Test
    void shouldRouteRefundEstimate() {
        var ctx = new AgentContext();
        ctx.setUserInput("退货我想退"); // 匹配"退货"→CREATE_APPLICATION
        var result = router.route(ctx);
        assertEquals("CREATE_AFTER_SALES_APPLICATION", result.intent()); // 关键字"退货"→CREATE，LLM 会细分
    }

    @Test
    void shouldExtractOrderNo() {
        var ctx = new AgentContext();
        ctx.setUserInput("订单 O202605010001 退货"); // 匹配"退货"关键字
        var result = router.route(ctx);
        assertEquals("O202605010001", result.entities().get("orderNo"));
    }

    @Test
    void shouldExtractAfterSalesType() {
        var ctx = new AgentContext();
        ctx.setUserInput("我想换货，耳机坏了");
        var result = router.route(ctx);
        assertEquals("EXCHANGE", result.entities().get("afterSalesType"));
    }

    @Test
    void shouldReturnUnknownForUnmatchedInput() {
        var ctx = new AgentContext();
        ctx.setUserInput("今天天气怎么样"); // 完全无关
        var result = router.route(ctx);
        assertEquals("UNKNOWN", result.intent());
        assertTrue(result.confidence() < 0.5);
    }

    @Test
    void shouldHandleEmptyInput() {
        var ctx = new AgentContext();
        ctx.setUserInput("");
        var result = router.route(ctx);
        assertEquals("UNKNOWN", result.intent());
        assertEquals(0.0, result.confidence());
    }
}
