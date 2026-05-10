package com.aftersales.domain.eligibility;

import com.aftersales.infra.entity.TradeOrder;
import com.aftersales.infra.entity.TradeOrderItem;

import java.util.List;

/**
 * 售后资格校验上下文。
 *
 * 携带校验所需的所有数据，避免每个 Checker 单独查询数据库。
 */
public class EligibilityContext {

    private Long userId;
    private String orderNo;
    private TradeOrder order; // 订单信息
    private List<TradeOrderItem> orderItems; // 订单项列表
    private String afterSalesType; // 申请的售后类型
    private List<ApplyItem> applyItems; // 申请的售后明细项
    private String reasonCode;

    /** 申请的售后明细项 */
    public static class ApplyItem {
        private Long orderItemId;
        private Integer applyQuantity;
        private java.math.BigDecimal applyAmount;

        public Long getOrderItemId() { return orderItemId; }
        public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
        public Integer getApplyQuantity() { return applyQuantity; }
        public void setApplyQuantity(Integer applyQuantity) { this.applyQuantity = applyQuantity; }
        public java.math.BigDecimal getApplyAmount() { return applyAmount; }
        public void setApplyAmount(java.math.BigDecimal applyAmount) { this.applyAmount = applyAmount; }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public TradeOrder getOrder() { return order; }
    public void setOrder(TradeOrder order) { this.order = order; }

    public List<TradeOrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<TradeOrderItem> orderItems) { this.orderItems = orderItems; }

    public String getAfterSalesType() { return afterSalesType; }
    public void setAfterSalesType(String afterSalesType) { this.afterSalesType = afterSalesType; }

    public List<ApplyItem> getApplyItems() { return applyItems; }
    public void setApplyItems(List<ApplyItem> applyItems) { this.applyItems = applyItems; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
}
