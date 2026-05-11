package com.aftersales.agent.memory;

/**
 * 记忆类型。
 */
public enum MemoryType {
    SHORT_TERM,     // 当前会话
    LONG_TERM,      // 用户长期偏好
    BUSINESS,       // 售后历史
    WORKFLOW,       // 当前任务状态
    SEMANTIC        // RAG 知识
}
