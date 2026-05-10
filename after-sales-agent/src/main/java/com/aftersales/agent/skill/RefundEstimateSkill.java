package com.aftersales.agent.skill;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.infra.entity.TradeOrder;
import com.aftersales.infra.entity.TradeOrderItem;
import com.aftersales.infra.mapper.PaymentRecordMapper;
import com.aftersales.infra.mapper.TradeOrderItemMapper;
import com.aftersales.infra.mapper.TradeOrderMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 退款预估技能。
 *
 * 只是预估，不执行退款。
 */
@Component
public class RefundEstimateSkill implements AgentSkill {

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public RefundEstimateSkill(TradeOrderMapper tradeOrderMapper,
                                TradeOrderItemMapper tradeOrderItemMapper,
                                PaymentRecordMapper paymentRecordMapper) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderItemMapper = tradeOrderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    @Override
    public String name() { return "refund.estimate"; }

    @Override
    public boolean supports(AgentContext context, AgentPlanStep step) {
        return context.getOrderNo() != null;
    }

    @Override
    public AgentSkillResult execute(AgentContext context, AgentPlanStep step) {
        TradeOrder order = tradeOrderMapper.selectByOrderNo(context.getOrderNo());
        if (order == null) return AgentSkillResult.fail("订单不存在");

        List<TradeOrderItem> items = tradeOrderItemMapper.selectByOrderId(order.getId());

        BigDecimal maxRefund = BigDecimal.ZERO;
        List<Map<String, Object>> itemEstimates = new ArrayList<>();
        for (TradeOrderItem item : items) {
            if (!"PROCESSING".equals(item.getAfterSalesStatus())) {
                Map<String, Object> est = new LinkedHashMap<>();
                est.put("orderItemId", item.getId());
                est.put("skuName", item.getSkuName());
                est.put("refundableAmount", item.getRefundableAmount());
                itemEstimates.add(est);
                maxRefund = maxRefund.add(item.getRefundableAmount());
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", context.getOrderNo());
        data.put("paidAmount", order.getPaidAmount());
        data.put("maxRefundable", maxRefund);
        data.put("items", itemEstimates);
        return AgentSkillResult.ok(data);
    }
}
