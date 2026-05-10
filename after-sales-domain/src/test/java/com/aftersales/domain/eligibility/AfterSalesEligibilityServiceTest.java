package com.aftersales.domain.eligibility;

import com.aftersales.infra.entity.TradeOrder;
import com.aftersales.infra.entity.TradeOrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 售后资格校验服务单元测试。
 */
class AfterSalesEligibilityServiceTest {

    private AfterSalesEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new AfterSalesEligibilityService();
    }

    @Test
    void shouldFailWhenOrderNotFound() {
        EligibilityContext ctx = new EligibilityContext();
        ctx.setUserId(10001L);
        ctx.setOrder(null); // 订单不存在

        var result = service.check(ctx);
        assertFalse(result.isPassed());
        assertTrue(result.getErrorMessage().contains("订单不存在"));
    }

    @Test
    void shouldFailWhenOrderNotBelongToUser() {
        TradeOrder order = new TradeOrder();
        order.setId(1001L);
        order.setUserId(10002L); // 不属于用户 10001
        order.setOrderStatus("DELIVERED");

        EligibilityContext ctx = new EligibilityContext();
        ctx.setUserId(10001L);
        ctx.setOrder(order);

        var result = service.check(ctx);
        assertFalse(result.isPassed());
        assertTrue(result.getErrorMessage().contains("不属于"));
    }

    @Test
    void shouldFailWhenOrderStatusNotAllow() {
        TradeOrder order = new TradeOrder();
        order.setId(1001L);
        order.setUserId(10001L);
        order.setOrderStatus("PENDING_PAYMENT"); // 未支付不允许售后

        EligibilityContext ctx = new EligibilityContext();
        ctx.setUserId(10001L);
        ctx.setOrder(order);

        var result = service.check(ctx);
        assertFalse(result.isPassed());
    }

    @Test
    void shouldPassForValidOrder() {
        TradeOrder order = new TradeOrder();
        order.setId(1001L);
        order.setUserId(10001L);
        order.setOrderStatus("DELIVERED");

        TradeOrderItem item = new TradeOrderItem();
        item.setId(10001L);
        item.setOrderId(1001L);
        item.setQuantity(1);
        item.setRefundableAmount(new BigDecimal("199.00"));
        item.setAfterSalesStatus("NONE");

        var applyItem = new EligibilityContext.ApplyItem();
        applyItem.setOrderItemId(10001L);
        applyItem.setApplyQuantity(1);
        applyItem.setApplyAmount(new BigDecimal("199.00"));

        EligibilityContext ctx = new EligibilityContext();
        ctx.setUserId(10001L);
        ctx.setOrder(order);
        ctx.setOrderItems(List.of(item));
        ctx.setApplyItems(List.of(applyItem));

        var result = service.check(ctx);
        assertTrue(result.isPassed(), "应该通过: " + result.getErrorMessage());
    }

    @Test
    void shouldFailWhenApplyQuantityExceedsOrderQuantity() {
        TradeOrder order = new TradeOrder();
        order.setId(1001L);
        order.setUserId(10001L);
        order.setOrderStatus("DELIVERED");

        TradeOrderItem item = new TradeOrderItem();
        item.setId(10001L);
        item.setOrderId(1001L);
        item.setQuantity(1); // 只买了1个
        item.setRefundableAmount(new BigDecimal("199.00"));
        item.setAfterSalesStatus("NONE");

        var applyItem = new EligibilityContext.ApplyItem();
        applyItem.setOrderItemId(10001L);
        applyItem.setApplyQuantity(3); // 申请退3个

        EligibilityContext ctx = new EligibilityContext();
        ctx.setUserId(10001L);
        ctx.setOrder(order);
        ctx.setOrderItems(List.of(item));
        ctx.setApplyItems(List.of(applyItem));

        var result = service.check(ctx);
        assertFalse(result.isPassed());
        assertTrue(result.getErrorMessage().contains("超过"));
    }
}
