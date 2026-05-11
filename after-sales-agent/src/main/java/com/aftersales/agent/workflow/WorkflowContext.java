package com.aftersales.agent.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.*;

@Data
@Builder
public class WorkflowContext {

    private final String workflowId;
    private final String workflowName;
    private final String userId;

    @Builder.Default
    private final Map<String, Object> variables = new LinkedHashMap<>();
    @Builder.Default
    private final Map<String, NodeResult> nodeResults = new LinkedHashMap<>();
    @Builder.Default
    private final List<String> executionPath = new ArrayList<>();

    @Builder.Default
    private WorkflowStatus status = WorkflowStatus.RUNNING;
    private String currentNodeId;
    private String approvalReason;
    private String approvalToken;

    public void setVariable(String key, Object value) { variables.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        Object v = variables.get(key);
        return v != null ? (T) v : null;
    }

    public Object getVariable(String key) { return variables.get(key); }

    public void recordNodeResult(String nodeId, NodeResult result) {
        nodeResults.put(nodeId, result);
        executionPath.add(nodeId);
    }

    public Map<String, NodeResult> getNodeResults() { return Collections.unmodifiableMap(nodeResults); }
    public List<String> getExecutionPath() { return Collections.unmodifiableList(executionPath); }
}
