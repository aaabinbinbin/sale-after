package com.aftersales.api.controller;

import com.aftersales.biz.service.OrderBizService;
import com.aftersales.common.result.Result;
import com.aftersales.infra.entity.TradeOrderItem;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单接口控制器。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderBizService orderBizService;

    public OrderController(OrderBizService orderBizService) {
        this.orderBizService = orderBizService;
    }

    /**
     * 查询订单列表。
     */
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> data = orderBizService.listOrders(pageNum, pageSize);
        return Result.ok(data);
    }

    /**
     * 查询订单详情（含订单项、支付记录、可售后提示）。
     */
    @GetMapping("/{orderNo}")
    public Result<Map<String, Object>> detail(@PathVariable String orderNo) {
        Map<String, Object> data = orderBizService.getOrderDetail(orderNo);
        return Result.ok(data);
    }

    /**
     * 查询订单项列表。
     */
    @GetMapping("/{orderNo}/items")
    public Result<List<TradeOrderItem>> items(@PathVariable String orderNo) {
        List<TradeOrderItem> data = orderBizService.getOrderItems(orderNo);
        return Result.ok(data);
    }
}
