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
     * Agent SSE 流式对话。
     *
     * 事件类型：trace, thought, tool, retrieve, delta, confirm, error, done
     */
    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时

        Long userId = UserContext.getUserId();
        String username = UserContext.get().getUsername();
        String role = UserContext.getRole();
        String conversationId = (String) body.getOrDefault("conversationId", "c-" + UUID.randomUUID());
        String userInput = (String) body.get("userInput");
        String orderNo = (String) body.get("orderNo");
        String afterSalesNo = (String) body.get("afterSalesNo");

        // 异步处理，通过 SSE 推送事件
        new Thread(() -> {
            try {
                // 1. trace 事件
                String traceId = "tr-" + System.currentTimeMillis();
                sendSseEvent(emitter, "trace", Map.of("traceId", traceId));

                // 2. thought 事件
                sendSseEvent(emitter, "thought", Map.of("content", "正在分析你的售后诉求..."));

                // 3. 调用 AgentFacade
                Map<String, Object> result = agentFacade.chat(userId, username, role, conversationId,
                        userInput, orderNo, afterSalesNo);

                // 4. tool 事件（模拟工具调用过程）
                sendSseEvent(emitter, "tool", Map.of("toolName", "IntentRouter", "status", "SUCCESS"));

                // 5. delta 事件
                String answer = (String) result.get("answer");
                sendSseEvent(emitter, "delta", Map.of("content", answer));

                // 6. confirm 事件（如果需要确认）
                if (Boolean.TRUE.equals(result.get("requiresConfirmation"))) {
                    sendSseEvent(emitter, "confirm", Map.of(
                            "confirmToken", result.get("confirmToken"),
                            "actionType", ((Map<?, ?>) result.get("suggestedAction")).get("actionType")));
                }

                // 7. done 事件
                sendSseEvent(emitter, "done", Map.of("traceId", result.get("traceId")));
                emitter.complete();
            } catch (Exception e) {
                try {
                    sendSseEvent(emitter, "error", Map.of("message", e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();

        return emitter;
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
