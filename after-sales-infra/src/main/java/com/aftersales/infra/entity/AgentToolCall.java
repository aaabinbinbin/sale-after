package com.aftersales.infra.entity;

import java.time.LocalDateTime;

/** Agent 工具调用记录实体 */
public class AgentToolCall {
    private Long id;
    private String traceId;
    private String toolName;
    private String toolInput;
    private String toolOutput;
    private Boolean success;
    private String errorMessage;
    private Long latencyMs;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getToolInput() { return toolInput; }
    public void setToolInput(String toolInput) { this.toolInput = toolInput; }
    public String getToolOutput() { return toolOutput; }
    public void setToolOutput(String toolOutput) { this.toolOutput = toolOutput; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
