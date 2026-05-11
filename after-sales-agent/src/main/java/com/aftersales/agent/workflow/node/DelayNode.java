package com.aftersales.agent.workflow.node;

import com.aftersales.agent.workflow.NodeResult;
import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowNode;

import java.util.Map;

/**
 * 延时等待节点。
 *
 * 用于需要等待外部回调的场景（如等待物流信息、等待支付确认）。
 * 实际延时由调用方通过定时任务 + WorkflowEngine.resume() 实现。
 */
public class DelayNode implements WorkflowNode {

    private final String id;
    private final String description;
    private final long delaySeconds;
    private final String waitReason;

    public DelayNode(String id, String description, long delaySeconds, String waitReason) {
        this.id = id;
        this.description = description;
        this.delaySeconds = delaySeconds;
        this.waitReason = waitReason;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getType() { return "DelayNode"; }

    @Override
    public NodeResult execute(WorkflowContext context) {
        context.setVariable(id + ".delayUntil",
                System.currentTimeMillis() + delaySeconds * 1000);
        context.setVariable(id + ".waitReason", waitReason);

        return NodeResult.end(Map.of(
                "nodeId", id,
                "description", description,
                "delaySeconds", delaySeconds,
                "waitReason", waitReason,
                "status", "WAITING_DELAY"
        ));
    }
}
