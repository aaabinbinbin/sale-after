package com.aftersales.agent.trace;

import com.aftersales.common.util.IdGenerator;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.infra.entity.AgentTrace;
import com.aftersales.infra.entity.AgentToolCall;
import com.aftersales.infra.mapper.AgentTraceMapper;
import com.aftersales.infra.mapper.AgentToolCallMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Agent Trace 服务。
 *
 * 记录每次 Agent 调用的 traceId、意图、工具调用、最终回答等。
 */
@Service
public class AgentTraceService {

    private final AgentTraceMapper traceMapper;
    private final AgentToolCallMapper toolCallMapper;

    public AgentTraceService(AgentTraceMapper traceMapper, AgentToolCallMapper toolCallMapper) {
        this.traceMapper = traceMapper;
        this.toolCallMapper = toolCallMapper;
    }

    /** 创建 Trace */
    public AgentTrace createTrace(Long userId, String conversationId, String userInput) {
        AgentTrace trace = new AgentTrace();
        trace.setTraceId(IdGenerator.genTraceId());
        trace.setUserId(userId);
        trace.setConversationId(conversationId);
        trace.setUserInput(userInput);
        trace.setStatus("PROCESSING");
        trace.setCreatedAt(java.time.LocalDateTime.now());
        trace.setUpdatedAt(java.time.LocalDateTime.now());
        traceMapper.insert(trace);
        return trace;
    }

    /** 记录工具调用 */
    public void recordToolCall(String traceId, String toolName, String toolInput, boolean success, String error) {
        AgentToolCall call = new AgentToolCall();
        call.setTraceId(traceId);
        call.setToolName(toolName);
        call.setToolInput(toolInput);
        call.setSuccess(success);
        call.setErrorMessage(error);
        call.setCreatedAt(java.time.LocalDateTime.now());
        toolCallMapper.insert(call);
    }

    /** 最终更新 Trace */
    public void completeTrace(String traceId, String intent, String riskLevel,
                               String finalAnswer, String status, String error, long latencyMs) {
        traceMapper.updateResult(traceId, intent, riskLevel, finalAnswer, status, error, latencyMs);
    }

    /** 查询 Trace */
    public AgentTrace getTrace(String traceId) {
        return traceMapper.selectByTraceId(traceId);
    }

    /** 查询最近 Trace */
    public List<AgentTrace> recentTraces(Long userId, int limit) {
        return traceMapper.selectByUserId(userId, limit);
    }
}
