package com.aftersales.agent.workflow.node;

import com.aftersales.agent.workflow.NodeResult;
import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowNode;

import java.util.Map;
import java.util.function.Function;

/**
 * 规则判断节点。
 *
 * 执行一个判断函数，根据返回值分支到不同后续节点。
 * 典型场景：金额判断、风险等级判断、时效判断等。
 */
public class RuleNode implements WorkflowNode {

    private final String id;
    private final String description;
    private final Function<WorkflowContext, RuleVerdict> rule;

    public RuleNode(String id, String description, Function<WorkflowContext, RuleVerdict> rule) {
        this.id = id;
        this.description = description;
        this.rule = rule;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getType() { return "RuleNode"; }

    @Override
    public NodeResult execute(WorkflowContext context) {
        RuleVerdict verdict = rule.apply(context);
        context.setVariable(id + ".verdict", verdict);
        return NodeResult.branch(verdict.name(), Map.of(
                "ruleId", id,
                "description", description,
                "verdict", verdict.name(),
                "reason", verdict.getReason()
        ));
    }

    /** 规则判决结果，作为分支标签 */
    public interface RuleVerdict {
        String name();
        String getReason();

        default RuleVerdict withReason(String reason) {
            return new SimpleVerdict(name(), reason);
        }
    }

    /** 简单判决实现 */
    private record SimpleVerdict(String name, String reason) implements RuleVerdict {
        @Override
        public String getReason() { return reason; }
    }

    /** 常用判决常量 */
    public static final class RuleVerdicts {
        private RuleVerdicts() {}

        public static final RuleVerdict PASS = new SimpleVerdict("PASS", "通过");
        public static final RuleVerdict FAIL = new SimpleVerdict("FAIL", "未通过");
        public static final RuleVerdict LOW = new SimpleVerdict("LOW", "低风险/低金额");
        public static final RuleVerdict HIGH = new SimpleVerdict("HIGH", "高风险/高金额");
        public static final RuleVerdict YES = new SimpleVerdict("YES", "是");
        public static final RuleVerdict NO = new SimpleVerdict("NO", "否");
        public static final RuleVerdict SKIP = new SimpleVerdict("SKIP", "跳过");
    }
}
