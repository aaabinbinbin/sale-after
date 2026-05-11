package com.aftersales.agent.tool;

import com.aftersales.biz.service.AfterSalesApplicationService;
import com.aftersales.biz.service.OrderBizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 订单领域工具聚合。
 *
 * 用 getOrderSummary() 替代多个细粒度的 queryOrderItem/Status/Price 调用，
 * 减少 Agent 的认知负担和 Tool 选择混乱。
 */
@Component
public class OrderDomainTools {

    private static final Logger log = LoggerFactory.getLogger(OrderDomainTools.class);

    private final OrderBizService orderBizService;
    private final AfterSalesApplicationService afterSalesService;

    public OrderDomainTools(OrderBizService orderBizService,
                            AfterSalesApplicationService afterSalesService) {
        this.orderBizService = orderBizService;
        this.afterSalesService = afterSalesService;
    }

    /**
     * 获取订单摘要（一次调用返回 Agent 所需的所有订单信息）。
     *
     * @param orderNo 订单号
     * @return 包含订单基本信息、商品明细、支付状态、售后提示的聚合结果
     */
    public Map<String, Object> getOrderSummary(String orderNo) {
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            Map<String, Object> detail = orderBizService.getOrderDetail(orderNo);
            summary.put("orderNo", orderNo);
            summary.put("status", detail.get("orderStatus"));
            summary.put("totalAmount", detail.get("totalAmount"));
            summary.put("paidAmount", detail.get("paidAmount"));
            summary.put("items", detail.get("items"));
            summary.put("payments", detail.get("payments"));
            summary.put("afterSalesHints", detail.get("afterSalesHints"));
            summary.put("canApplyAfterSales", detail.get("canApplyAfterSales"));
            log.info("OrderDomainTools: 订单摘要构建完成 orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("订单摘要构建失败 orderNo={}: {}", orderNo, e.getMessage());
            summary.put("error", e.getMessage());
        }
        return summary;
    }

    /**
     * 获取订单售后历史。
     */
    public List<Map<String, Object>> getOrderAfterSalesHistory(Long userId, int limit) {
        try {
            Map<String, Object> result = afterSalesService.listAfterSales(null, null, null, 1, limit);
            return (List<Map<String, Object>>) result.get("records");
        } catch (Exception e) {
            log.error("售后历史查询失败: {}", e.getMessage());
            return List.of();
        }
    }
}
