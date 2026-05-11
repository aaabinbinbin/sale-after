package com.aftersales.agent.workflow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class WorkflowResult {

    private final boolean completed;
    private final boolean waitingApproval;
    private final WorkflowContext context;
    private final String error;

    public static WorkflowResult completed(WorkflowContext ctx) {
        return new WorkflowResult(true, false, ctx, null);
    }

    public static WorkflowResult failed(WorkflowContext ctx, String error) {
        return new WorkflowResult(false, false, ctx, error);
    }

    public static WorkflowResult waitingApproval(WorkflowContext ctx) {
        return new WorkflowResult(false, true, ctx, null);
    }

    public Map<String, NodeResult> getNodeResults() {
        return context != null ? context.getNodeResults() : Collections.emptyMap();
    }

    public List<String> getExecutionPath() {
        return context != null ? context.getExecutionPath() : Collections.emptyList();
    }
}
