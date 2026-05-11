package com.aftersales.agent.trace;

import com.aftersales.common.util.IdGenerator;
import com.aftersales.infra.entity.AgentLlmCall;
import com.aftersales.infra.entity.AgentToolCall;
import com.aftersales.infra.entity.AgentTrace;
import com.aftersales.infra.mapper.AgentLlmCallMapper;
import com.aftersales.infra.mapper.AgentToolCallMapper;
import com.aftersales.infra.mapper.AgentTraceMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Agent Trace 服务。
 *
 * 记录每次 Agent 调用的完整轨迹：
 * - agent_trace：请求级别（意图、风险、最终回答）
 * - agent_tool_call：每个 Skill 执行
 * - agent_llm_call：每次 LLM 调用（prompt、response、token）
 */
@Service
public class AgentTraceService {

    private final AgentTraceMapper traceMapper;
    private final AgentToolCallMapper toolCallMapper;
    private final AgentLlmCallMapper llmCallMapper;

    public AgentTraceService(AgentTraceMapper traceMapper,
                              AgentToolCallMapper toolCallMapper,
                              AgentLlmCallMapper llmCallMapper) {
        this.traceMapper = traceMapper;
        this.toolCallMapper = toolCallMapper;
        this.llmCallMapper = llmCallMapper;
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

    /** 记录工具调用（含 tool output） */
    public void recordToolCall(String traceId, String toolName, String toolInput,
                                String toolOutput, boolean success, String error) {
        AgentToolCall call = new AgentToolCall();
        call.setTraceId(traceId);
        call.setToolName(toolName);
        call.setToolInput(truncate(toolInput, 2000));
        call.setToolOutput(truncate(toolOutput, 2000));
        call.setSuccess(success);
        call.setErrorMessage(error);
        call.setCreatedAt(java.time.LocalDateTime.now());
        toolCallMapper.insert(call);
    }

    /** 记录 LLM 调用 */
    public void recordLlmCall(String traceId, String callType, String model, int roundNum,
                               String systemPrompt, String userPrompt, String rawResponse,
                               int inputTokens, int outputTokens, long latencyMs,
                               boolean success, String errorMessage) {
        AgentLlmCall call = new AgentLlmCall();
        call.setTraceId(traceId);
        call.setCallType(callType);
        call.setModel(model);
        call.setRoundNum(roundNum);
        call.setSystemPrompt(truncate(systemPrompt, 5000));
        call.setUserPrompt(truncate(userPrompt, 5000));
        call.setRawResponse(truncate(rawResponse, 5000));
        call.setInputTokens(inputTokens);
        call.setOutputTokens(outputTokens);
        call.setTotalTokens(inputTokens + outputTokens);
        call.setLatencyMs(latencyMs);
        call.setSuccess(success);
        call.setErrorMessage(truncate(errorMessage, 1000));
        call.setCreatedAt(java.time.LocalDateTime.now());
        llmCallMapper.insert(call);
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

    /** 查询 Trace 含 LLM 调用记录 */
    public Map<String, Object> getTraceDetail(String traceId) {
        AgentTrace trace = traceMapper.selectByTraceId(traceId);
        List<AgentToolCall> toolCalls = toolCallMapper.selectByTraceId(traceId);
        List<AgentLlmCall> llmCalls = llmCallMapper.selectByTraceId(traceId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("trace", trace);
        detail.put("toolCalls", toolCalls);
        detail.put("llmCalls", llmCalls);
        return detail;
    }

    /** 查询最近 Trace */
    public List<AgentTrace> recentTraces(Long userId, int limit) {
        return traceMapper.selectByUserId(userId, limit);
    }

    /** 查询 LLM 调用记录 */
    public List<AgentLlmCall> getLlmCallsByTraceId(String traceId) {
        return llmCallMapper.selectByTraceId(traceId);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...(truncated)";
    }
}
