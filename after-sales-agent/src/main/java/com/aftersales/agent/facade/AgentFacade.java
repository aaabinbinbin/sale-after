package com.aftersales.agent.facade;

import com.aftersales.agent.confirm.AgentConfirmService;
import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.agent.planner.AgentPlanner;
import com.aftersales.agent.risk.AgentRiskGuard;
import com.aftersales.agent.router.AgentIntentRouter;
import com.aftersales.agent.skill.AgentSkill;
import com.aftersales.agent.skill.SkillRegistry;
import com.aftersales.agent.trace.AgentTraceService;
import com.aftersales.common.enums.AgentRiskLevel;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.infra.entity.AgentTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Agent 主入口 Facade。
 *
 * 负责创建 Trace、构建上下文、意图识别、技能编排、风险判断、确认生成和 Trace 记录。
 */
@Service
public class AgentFacade {

    private static final Logger log = LoggerFactory.getLogger(AgentFacade.class);

    private final AgentIntentRouter intentRouter;
    private final AgentPlanner planner;
    private final SkillRegistry skillRegistry;
    private final AgentRiskGuard riskGuard;
    private final AgentConfirmService confirmService;
    private final AgentTraceService traceService;

    public AgentFacade(AgentIntentRouter intentRouter, AgentPlanner planner,
                        SkillRegistry skillRegistry, AgentRiskGuard riskGuard,
                        AgentConfirmService confirmService, AgentTraceService traceService) {
        this.intentRouter = intentRouter;
        this.planner = planner;
        this.skillRegistry = skillRegistry;
        this.riskGuard = riskGuard;
        this.confirmService = confirmService;
        this.traceService = traceService;
    }

    /**
     * 处理 Agent 对话（非流式）。
     *
     * @param userId          用户ID
     * @param username        用户名
     * @param role            角色
     * @param conversationId  会话ID
     * @param userInput       用户输入
     * @param orderNo         关联订单号（可选）
     * @param afterSalesNo    关联售后单号（可选）
     * @return 对话结果
     */
    public Map<String, Object> chat(Long userId, String username, String role,
                                     String conversationId, String userInput,
                                     String orderNo, String afterSalesNo) {
        long startTime = System.currentTimeMillis();

        // 1. 创建 Trace
        AgentTrace trace = traceService.createTrace(userId, conversationId, userInput);

        // 2. 构建上下文
        AgentContext ctx = buildContext(trace.getTraceId(), userId, username, role,
                conversationId, userInput, orderNo, afterSalesNo);

        try {
            // 3. 意图识别
            var intentResult = intentRouter.route(ctx);
            ctx.setIntent(intentResult.intent());
            log.info("Agent 意图识别 traceId={} intent={} confidence={}", trace.getTraceId(),
                    intentResult.intent(), intentResult.confidence());

            // 4. 构建计划
            var plan = planner.buildPlan(ctx, intentResult.intent(), intentResult.needRag(), intentResult.needTool());

            // 5. 执行技能
            List<Map<String, Object>> stepResults = new ArrayList<>();
            for (AgentPlanStep step : plan.getSteps()) {
                AgentSkill skill = skillRegistry.get(step.getSkill());
                var result = skill.execute(ctx, step);
                stepResults.add(Map.of("skill", step.getSkill(), "success", result.success(), "data", result.data()));
                traceService.recordToolCall(trace.getTraceId(), step.getSkill(),
                        JsonUtils.toJson(ctx), result.success(), result.error());
            }

            // 6. 风险评估
            AgentRiskLevel riskLevel = riskGuard.assess(intentResult.intent());

            // 7. 生成回答
            String answer = buildAnswer(intentResult.intent(), stepResults, riskLevel);
            String confirmToken = null;

            // 8. 高风险动作生成 confirmToken
            if (riskLevel.requiresConfirmation()) {
                confirmToken = confirmService.generateToken(trace.getTraceId(),
                        mapIntentToAction(intentResult.intent()), buildConfirmPayload(ctx), 15);
            }

            // 9. 完成 Trace
            long latency = System.currentTimeMillis() - startTime;
            traceService.completeTrace(trace.getTraceId(), intentResult.intent(), riskLevel.getCode(),
                    answer, "SUCCESS", null, latency);

            // 10. 构建返回
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("traceId", trace.getTraceId());
            result.put("intent", intentResult.intent());
            result.put("riskLevel", riskLevel.getCode());
            result.put("answer", answer);
            result.put("requiresConfirmation", riskLevel.requiresConfirmation());
            if (confirmToken != null) {
                result.put("confirmToken", confirmToken);
                result.put("suggestedAction", Map.of("actionType", mapIntentToAction(intentResult.intent())));
            }
            return result;
        } catch (Exception e) {
            log.error("Agent 处理异常 traceId={}", trace.getTraceId(), e);
            long latency = System.currentTimeMillis() - startTime;
            traceService.completeTrace(trace.getTraceId(), "UNKNOWN", "LOW", null, "ERROR", e.getMessage(), latency);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("traceId", trace.getTraceId());
            result.put("intent", "UNKNOWN");
            result.put("answer", "抱歉，处理你的请求时出现了问题，请稍后重试。");
            result.put("requiresConfirmation", false);
            return result;
        }
    }

    private AgentContext buildContext(String traceId, Long userId, String username, String role,
                                       String conversationId, String userInput, String orderNo, String afterSalesNo) {
        AgentContext ctx = new AgentContext();
        ctx.setTraceId(traceId);
        ctx.setUserId(userId);
        ctx.setUsername(username);
        ctx.setRole(role);
        ctx.setConversationId(conversationId);
        ctx.setUserInput(userInput);
        ctx.setOrderNo(orderNo);
        ctx.setAfterSalesNo(afterSalesNo);
        return ctx;
    }

    private String buildAnswer(String intent, List<Map<String, Object>> stepResults, AgentRiskLevel riskLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据分析，");

        for (Map<String, Object> sr : stepResults) {
            if (Boolean.TRUE.equals(sr.get("success"))) {
                sb.append("已").append(sr.get("skill")).append("。");
            }
        }

        if (riskLevel.requiresConfirmation()) {
            sb.append("该操作属于高风险动作，请确认后执行。");
        }
        return sb.toString();
    }

    private String mapIntentToAction(String intent) {
        return switch (intent) {
            case "CREATE_AFTER_SALES_APPLICATION" -> "CREATE_AFTER_SALES_APPLICATION";
            case "EXECUTE_REFUND" -> "EXECUTE_REFUND";
            case "GRANT_COMPENSATION" -> "GRANT_COMPENSATION";
            default -> intent;
        };
    }

    private Map<String, Object> buildConfirmPayload(AgentContext ctx) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", ctx.getOrderNo());
        payload.put("afterSalesNo", ctx.getAfterSalesNo());
        return payload;
    }

    /** Getter for traceService */
    public AgentTraceService getTraceService() { return traceService; }

    /** Getter for confirmService */
    public AgentConfirmService getConfirmService() { return confirmService; }
}
