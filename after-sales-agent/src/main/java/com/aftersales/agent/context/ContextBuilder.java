package com.aftersales.agent.context;

import com.aftersales.agent.risk.AgentRiskGuard;
import com.aftersales.agent.trace.AgentTraceService;
import com.aftersales.biz.rule.AfterSaleRuleEngine;
import lombok.Data;

import com.aftersales.biz.risk.RiskContext;
import com.aftersales.biz.risk.RiskEngine;
import com.aftersales.biz.risk.RiskResult;
import com.aftersales.biz.service.AfterSalesApplicationService;
import com.aftersales.biz.service.OrderBizService;
import com.aftersales.common.context.UserContext;
import com.aftersales.infra.entity.TradeOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 上下文构建器。
 *
 * 分层构建 Agent 所需的完整上下文，禁止 Controller / Service 手工拼接 Prompt。
 *
 * 分层结构：
 * 1. UserContext      — 用户画像
 * 2. ConversationContext — 对话历史
 * 3. BusinessContext   — 业务数据（订单、售后单）
 * 4. RiskContext       — 风控评估结果
 * 5. PolicyContext     — 适用策略
 * 6. MemoryContext     — 长期记忆
 */
@Component
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    private final OrderBizService orderBizService;
    private final AfterSalesApplicationService afterSalesService;
    private final AfterSaleRuleEngine ruleEngine;
    private final RiskEngine riskEngine;

    public ContextBuilder(OrderBizService orderBizService,
                          AfterSalesApplicationService afterSalesService,
                          AfterSaleRuleEngine ruleEngine,
                          RiskEngine riskEngine) {
        this.orderBizService = orderBizService;
        this.afterSalesService = afterSalesService;
        this.ruleEngine = ruleEngine;
        this.riskEngine = riskEngine;
    }

    /**
     * 构建完整上下文。
     */
    public BuiltContext build(String userId, String userInput, String orderNo,
                               String afterSalesNo, String intent) {
        BuiltContext ctx = new BuiltContext();
        ctx.setUserId(userId);
        ctx.setUserInput(userInput);
        ctx.setOrderNo(orderNo);
        ctx.setAfterSalesNo(afterSalesNo);
        ctx.setIntent(intent);

        // Layer 1: UserContext
        ctx.setUserProfile(buildUserProfile(userId));

        // Layer 2: ConversationContext (由 AgentLoop 填充)
        ctx.setConversationHistory(new ArrayList<>());

        // Layer 3: BusinessContext
        if (orderNo != null) {
            try {
                Map<String, Object> orderDetail = orderBizService.getOrderDetail(orderNo);
                ctx.setBusinessData(Map.of("order", orderDetail));
            } catch (Exception e) {
                log.warn("订单查询失败 orderNo={}: {}", orderNo, e.getMessage());
                ctx.setBusinessData(Map.of("order", "查询失败"));
            }
        }

        if (afterSalesNo != null) {
            try {
                Map<String, Object> asDetail = afterSalesService.getDetail(afterSalesNo);
                Map<String, Object> bizData = new LinkedHashMap<>(ctx.getBusinessData());
                bizData.put("afterSales", asDetail);
                ctx.setBusinessData(bizData);
            } catch (Exception e) {
                log.warn("售后查询失败 afterSalesNo={}: {}", afterSalesNo, e.getMessage());
            }
        }

        // Layer 4: RiskContext
        RiskResult riskResult = assessRisk(userId, orderNo, afterSalesNo);
        ctx.setRiskResult(riskResult);

        // Layer 5-6: Policy / Memory (由 AgentFacade 后续填充)
        ctx.setApplicablePolicies(List.of());
        ctx.setMemoryHints(List.of());

        log.info("ContextBuilder 构建完成: userId={}, intent={}, layers=4", userId, intent);
        return ctx;
    }

    /** 构建用户画像 */
    private Map<String, Object> buildUserProfile(String userId) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", userId);
        // 后续可接入用户中心获取 VIP 等级、注册时间等
        return profile;
    }

    /** 快速风控评估 */
    private RiskResult assessRisk(String userId, String orderNo, String afterSalesNo) {
        try {
            RiskContext riskCtx = RiskContext.builder()
                    .userId(userId).afterSalesNo(afterSalesNo)
                    .orderNo(orderNo).applyAmount(BigDecimal.ZERO).build();
            return riskEngine.assess(riskCtx);
        } catch (Exception e) {
            log.warn("风控评估失败: {}", e.getMessage());
            return RiskResult.low();
        }
    }

    @Data
    public static class BuiltContext {
        private String userId;
        private String userInput;
        private String orderNo;
        private String afterSalesNo;
        private String intent;
        private Map<String, Object> userProfile = Map.of();
        private List<Map<String, Object>> conversationHistory = List.of();
        private Map<String, Object> businessData = new LinkedHashMap<>();
        private RiskResult riskResult;
        private List<String> applicablePolicies = List.of();
        private List<String> memoryHints = List.of();

        public String toPromptFragment() {
            StringBuilder sb = new StringBuilder();
            sb.append("【用户信息】").append(userProfile).append("\n");
            sb.append("【业务数据】").append(businessData).append("\n");
            if (riskResult != null && riskResult.needManualReview()) {
                sb.append("【风控提醒】风险等级:").append(riskResult.getRiskLevel())
                        .append(" 评分:").append(riskResult.getRiskScore())
                        .append(" 原因:").append(riskResult.getRiskReasons()).append("\n");
            }
            if (!memoryHints.isEmpty()) {
                sb.append("【历史记忆】").append(memoryHints).append("\n");
            }
            return sb.toString();
        }
    }
}
