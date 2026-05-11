package com.aftersales.agent.facade;

import com.aftersales.agent.confirm.AgentConfirmService;
import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loop.AgentLoopService;
import com.aftersales.agent.orchestrator.OrchestrationEngine;
import com.aftersales.agent.planner.AgentPlan;
import com.aftersales.agent.risk.AgentRiskGuard;
import com.aftersales.agent.router.AgentIntentRouter;
import com.aftersales.agent.trace.AgentTraceService;
import com.aftersales.infra.entity.AgentTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 主入口 Facade（外观模式）。
 */
@Service
public class AgentFacade {

    private static final Logger log = LoggerFactory.getLogger(AgentFacade.class);
    private static final String MODEL = "qwen3.6-flash";
    private final AgentIntentRouter intentRouter;
    private final AgentLoopService loopService;
    private final OrchestrationEngine orchestrationEngine;
    private final AgentRiskGuard riskGuard;
    private final AgentConfirmService confirmService;
    private final AgentTraceService traceService;
    private final ChatClient chatClient;

    private static final String SUMMARY_PROMPT = """
            你是电商售后助手。根据系统执行结果，用自然语言回复用户。

            要求：
            - 简洁、亲切、专业
            - 如果执行了售后相关的操作，明确告知操作结果
            - 如果有下一步需要用户做的，清晰说明
            - 不要编造没有发生过的事实

            用户原始输入：%s
            意图：%s
            执行结果摘要：%s
            各步骤结果：
            %s

            请生成面向用户的自然语言回复：""";

    public AgentFacade(AgentIntentRouter intentRouter, AgentLoopService loopService,
                        OrchestrationEngine orchestrationEngine, AgentRiskGuard riskGuard,
                        AgentConfirmService confirmService, AgentTraceService traceService,
                        ChatClient chatClient) {
        this.intentRouter = intentRouter;
        this.loopService = loopService;
        this.orchestrationEngine = orchestrationEngine;
        this.riskGuard = riskGuard;
        this.confirmService = confirmService;
        this.traceService = traceService;
        this.chatClient = chatClient;
    }

    /** 处理 Agent 对话 */
    public Map<String, Object> chat(Long userId, String username, String role,
                                     String conversationId, String userInput,
                                     String orderNo, String afterSalesNo) {
        long startTime = System.currentTimeMillis();
        AgentTrace trace = traceService.createTrace(userId, conversationId, userInput);

        AgentContext ctx = new AgentContext();
        ctx.setTraceId(trace.getTraceId());
        ctx.setUserId(userId);
        ctx.setUsername(username);
        ctx.setRole(role);
        ctx.setConversationId(conversationId);
        ctx.setUserInput(userInput);
        ctx.setOrderNo(orderNo);
        ctx.setAfterSalesNo(afterSalesNo);

        try {
            var intentResult = intentRouter.route(ctx);
            ctx.setIntent(intentResult.intent());

            var loopResult = loopService.run(ctx, intentResult.intent(),
                    intentResult.confidence(), intentResult.entities());

            if (loopResult.escalate()) {
                traceService.completeTrace(trace.getTraceId(), intentResult.intent(), "LOW",
                        "转人工: " + loopResult.escalateReason(), "ESCALATED", null,
                        System.currentTimeMillis() - startTime);
                return Map.of("traceId", trace.getTraceId(), "intent", "ESCALATED",
                        "answer", "已转接人工客服处理", "escalated", true, "rounds", loopResult.rounds());
            }

            AgentPlan plan = loopResult.plan();
            var execResult = orchestrationEngine.execute(plan, ctx);
            var assessment = riskGuard.assessQuick(plan, intentResult.confidence());

            if (!assessment.canAutoExecute() || execResult.hasConfirmRequired()) {
                String confirmToken = confirmService.generateToken(trace.getTraceId(),
                        plan.getIntent(), Map.of("summary", plan.getSummary()), 15, userId);
                traceService.completeTrace(trace.getTraceId(), intentResult.intent(), "HIGH",
                        plan.getSummary(), "WAIT_CONFIRM", null, System.currentTimeMillis() - startTime);
                return Map.of("traceId", trace.getTraceId(), "intent", intentResult.intent(),
                        "riskLevel", "HIGH", "answer", plan.getSummary(), "requiresConfirmation", true,
                        "confirmToken", confirmToken);
            }

            // === LLM 生成自然语言回复 ===
            String answer = generateFinalAnswer(ctx, intentResult.intent(), plan, execResult.message());
            traceService.completeTrace(trace.getTraceId(), intentResult.intent(), "LOW",
                    answer, "SUCCESS", null, System.currentTimeMillis() - startTime);
            return Map.of("traceId", trace.getTraceId(), "intent", intentResult.intent(),
                    "riskLevel", "LOW", "answer", answer, "requiresConfirmation", false,
                    "skillResults", ctx.getSkillResults());

        } catch (Exception e) {
            log.error("Agent 异常 traceId={}", trace.getTraceId(), e);
            traceService.completeTrace(trace.getTraceId(), "UNKNOWN", "LOW", null,
                    "ERROR", e.getMessage(), System.currentTimeMillis() - startTime);
            return Map.of("traceId", trace.getTraceId(), "answer", "处理异常，请稍后重试",
                    "error", e.getMessage(), "requiresConfirmation", false);
        }
    }

    /** LLM 基于执行结果生成自然语言回复 */
    private String generateFinalAnswer(AgentContext ctx, String intent, AgentPlan plan, String execMessage) {
        String skillSummary = ctx.getSkillResults().entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + summarizeValue(e.getValue()))
                .collect(Collectors.joining("\n"));
        if (skillSummary.isEmpty()) skillSummary = execMessage;

        String prompt = String.format(SUMMARY_PROMPT,
                ctx.getUserInput(), intent, plan.getSummary(), skillSummary);

        try {
            long start = System.currentTimeMillis();
            var chatResp = chatClient.prompt().user(prompt).call().chatResponse();
            String answer = chatResp.getResult().getOutput().getContent();
            long latency = System.currentTimeMillis() - start;

            int[] tokens = extractUsage(chatResp, prompt, answer);
            traceService.recordLlmCall(ctx.getTraceId(), "FINAL_ANSWER", MODEL, 1,
                    "", prompt, answer, tokens[0], tokens[1], latency, true, null);

            log.info("LLM 最终回复生成: {}", answer);
            return answer;
        } catch (Exception e) {
            log.warn("LLM 最终回复生成失败: {}", e.getMessage());
            return plan.getSummary() != null ? plan.getSummary() : execMessage;
        }
    }

    /** 将 Skill 执行结果压缩为一行摘要 */
    private String summarizeValue(Object value) {
        if (value instanceof Map<?, ?> m) {
            Set<?> keys = m.keySet();
            if (keys.size() <= 3) return keys.toString();
            return "[" + keys.size() + " fields]";
        }
        if (value instanceof String s) return s.length() > 100 ? s.substring(0, 100) + "..." : s;
        return value != null ? value.getClass().getSimpleName() : "null";
    }

    private int[] extractUsage(org.springframework.ai.chat.model.ChatResponse resp, String in, String out) {
        try {
            var usage = resp.getMetadata().getUsage();
            if (usage != null) return new int[]{usage.getPromptTokens().intValue(), usage.getGenerationTokens().intValue()};
        } catch (Exception ignored) {}
        return new int[]{estimateTokens(in), estimateTokens(out)};
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        int chars = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) chars++; else other++;
        }
        return (int) (chars / 1.5 + other / 4.0);
    }

    /** 带回调的流式对话（SSE 用） */
    public Map<String, Object> chatStream(Long userId, String username, String role,
                                           String conversationId, String userInput,
                                           String orderNo, String afterSalesNo,
                                           StreamCallback callback) {
        long startTime = System.currentTimeMillis();
        AgentTrace trace = traceService.createTrace(userId, conversationId, userInput);

        AgentContext ctx = new AgentContext();
        ctx.setTraceId(trace.getTraceId());
        ctx.setUserId(userId); ctx.setUsername(username); ctx.setRole(role);
        ctx.setConversationId(conversationId); ctx.setUserInput(userInput);
        ctx.setOrderNo(orderNo); ctx.setAfterSalesNo(afterSalesNo);

        try {
            callback.onTrace(trace.getTraceId());

            var intentResult = intentRouter.route(ctx);
            ctx.setIntent(intentResult.intent());
            callback.onThought("意图识别: " + intentResult.intent() + " (置信度:" +
                    String.format("%.2f", intentResult.confidence()) + ")");

            var loopResult = loopService.run(ctx, intentResult.intent(),
                    intentResult.confidence(), intentResult.entities());

            if (loopResult.escalate()) {
                callback.onThought("转人工: " + loopResult.escalateReason());
                traceService.completeTrace(trace.getTraceId(), intentResult.intent(), "LOW",
                        "转人工: " + loopResult.escalateReason(), "ESCALATED", null,
                        System.currentTimeMillis() - startTime);
                callback.onDone(trace.getTraceId());
                return Map.of("traceId", trace.getTraceId(), "intent", "ESCALATED",
                        "answer", "已转接人工客服处理", "escalated", true);
            }

            AgentPlan plan = loopResult.plan();
            callback.onThought("执行计划: " + plan.getSummary());

            var execResult = orchestrationEngine.execute(plan, ctx);
            for (var step : execResult.stepResults()) {
                callback.onTool(step.skill(), step.success() ? "SUCCESS" : "FAILED");
            }

            var assessment = riskGuard.assessQuick(plan, intentResult.confidence());

            if (!assessment.canAutoExecute() || execResult.hasConfirmRequired()) {
                String confirmToken = confirmService.generateToken(trace.getTraceId(),
                        plan.getIntent(), Map.of("summary", plan.getSummary()), 15, userId);
                callback.onConfirm(confirmToken, plan.getIntent());
                traceService.completeTrace(trace.getTraceId(), intentResult.intent(), "HIGH",
                        plan.getSummary(), "WAIT_CONFIRM", null, System.currentTimeMillis() - startTime);
                callback.onDone(trace.getTraceId());
                return Map.of("traceId", trace.getTraceId(), "intent", intentResult.intent(),
                        "riskLevel", "HIGH", "answer", plan.getSummary(), "requiresConfirmation", true,
                        "confirmToken", confirmToken);
            }

            String answer = generateFinalAnswer(ctx, intentResult.intent(), plan, execResult.message());
            callback.onDelta(answer);
            traceService.completeTrace(trace.getTraceId(), intentResult.intent(), "LOW",
                    answer, "SUCCESS", null, System.currentTimeMillis() - startTime);
            callback.onDone(trace.getTraceId());
            return Map.of("traceId", trace.getTraceId(), "intent", intentResult.intent(),
                    "riskLevel", "LOW", "answer", answer, "requiresConfirmation", false);

        } catch (Exception e) {
            log.error("Agent 异常 traceId={}", trace.getTraceId(), e);
            callback.onError(e.getMessage());
            callback.onDone(trace.getTraceId());
            traceService.completeTrace(trace.getTraceId(), "UNKNOWN", "LOW", null,
                    "ERROR", e.getMessage(), System.currentTimeMillis() - startTime);
            return Map.of("traceId", trace.getTraceId(), "answer", "处理异常",
                    "error", e.getMessage(), "requiresConfirmation", false);
        }
    }

    /** SSE 流式回调接口 */
    public interface StreamCallback {
        void onTrace(String traceId);
        void onThought(String content);
        void onTool(String toolName, String status);
        void onDelta(String content);
        void onConfirm(String confirmToken, String actionType);
        void onError(String message);
        void onDone(String traceId);
    }

    public AgentTraceService getTraceService() { return traceService; }
    public AgentConfirmService getConfirmService() { return confirmService; }
}
