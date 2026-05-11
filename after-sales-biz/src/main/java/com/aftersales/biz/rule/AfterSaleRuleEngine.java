package com.aftersales.biz.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 售后规则引擎。
 *
 * 收集所有 AfterSaleRule 实现，按优先级链式执行。
 * BLOCK 模式失败立即终止，WARN 模式失败继续但收集在结果中。
 *
 * Agent 通过此引擎获取决策结果，而非自己推理业务规则。
 */
@Component
public class AfterSaleRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleRuleEngine.class);

    private final List<AfterSaleRule> rules;

    /**
     * Spring 自动注入所有 AfterSaleRule 实现，按优先级排序。
     */
    public AfterSaleRuleEngine(List<AfterSaleRule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(AfterSaleRule::getPriority))
                .toList();
        log.info("RuleEngine 加载 {} 条规则: {}", this.rules.size(),
                this.rules.stream().map(r -> r.getName() + "(p=" + r.getPriority() + ")").toList());
    }

    /**
     * 评估所有规则。
     *
     * @return 规则引擎汇总结果
     */
    public RuleEngineResult evaluate(AfterSaleRuleContext context) {
        List<RuleResult> results = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean blocked = false;

        for (AfterSaleRule rule : rules) {
            RuleResult result = rule.evaluate(context);
            results.add(result);

            if (result.isPassed()) {
                log.debug("规则 {} 通过", rule.getName());
            } else if (result.isBlock()) {
                log.warn("规则 {} 阻断: {}", rule.getName(), result.getReason());
                blocked = true;
                break;
            } else if (result.isWarn()) {
                log.warn("规则 {} 警告: {}", rule.getName(), result.getReason());
                warnings.add(rule.getName() + ": " + result.getReason());
            }
        }

        boolean allPassed = !blocked;
        return new RuleEngineResult(allPassed, blocked, results, warnings);
    }

    /**
     * 快速评估：遇到第一个 BLOCK 即返回，不收集警告。
     */
    public boolean evaluateQuick(AfterSaleRuleContext context) {
        for (AfterSaleRule rule : rules) {
            RuleResult result = rule.evaluate(context);
            if (result.isBlock()) return false;
        }
        return true;
    }

    /** 获取所有已注册规则（用于动态展示） */
    public List<AfterSaleRule> getRules() { return rules; }

    /**
     * 规则引擎汇总结果。
     */
    public record RuleEngineResult(
            boolean allPassed,
            boolean blocked,
            List<RuleResult> results,
            List<String> warnings
    ) {
        public String getSummary() {
            if (allPassed) return "全部规则通过 (" + results.size() + " 条)";
            if (blocked) return "规则阻断: " + results.stream()
                    .filter(r -> !r.isPassed())
                    .map(r -> r.getRuleName() + "=" + r.getReason())
                    .findFirst().orElse("未知");
            return "规则警告: " + String.join("; ", warnings);
        }
    }
}
