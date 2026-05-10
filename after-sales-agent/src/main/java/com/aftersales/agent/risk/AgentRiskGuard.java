package com.aftersales.agent.risk;

import com.aftersales.common.enums.AgentIntent;
import com.aftersales.common.enums.AgentRiskLevel;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Agent 风险守卫。
 *
 * 判断意图对应的风险等级，高风险动作必须生成 confirmToken。
 */
@Component
public class AgentRiskGuard {

    // 高风险意图（需要确认）
    private static final Set<String> HIGH_RISK_INTENTS = Set.of(
            AgentIntent.CREATE_AFTER_SALES_APPLICATION.getCode(),
            "APPROVE_AFTER_SALES",
            "EXECUTE_REFUND",
            "GRANT_COMPENSATION",
            "SHIP_EXCHANGE",
            "CONFIRM_RETURN_RECEIVED"
    );

    // 中风险意图
    private static final Set<String> MEDIUM_RISK_INTENTS = Set.of(
            AgentIntent.REFUND_ESTIMATION.getCode(),
            AgentIntent.EXCHANGE_STOCK_CHECK.getCode(),
            AgentIntent.CUSTOMER_SERVICE_ASSISTANT.getCode()
    );

    /**
     * 评估风险等级。
     */
    public AgentRiskLevel assess(String intent) {
        if (HIGH_RISK_INTENTS.contains(intent)) return AgentRiskLevel.HIGH;
        if (MEDIUM_RISK_INTENTS.contains(intent)) return AgentRiskLevel.MEDIUM;
        return AgentRiskLevel.LOW;
    }

    /**
     * 判断是否需要确认。
     */
    public boolean requiresConfirmation(String intent) {
        return assess(intent).requiresConfirmation();
    }
}
