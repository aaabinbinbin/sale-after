package com.aftersales.biz.rule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RuleResult {

    public enum FailureMode { BLOCK, WARN }

    private final boolean passed;
    private final String ruleName;
    private final String reason;
    private final FailureMode failureMode;

    public static RuleResult pass(String ruleName) {
        return new RuleResult(true, ruleName, null, null);
    }

    public static RuleResult pass(String ruleName, String reason) {
        return new RuleResult(true, ruleName, reason, null);
    }

    public static RuleResult block(String ruleName, String reason) {
        return new RuleResult(false, ruleName, reason, FailureMode.BLOCK);
    }

    public static RuleResult warn(String ruleName, String reason) {
        return new RuleResult(false, ruleName, reason, FailureMode.WARN);
    }

    public boolean isBlock() { return !passed && failureMode == FailureMode.BLOCK; }
    public boolean isWarn() { return !passed && failureMode == FailureMode.WARN; }
}
