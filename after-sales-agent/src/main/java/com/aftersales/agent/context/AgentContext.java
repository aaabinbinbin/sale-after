package com.aftersales.agent.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Agent 执行上下文。贯穿整个请求生命周期，每个组件向自己负责的字段写入数据。
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentContext {

    // ====== 固定字段（请求进入时填充） ======
    private String traceId;
    private String conversationId;
    private Long userId;
    private String username;
    private String role;
    private String userInput;
    private String orderNo;
    private String afterSalesNo;

    // ====== Step 1 填充：IntentRouter 输出 ======
    private String intent;
    private String riskLevel;

    // ====== Step 3-4 填充：按需获取的上下文数据 ======
    private Map<String, Object> contextData = new LinkedHashMap<>();

    // ====== Step 5 填充：每个 Skill 的执行结果 ======
    private Map<String, Object> skillResults = new LinkedHashMap<>();

    // ====== Agent Loop 历史记录 ======
    private List<Map<String, Object>> actionHistory = new ArrayList<>();

    // ====== 扩展字段 ======
    private Map<String, Object> extra = new LinkedHashMap<>();

    // ====== getter / setter ======

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

    public Map<String, Object> getContextData() { return contextData; }
    public void setContextData(Map<String, Object> contextData) { this.contextData = contextData; }
    public void putContextData(String key, Object value) { this.contextData.put(key, value); }

    public Map<String, Object> getSkillResults() { return skillResults; }
    public void setSkillResults(Map<String, Object> skillResults) { this.skillResults = skillResults; }

    public List<Map<String, Object>> getActionHistory() { return actionHistory; }
    public void setActionHistory(List<Map<String, Object>> actionHistory) { this.actionHistory = actionHistory; }
    public void addActionToHistory(String action, Object result) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("action", action);
        record.put("result", result);
        record.put("timestamp", System.currentTimeMillis());
        this.actionHistory.add(record);
    }

    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
    public void putExtra(String key, Object value) { this.extra.put(key, value); }
    public Object getExtra(String key) { return extra.get(key); }
}
