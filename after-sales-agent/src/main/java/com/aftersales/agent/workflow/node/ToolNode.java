package com.aftersales.agent.workflow.node;

import com.aftersales.agent.workflow.NodeResult;
import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowNode;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * 工具调用节点。
 *
 * 封装对业务 Service / Tool 的调用，执行后进入下一节点。
 */
public class ToolNode implements WorkflowNode {

    private final String id;
    private final String toolName;
    private final Function<WorkflowContext, Map<String, Object>> tool;

    public ToolNode(String id, String toolName, Function<WorkflowContext, Map<String, Object>> tool) {
        this.id = id;
        this.toolName = toolName;
        this.tool = tool;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getType() { return "ToolNode"; }

    @Override
    public NodeResult execute(WorkflowContext context) {
        try {
            Map<String, Object> output = tool.apply(context);
            if (output == null) output = Collections.emptyMap();
            context.setVariable(id + ".output", output);
            output.forEach((k, v) -> context.setVariable(k, v));
            return NodeResult.next(null, output);
        } catch (Exception e) {
            return NodeResult.fail("工具 [" + toolName + "] 执行失败: " + e.getMessage());
        }
    }
}
