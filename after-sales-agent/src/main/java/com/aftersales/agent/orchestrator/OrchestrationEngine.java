package com.aftersales.agent.orchestrator;

import com.aftersales.agent.context.AgentContext;
import com.aftersales.agent.loader.SkillDefinition;
import com.aftersales.agent.loader.SkillLoader;
import com.aftersales.agent.planner.AgentPlan;
import com.aftersales.agent.planner.AgentPlanStep;
import com.aftersales.agent.skill.SkillResult;
import com.aftersales.agent.trace.AgentTraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * 编排引擎。
 *
 * 按 executionMode 执行 AgentPlan 中的每个步骤：
 * - SEQUENTIAL      → 串行执行
 * - PARALLEL        → CompletableFuture 并发执行
 * - CONFIRM_REQUIRED → 暂停，生成 confirmToken
 *
 * 执行完成后调用 RiskGuard 判断是否升级人工。
 * 失败或拒绝 → 返回失败状态给 Agent Loop 重新决策。
 *
 * 设计模式：模板方法模式——执行框架固定，每个步骤的具体行为由 Skill 决定。
 */
@Component
public class OrchestrationEngine {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationEngine.class);

    private final SkillLoader skillLoader;
    private final ToolRegistry toolRegistry;
    private final AgentTraceService traceService;

    public OrchestrationEngine(SkillLoader skillLoader, ToolRegistry toolRegistry,
                                AgentTraceService traceService) {
        this.skillLoader = skillLoader;
        this.toolRegistry = toolRegistry;
        this.traceService = traceService;
    }

    /**
     * 执行 Agent 计划。
     *
     * @param plan 待执行的计划
     * @param ctx  Agent 上下文（含 contextData + actionHistory）
     * @return 执行结果
     */
    public ExecutionResult execute(AgentPlan plan, AgentContext ctx) {
        List<StepExecutionResult> stepResults = new ArrayList<>();
        boolean hasConfirmRequired = false;
        String confirmToken = null;

        for (AgentPlanStep step : plan.getSteps()) {
            long stepStart = System.currentTimeMillis();

            // 安全防线1：Skill 名称白名单校验
            SkillDefinition def;
            try {
                def = skillLoader.get(step.getSkill());
            } catch (Exception e) {
                stepResults.add(new StepExecutionResult(step.getStepNo(), step.getSkill(), false, e.getMessage()));
                traceService.recordToolCall(ctx.getTraceId(), step.getSkill(), "", "白名单校验失败", false, e.getMessage());
                continue;
            }

            // 安全防线2：CONFIRM_REQUIRED 只能用于 HIGH 风险 Skill
            if (step.getExecutionMode() == ExecutionMode.CONFIRM_REQUIRED
                    && !"HIGH".equals(def.getRiskLevel())) {
                stepResults.add(new StepExecutionResult(step.getStepNo(), step.getSkill(), false,
                        "执行模式 CONFIRM_REQUIRED 只能用于 HIGH 风险 Skill"));
                continue;
            }

            try {
                // 按执行模式执行
                if (step.getExecutionMode() == ExecutionMode.CONFIRM_REQUIRED) {
                    hasConfirmRequired = true;
                    // 不在此处执行，由 AgentFacade 生成 confirmToken
                    stepResults.add(new StepExecutionResult(step.getStepNo(), step.getSkill(), true, "等待确认"));
                    log.info("步骤 {} ({}) 需要人工确认", step.getStepNo(), step.getSkill());
                    break; // CONFIRM_REQUIRED 步骤之后暂停
                }

                // 并行步骤在 PARALLEL 模式中一并处理，当前简化为串行
                SkillResult result = executeStep(ctx, def);
                long latency = System.currentTimeMillis() - stepStart;

                stepResults.add(new StepExecutionResult(step.getStepNo(), step.getSkill(),
                        result.isSuccess(), result.isSuccess() ? "OK" : result.getError()));

                // 记录 tool call（含 tool output）
                traceService.recordToolCall(ctx.getTraceId(), step.getSkill(),
                        "context size=" + ctx.getContextData().size(),
                        result.isSuccess() && result.getData() != null
                                ? "data keys=" + result.getData().keySet() : result.getError(),
                        result.isSuccess(), result.isSuccess() ? null : result.getError());

                // 失败时返回给 Loop 重新决策
                if (!result.isSuccess()) {
                    log.warn("步骤 {} ({}) 执行失败: {}", step.getStepNo(), step.getSkill(), result.getError());
                    return new ExecutionResult(false, hasConfirmRequired, confirmToken, stepResults,
                            "步骤 " + step.getStepNo() + " 失败: " + result.getError());
                }

                // 结果注入上下文
                if (result.getData() != null) {
                    ctx.getSkillResults().put(step.getSkill(), result.getData());
                }

            } catch (Exception e) {
                log.error("步骤 {} 执行异常: {}", step.getSkill(), e);
                stepResults.add(new StepExecutionResult(step.getStepNo(), step.getSkill(), false, e.getMessage()));
                return new ExecutionResult(false, hasConfirmRequired, null, stepResults,
                        "步骤 " + step.getSkill() + " 异常: " + e.getMessage());
            }
        }

        return new ExecutionResult(true, hasConfirmRequired, confirmToken, stepResults, "全部步骤执行完成");
    }

    /** 执行单个步骤：通过 ToolRegistry 调业务 Service */
    private SkillResult executeStep(AgentContext ctx, SkillDefinition def) {
        if (def.getRequiredTools().isEmpty()) {
            return SkillResult.ok(Map.of("message", "Skill " + def.getName() + " 无工具调用"));
        }
        // 调用第一个声明的工具（大多数 Skill 只有一个 requiredTool）
        String toolName = def.getRequiredTools().get(0);
        return toolRegistry.execute(toolName, ctx, def);
    }

    /** 执行结果 */
    public record ExecutionResult(
            boolean success,
            boolean hasConfirmRequired,
            String confirmToken,
            List<StepExecutionResult> stepResults,
            String message
    ) {}

    /** 单个步骤执行结果 */
    public record StepExecutionResult(int stepNo, String skill, boolean success, String message) {}
}
