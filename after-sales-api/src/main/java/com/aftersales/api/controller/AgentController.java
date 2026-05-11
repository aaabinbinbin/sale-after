package com.aftersales.api.controller;

import com.aftersales.agent.facade.AgentFacade;
import com.aftersales.common.context.UserContext;
import com.aftersales.common.result.Result;
import com.aftersales.common.util.JsonUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * Agent 接口控制器。
 *
 * 提供普通 chat、SSE 流式 chat、confirm 确认和 trace 查询。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentFacade agentFacade;

    public AgentController(AgentFacade agentFacade) {
        this.agentFacade = agentFacade;
    }

    /**
     * Agent 普通对话（非流式）。
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        String username = UserContext.get().getUsername();
        String role = UserContext.getRole();
        String conversationId = (String) body.getOrDefault("conversationId", "c-" + UUID.randomUUID());
        String userInput = (String) body.get("userInput");
        String orderNo = (String) body.get("orderNo");
        String afterSalesNo = (String) body.get("afterSalesNo");

        Map<String, Object> data = agentFacade.chat(userId, username, role, conversationId,
                userInput, orderNo, afterSalesNo);
        return Result.ok(data);
    }

    /**
     * Agent SSE 流式对话。实时推送 trace → thought → tool → delta → confirm → done。
     */
    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(300_000L);
        Long userId = UserContext.getUserId();
        String username = UserContext.get().getUsername();
        String role = UserContext.getRole();
        String conversationId = (String) body.getOrDefault("conversationId", "c-" + UUID.randomUUID());
        String userInput = (String) body.get("userInput");
        String orderNo = (String) body.get("orderNo");
        String afterSalesNo = (String) body.get("afterSalesNo");

        new Thread(() -> {
            try {
                agentFacade.chatStream(userId, username, role, conversationId,
                        userInput, orderNo, afterSalesNo,
                        new AgentFacade.StreamCallback() {
                            public void onTrace(String traceId) {
                                safeSend(emitter, "trace", Map.of("traceId", traceId)); }
                            public void onThought(String content) {
                                safeSend(emitter, "thought", Map.of("content", content)); }
                            public void onTool(String toolName, String status) {
                                safeSend(emitter, "tool", Map.of("toolName", toolName, "status", status)); }
                            public void onDelta(String content) {
                                safeSend(emitter, "delta", Map.of("content", content)); }
                            public void onConfirm(String confirmToken, String actionType) {
                                safeSend(emitter, "confirm", Map.of("confirmToken", confirmToken, "actionType", actionType)); }
                            public void onError(String message) {
                                safeSend(emitter, "error", Map.of("message", message)); }
                            public void onDone(String traceId) {
                                safeSend(emitter, "done", Map.of("traceId", traceId));
                                emitter.complete();
                            }
                        });
            } catch (Exception e) {
                safeSend(emitter, "error", Map.of("message", e.getMessage()));
                emitter.complete();
            }
        }).start();
        return emitter;
    }

    private void safeSend(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(
                    com.aftersales.common.util.JsonUtils.toJson(data),
                    MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {}
    }

    /**
     * 确认高风险动作。
     */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody Map<String, String> body) {
        String confirmToken = body.get("confirmToken");
        // 1. 验证 Token
        Map<String, Object> actionPayload = agentFacade.getConfirmService().validateAndGet(confirmToken);

        // 2. 根据 actionType 执行业务动作（简化：标记确认）
        agentFacade.getConfirmService().markConfirmed(confirmToken);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("confirmToken", confirmToken);
        result.put("status", "CONFIRMED");
        result.put("message", "高风险动作已确认，正在执行...");
        return Result.ok(result);
    }

    /**
     * 查询 Trace 详情。
     */
    @GetMapping("/traces/{traceId}")
    public Result<?> getTrace(@PathVariable String traceId) {
        var trace = agentFacade.getTraceService().getTrace(traceId);
        return Result.ok(trace);
    }

    /**
     * 查询最近 Trace 列表。
     */
    @GetMapping("/traces/recent")
    public Result<?> recentTraces(@RequestParam(defaultValue = "10") int limit) {
        var traces = agentFacade.getTraceService().recentTraces(UserContext.getUserId(), limit);
        return Result.ok(traces);
    }

    private void sendSseEvent(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event)
                .data(JsonUtils.toJson(data), MediaType.APPLICATION_JSON));
    }
}
