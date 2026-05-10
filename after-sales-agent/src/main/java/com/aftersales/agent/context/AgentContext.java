package com.aftersales.agent.context;

import java.util.*;

/**
 * Agent 执行上下文。
 *
 * 包含当前用户信息、输入、订单/售后摘要等。
 */
public class AgentContext {

    private String traceId;
    private String conversationId;
    private Long userId;
    private String username;
    private String role;
    private String userInput;
    private String orderNo;
    private String afterSalesNo;
    private String intent;
    private String riskLevel;

    // 扩展数据
    private Map<String, Object> extra = new LinkedHashMap<>();

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUserInput() { return userInput; }
    public void setUserInput(String userInput) { this.userInput = userInput; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getAfterSalesNo() { return afterSalesNo; }
    public void setAfterSalesNo(String afterSalesNo) { this.afterSalesNo = afterSalesNo; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }

    public void putExtra(String key, Object value) { this.extra.put(key, value); }
    public Object getExtra(String key) { return extra.get(key); }
}
