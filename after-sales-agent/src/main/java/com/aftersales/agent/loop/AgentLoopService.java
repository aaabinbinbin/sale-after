package com.aftersales.agent.loop;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.loader.SkillLoader;
import com.aftersales.agent.orchestrator.ExecutionMode;
import com.aftersales.agent.planner.AgentPlan;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.agent.trace.AgentTraceService;
import com.aftersales.agent.registry.SkillRegistry;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent Loop 服务（ReAct 模式）。
 *
 * LLM 在循环中逐轮决策：
 * - FETCH_DATA  → 调 Skill 获取数据 → 结果注入上下文 → 下一轮
 * - GENERATE_PLAN → 生成执行计划 → 跳出循环
 * - ESCALATE     → 转人工
 *
 * 最多 3 轮，超限强制转人工。
 */
@Component
public class AgentLoopService {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopService.class);
    private static final int MAX_ROUNDS = 3;

    private static final String MODEL = "qwen3.6-flash";
    private final SkillRegistry skillRegistry;
    private final SkillLoader skillLoader;
    private final ChatClient chatClient;
    private final AgentTraceService traceService;

    /** Agent Loop 的 system prompt */
    private static final String LOOP_SYSTEM_PROMPT = """
            你是智能售后系统的决策引擎。根据当前上下文，决定下一步动作。

            可用动作（action字段）：
            - FETCH_DATA：需要更多信息，调用指定的skills获取数据
            - GENERATE_PLAN：信息足够，生成执行计划
            - ESCALATE：无法处理，转人工

            执行计划中的 executionMode：
            - SEQUENTIAL：串行执行
            - PARALLEL：并发执行
            - CONFIRM_REQUIRED：暂停等人工确认（仅HIGH风险skill）

            输出格式：
            {"action":"FETCH_DATA","skills":["order.query"],"reason":"需要查订单"}
            {"action":"GENERATE_PLAN","plan":{"summary":"...","steps":[{"skill":"...","mode":"SEQUENTIAL"}]}}
            {"action":"ESCALATE","reason":"无法处理，需要人工介入"}
            只输出JSON，不要其他内容。""";

    public AgentLoopService(SkillRegistry skillRegistry, SkillLoader skillLoader,
                             ChatClient chatClient, AgentTraceService traceService) {
        this.skillRegistry = skillRegistry;
        this.skillLoader = skillLoader;
        this.chatClient = chatClient;
        this.traceService = traceService;
    }

    /** 运行 Agent Loop */
    public LoopResult run(AgentContext ctx, String intent, double confidence, Map<String, Object> entities) {
        if (entities.containsKey("orderNo")) {
            ctx.setOrderNo((String) entities.get("orderNo"));
        }
        if (entities.containsKey("afterSalesNo")) {
            ctx.setAfterSalesNo((String) entities.get("afterSalesNo"));
        }

        List<SkillDefinition> matchedSkills = skillRegistry.match(intent, ctx.getUserInput());

        for (int round = 1; round <= MAX_ROUNDS; round++) {
            log.info("Agent Loop 第 {} 轮, intent={}", round, intent);
            LoopDecision decision = decide(ctx, intent, matchedSkills, round);

            switch (decision.action) {
                case FETCH_DATA -> {
                    for (String skillName : decision.skillsToFetch) {
                        ctx.getContextData().put(skillName, "fetched");
                        ctx.putExtra("lastFetchRound", round);
                    }
                }
                case GENERATE_PLAN -> {
                    AgentPlan plan = generatePlan(ctx, intent, matchedSkills);
                    return LoopResult.plan(plan, round);
                }
                case ESCALATE -> {
                    log.info("Agent 主动转人工 round={} reason={}", round, decision.reason);
                    return LoopResult.escalate(decision.reason, ctx, round);
                }
            }
        }

        log.warn("Agent Loop 超 {} 轮，强制转人工", MAX_ROUNDS);
        return LoopResult.escalate("Agent 在 " + MAX_ROUNDS + " 轮内无法完成", ctx, MAX_ROUNDS);
    }

    /** LLM 决策：下一步动作 */
    LoopDecision decide(AgentContext ctx, String intent, List<SkillDefinition> skills, int round) {
        try {
            String skillList = skills.stream()
                    .map(s -> s.getName() + ": " + s.getDescription())
                    .collect(Collectors.joining("\n"));

            String userPrompt = String.format("""
                    用户输入: %s
                    意图: %s (置信度: %.2f)
                    当前轮次: %d/%d
                    已获取数据: %s
                    可用Skill列表:
                    %s
                    """,
                    ctx.getUserInput(), intent, 0.8, round, MAX_ROUNDS,
                    ctx.getContextData().isEmpty() ? "无" : String.join(",", ctx.getContextData().keySet()),
                    skillList);

            long start = System.currentTimeMillis();
            var chatResp = chatClient.prompt()
                    .system(LOOP_SYSTEM_PROMPT).user(userPrompt).call().chatResponse();
            String llmResponse = chatResp.getResult().getOutput().getContent();
            long latency = System.currentTimeMillis() - start;

            // 记录 LLM 调用（优先读 API 返回的 usage，降级估算）
            int[] tokens = extractUsage(chatResp, LOOP_SYSTEM_PROMPT + userPrompt, llmResponse);
            traceService.recordLlmCall(ctx.getTraceId(), "LOOP_DECISION", MODEL, round,
                    LOOP_SYSTEM_PROMPT, userPrompt, llmResponse,
                    tokens[0], tokens[1], latency, true, null);

            log.info("LLM 决策响应 round={}: {}", round, llmResponse);
            return parseDecision(llmResponse);

        } catch (Exception e) {
            log.warn("LLM 决策失败 round={}: {}", round, e.getMessage());
            // 降级
            if (round == 1 && ctx.getContextData().isEmpty()) {
                return LoopDecision.fetchData(skills.stream().map(SkillDefinition::getName).toList());
            }
            return LoopDecision.generatePlan();
        }
    }

    /** 解析 LLM 决策 JSON */
    @SuppressWarnings("unchecked")
    private LoopDecision parseDecision(String json) {
        try {
            String jsonStr = json.trim().replaceAll("```json|```", "").trim();
            Map<String, Object> map = JsonUtils.fromJson(jsonStr, Map.class);
            String action = (String) map.get("action");

            if ("FETCH_DATA".equals(action)) {
                List<String> skillList = (List<String>) map.get("skills");
                return LoopDecision.fetchData(skillList != null ? skillList : List.of());
            } else if ("GENERATE_PLAN".equals(action)) {
                return LoopDecision.generatePlan();
            } else if ("ESCALATE".equals(action)) {
                return LoopDecision.escalate((String) map.getOrDefault("reason", "Agent判定需转人工"));
            }
        } catch (Exception e) {
            log.warn("LLM 决策解析失败: {}", e.getMessage());
        }
        return LoopDecision.generatePlan(); // 降级
    }

    /** LLM 生成执行计划 */
    AgentPlan generatePlan(AgentContext ctx, String intent, List<SkillDefinition> skills) {
        String planSystemPrompt = "你是计划生成器。根据意图和可用Skill，生成执行计划。只输出JSON。";
        try {
            String skillList = skills.stream()
                    .map(s -> String.format("- %s (risk:%s): %s", s.getName(), s.getRiskLevel(), s.getDescription()))
                    .collect(Collectors.joining("\n"));

            String userPrompt = String.format("""
                    意图: %s | 用户输入: %s
                    可用Skill:
                    %s
                    已获取上下文: %s
                    请生成执行计划。HIGH风险skill用CONFIRM_REQUIRED模式，其余用SEQUENTIAL。
                    """, intent, ctx.getUserInput(), skillList,
                    ctx.getContextData().isEmpty() ? "无" : ctx.getContextData().keySet());

            long start = System.currentTimeMillis();
            var chatResp = chatClient.prompt()
                    .system(planSystemPrompt).user(userPrompt).call().chatResponse();
            String llmResponse = chatResp.getResult().getOutput().getContent();
            long latency = System.currentTimeMillis() - start;

            int[] tokens = extractUsage(chatResp, planSystemPrompt + userPrompt, llmResponse);
            traceService.recordLlmCall(ctx.getTraceId(), "PLAN_GENERATION", MODEL,
                    ctx.getExtra("lastFetchRound") instanceof Integer r ? r : 1,
                    planSystemPrompt, userPrompt, llmResponse,
                    tokens[0], tokens[1], latency, true, null);

            log.info("LLM 计划生成: {}", llmResponse);
            return parsePlan(llmResponse, intent, skills);

        } catch (Exception e) {
            log.warn("LLM 计划生成失败: {}", e.getMessage());
            return buildDefaultPlan(intent, skills);
        }
    }

    /** 从 API 返回提取真实 token，不可用时降级估算 */
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

    /** 解析 LLM 计划 JSON */
    @SuppressWarnings("unchecked")
    private AgentPlan parsePlan(String json, String intent, List<SkillDefinition> skills) {
        AgentPlan plan = new AgentPlan();
        plan.setPlanId("plan-" + IdGenerator.genTraceId());
        plan.setIntent(intent);

        try {
            String jsonStr = json.trim().replaceAll("```json|```", "").trim();
            Map<String, Object> map = JsonUtils.fromJson(jsonStr, Map.class);
            Object planObj = map.get("plan");
            if (planObj instanceof Map) {
                Map<String, Object> planMap = (Map<String, Object>) planObj;
                plan.setSummary((String) planMap.getOrDefault("summary", "执行计划"));

                Object stepsObj = planMap.get("steps");
                if (stepsObj instanceof List) {
                    List<Map<String, Object>> steps = (List<Map<String, Object>>) stepsObj;
                    int stepNo = 1;
                    for (Map<String, Object> stepMap : steps) {
                        String skillName = (String) stepMap.get("skill");
                        String modeStr = (String) stepMap.getOrDefault("mode", "SEQUENTIAL");

                        // 白名单校验
                        if (skillName == null || skillLoader.allSkills().stream()
                                .noneMatch(s -> s.getName().equals(skillName))) continue;

                        ExecutionMode mode;
                        try { mode = ExecutionMode.valueOf(modeStr); }
                        catch (Exception e) { mode = ExecutionMode.SEQUENTIAL; }

                        AgentPlanStep step = new AgentPlanStep(stepNo++, skillName, mode);
                        plan.getSteps().add(step);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LLM 计划解析失败: {}", e.getMessage());
            plan.setSummary("解析失败，使用默认计划");
        }

        if (plan.getSteps().isEmpty()) {
            plan = buildDefaultPlan(intent, skills);
            plan.setPlanId("plan-" + IdGenerator.genTraceId());
        }
        return plan;
    }

    /** 默认计划（LLM 不可用时的降级） */
    private AgentPlan buildDefaultPlan(String intent, List<SkillDefinition> skills) {
        AgentPlan plan = new AgentPlan();
        plan.setPlanId("plan-" + IdGenerator.genTraceId());
        plan.setIntent(intent);
        plan.setSummary("默认执行计划 (intent=" + intent + ")");

        int stepNo = 1;
        for (SkillDefinition skill : skills) {
            ExecutionMode mode = "HIGH".equals(skill.getRiskLevel())
                    ? ExecutionMode.CONFIRM_REQUIRED : ExecutionMode.SEQUENTIAL;
            plan.getSteps().add(new AgentPlanStep(stepNo++, skill.getName(), mode));
        }
        return plan;
    }

    // ====== 辅助类 ======

    static class LoopDecision {
        enum Action { FETCH_DATA, GENERATE_PLAN, ESCALATE }
        Action action;
        List<String> skillsToFetch;
        String reason;

        static LoopDecision fetchData(List<String> skills) {
            LoopDecision d = new LoopDecision();
            d.action = Action.FETCH_DATA;
            d.skillsToFetch = skills;
            return d;
        }
        static LoopDecision generatePlan() {
            LoopDecision d = new LoopDecision();
            d.action = Action.GENERATE_PLAN;
            return d;
        }
        static LoopDecision escalate(String reason) {
            LoopDecision d = new LoopDecision();
            d.action = Action.ESCALATE;
            d.reason = reason;
            return d;
        }
    }

    public record LoopResult(boolean hasPlan, AgentPlan plan, boolean escalate,
                              String escalateReason, Map<String, Object> contextSnapshot, int rounds) {
        public static LoopResult plan(AgentPlan plan, int rounds) {
            return new LoopResult(true, plan, false, null, Map.of(), rounds);
        }
        public static LoopResult escalate(String reason, AgentContext ctx, int rounds) {
            Map<String, Object> snapshot = new LinkedHashMap<>(ctx.getContextData());
            snapshot.put("userInput", ctx.getUserInput());
            return new LoopResult(false, null, true, reason, snapshot, rounds);
        }
    }
}
