package com.aftersales.agent.workflow.node;

import com.aftersales.agent.workflow.NodeResult;
import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowNode;
import com.aftersales.agent.workflow.WorkflowStatus;

import java.util.Map;
import java.util.UUID;

/**
 * 人工审批节点。
 *
 * 暂停工作流，生成审批 token，等待人工审批后通过 WorkflowEngine.resume() 恢复。
 */
public class ApprovalNode implements WorkflowNode {

    private final String id;
    private final String description;
    private final String approverRole; // 审批人角色要求

    public ApprovalNode(String id, String description, String approverRole) {
        this.id = id;
        this.description = description;
        this.approverRole = approverRole;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getType() { return "ApprovalNode"; }

    @Override
    public NodeResult execute(WorkflowContext context) {
        String token = UUID.randomUUID().toString().replace("-", "");

        context.setStatus(WorkflowStatus.WAITING_APPROVAL);
        context.setApprovalToken(token);
        context.setApprovalReason("工作流 [" + context.getWorkflowName() + "] 节点 [" + id + "] 需要 "
                + approverRole + " 审批");
        context.setVariable("approvalToken", token);

        return NodeResult.end(Map.of(
                "nodeId", id,
                "description", description,
                "approverRole", approverRole,
                "approvalToken", token,
                "status", "WAITING_APPROVAL"
        ));
    }
}
