package com.aftersales.agent.workflow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 工作流引擎。
 *
 * 根据 WorkflowDefinition 中定义的 DAG 依次执行节点。
 * 支持条件分支（通过 NodeResult.branch 匹配边）、审批挂起、超时控制。
 *
 * 确定性流程 → Workflow，不确定性决策 → LLM 节点。
 */
@Component
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);
    private static final int MAX_STEPS = 50;

    /**
     * 执行工作流。
     *
     * @param definition 工作流定义
     * @param context    执行上下文
     * @return 工作流执行结果
     */
    public WorkflowResult execute(WorkflowDefinition definition, WorkflowContext context) {
        context.setStatus(WorkflowStatus.RUNNING);

        String currentNodeId = definition.getStartNodeId();
        int step = 0;

        while (currentNodeId != null && step < MAX_STEPS) {
            WorkflowNode node = definition.getNodes().get(currentNodeId);
            if (node == null) {
                log.error("工作流 [{}] 节点 {} 不存在", definition.getName(), currentNodeId);
                context.setStatus(WorkflowStatus.FAILED);
                return WorkflowResult.failed(context, "节点 " + currentNodeId + " 不存在");
            }

            step++;
            context.setCurrentNodeId(currentNodeId);
            log.info("工作流 [{}] 执行节点 [{}/{}]: {}", definition.getName(), step, MAX_STEPS, currentNodeId);

            NodeResult result;
            try {
                result = node.execute(context);
            } catch (Exception e) {
                log.error("工作流 [{}] 节点 {} 执行异常: {}", definition.getName(), currentNodeId, e.getMessage());
                context.recordNodeResult(currentNodeId, NodeResult.fail(e.getMessage()));
                context.setStatus(WorkflowStatus.FAILED);
                return WorkflowResult.failed(context, "节点 " + currentNodeId + " 异常: " + e.getMessage());
            }

            context.recordNodeResult(currentNodeId, result);

            if (!result.isSuccess()) {
                log.warn("工作流 [{}] 节点 {} 失败: {}", definition.getName(), currentNodeId, result.getError());
                context.setStatus(WorkflowStatus.FAILED);
                return WorkflowResult.failed(context, result.getError());
            }

            // 审批节点挂起
            if ("ApprovalNode".equals(node.getType()) && context.getStatus() == WorkflowStatus.WAITING_APPROVAL) {
                log.info("工作流 [{}] 挂起等待审批 at 节点 {}", definition.getName(), currentNodeId);
                return WorkflowResult.waitingApproval(context);
            }

            // 解析下一节点
            currentNodeId = definition.resolveNext(currentNodeId, result.getBranch());
        }

        if (step >= MAX_STEPS) {
            log.error("工作流 [{}] 超过最大步数 {}，强制终止", definition.getName(), MAX_STEPS);
            context.setStatus(WorkflowStatus.FAILED);
            return WorkflowResult.failed(context, "工作流超过最大步数 " + MAX_STEPS);
        }

        context.setStatus(WorkflowStatus.COMPLETED);
        log.info("工作流 [{}] 执行完成, 共 {} 步", definition.getName(), step);
        return WorkflowResult.completed(context);
    }

    /**
     * 从审批挂起点恢复执行。
     *
     * @param definition    工作流定义
     * @param context       执行上下文（需包含 currentNodeId 指向审批节点）
     * @param approvalData  审批结果数据（如 {"approved":true, "remark":"通过"}）
     * @return 工作流执行结果
     */
    public WorkflowResult resume(WorkflowDefinition definition, WorkflowContext context,
                                  Map<String, Object> approvalData) {
        String resumeNodeId = context.getCurrentNodeId();
        if (resumeNodeId == null) {
            return WorkflowResult.failed(context, "无法恢复：上下文中无挂起点");
        }

        // 模拟审批节点返回的 branch
        boolean approved = Boolean.TRUE.equals(approvalData.getOrDefault("approved", false));
        String branch = approved ? "APPROVED" : "REJECTED";

        NodeResult resumeResult = NodeResult.branch(branch, approvalData);
        context.recordNodeResult(resumeNodeId + "_resume", resumeResult);

        String nextNodeId = definition.resolveNext(resumeNodeId, branch);
        if (nextNodeId == null) {
            context.setStatus(WorkflowStatus.COMPLETED);
            return WorkflowResult.completed(context);
        }

        // 继续执行后续节点
        return continueFrom(definition, context, nextNodeId);
    }

    /** 从指定节点继续执行 */
    private WorkflowResult continueFrom(WorkflowDefinition definition, WorkflowContext context, String startNodeId) {
        context.setStatus(WorkflowStatus.RUNNING);
        int step = context.getExecutionPath().size();

        String currentNodeId = startNodeId;
        while (currentNodeId != null && step < MAX_STEPS) {
            WorkflowNode node = definition.getNodes().get(currentNodeId);
            if (node == null) {
                context.setStatus(WorkflowStatus.FAILED);
                return WorkflowResult.failed(context, "节点 " + currentNodeId + " 不存在");
            }
            step++;
            context.setCurrentNodeId(currentNodeId);

            NodeResult result;
            try {
                result = node.execute(context);
            } catch (Exception e) {
                context.recordNodeResult(currentNodeId, NodeResult.fail(e.getMessage()));
                context.setStatus(WorkflowStatus.FAILED);
                return WorkflowResult.failed(context, "节点 " + currentNodeId + " 异常: " + e.getMessage());
            }

            context.recordNodeResult(currentNodeId, result);

            if (!result.isSuccess()) {
                context.setStatus(WorkflowStatus.FAILED);
                return WorkflowResult.failed(context, result.getError());
            }

            if ("ApprovalNode".equals(node.getType()) && context.getStatus() == WorkflowStatus.WAITING_APPROVAL) {
                return WorkflowResult.waitingApproval(context);
            }

            currentNodeId = definition.resolveNext(currentNodeId, result.getBranch());
        }
        context.setStatus(WorkflowStatus.COMPLETED);
        return WorkflowResult.completed(context);
    }
}
