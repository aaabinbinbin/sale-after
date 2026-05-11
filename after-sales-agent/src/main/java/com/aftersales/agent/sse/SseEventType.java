package com.aftersales.agent.sse;

/**
 * SSE 事件类型枚举。
 *
 * 对应 CLAUDE.md Agent 约束第 10 条：
 * SSE 事件包括 trace、thought、tool、retrieve、delta、confirm、error、done。
 */
public enum SseEventType {

    /** 请求进入，返回 traceId */
    TRACE("trace"),

    /** 当前阶段描述（意图识别中、查询订单中...） */
    THOUGHT("thought"),

    /** 工具/Skill 调用状态，含 toolName + status（STARTED/SUCCESS/FAILED） */
    TOOL("tool"),

    /** RAG 检索结果，含 hitCount + topDocs */
    RETRIEVE("retrieve"),

    /** 最终回答文本增量 */
    DELTA("delta"),

    /** 高风险确认动作，含 confirmToken + actionType */
    CONFIRM("confirm"),

    /** 错误事件，含 message */
    ERROR("error"),

    /** 完成事件，含 traceId */
    DONE("done");

    private final String eventName;

    SseEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() { return eventName; }
}
