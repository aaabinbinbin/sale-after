package com.aftersales.agent.orchestrator;

/**
 * Agent 计划步骤的执行模式。
 *
 * SEQUENTIAL        串行执行，上一步的输出作为下一步的输入
 * PARALLEL          并发执行，与其他无依赖步骤同时跑
 * CONFIRM_REQUIRED  暂停执行，生成 confirmToken，等待用户确认
 */
public enum ExecutionMode {
    SEQUENTIAL,
    PARALLEL,
    CONFIRM_REQUIRED
}
