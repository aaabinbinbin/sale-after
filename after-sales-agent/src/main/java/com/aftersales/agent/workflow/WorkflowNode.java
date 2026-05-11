package com.aftersales.agent.workflow;

/**
 * 工作流节点接口。
 *
 * 所有节点类型（RuleNode / ToolNode / LLMNode / ApprovalNode / EventNode / DelayNode）
 * 实现此接口。每个节点返回一个 NodeResult，指示下一个节点 ID 或终止。
 */
public interface WorkflowNode {

    /** 节点唯一标识 */
    String getId();

    /** 节点类型标签 */
    String getType();

    /** 执行节点逻辑，返回结果指示下一步 */
    NodeResult execute(WorkflowContext context);
}
