package com.aftersales.biz.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.enums.UserRole;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.infra.entity.PaymentRecord;
import com.aftersales.infra.entity.TradeOrder;
import com.aftersales.infra.entity.TradeOrderItem;
import com.aftersales.infra.mapper.PaymentRecordMapper;
import com.aftersales.infra.mapper.TradeOrderItemMapper;
import com.aftersales.infra.mapper.TradeOrderMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 订单业务服务。
 *
 * 负责订单查询的用例编排，不写 SQL，不直接操作 Mapper（复杂逻辑委托 Service）。
 * 普通用户只能查自己的订单，客服/管理员可查全部。
 */
@Service
public class OrderBizService {

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public OrderBizService(TradeOrderMapper tradeOrderMapper,
                           TradeOrderItemMapper tradeOrderItemMapper,
                           PaymentRecordMapper paymentRecordMapper) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderItemMapper = tradeOrderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    /**
     * 查询订单列表。
     */
    public Map<String, Object> listOrders(int pageNum, int pageSize) {
        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();

        int offset = (pageNum - 1) * pageSize;
        List<TradeOrder> orders;
        long total;

        // 普通用户只能看自己的订单
        if (UserRole.USER.getCode().equals(role)) {
            orders = tradeOrderMapper.selectByUserId(userId, offset, pageSize);
            total = tradeOrderMapper.countByUserId(userId);
        } else {
            orders = tradeOrderMapper.selectAll(offset, pageSize);
            total = tradeOrderMapper.countAll();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", orders);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("pages", pageSize > 0 ? (total + pageSize - 1) / pageSize : 0);
        return result;
    }

    /**
     * 查询订单详情（含订单项和支付记录）。
     */
    public Map<String, Object> getOrderDetail(String orderNo) {
        TradeOrder order = tradeOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 普通用户只能看自己的订单
        String role = UserContext.getRole();
        if (UserRole.USER.getCode().equals(role) && !order.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_BELONG_TO_USER);
        }

        List<TradeOrderItem> items = tradeOrderItemMapper.selectByOrderId(order.getId());
        List<PaymentRecord> payments = paymentRecordMapper.selectByOrderId(order.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("payments", payments);
        // 提示哪些订单项可以售后
        List<Map<String, Object>> afterSalesHints = new ArrayList<>();
        for (TradeOrderItem item : items) {
            if ("NONE".equals(item.getAfterSalesStatus())) {
                Map<String, Object> hint = new LinkedHashMap<>();
                hint.put("orderItemId", item.getId());
                hint.put("skuName", item.getSkuName());
                hint.put("refundableAmount", item.getRefundableAmount());
                hint.put("canApplyAfterSales", true);
                afterSalesHints.add(hint);
            }
        }
        result.put("afterSalesHints", afterSalesHints);
        return result;
    }

    /**
     * 查询订单项列表。
     */
    public List<TradeOrderItem> getOrderItems(String orderNo) {
        TradeOrder order = tradeOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        String role = UserContext.getRole();
        if (UserRole.USER.getCode().equals(role) && !order.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_BELONG_TO_USER);
        }
        return tradeOrderItemMapper.selectByOrderId(order.getId());
    }
}
