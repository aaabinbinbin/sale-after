package com.aftersales.agent.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PolicyResult {

    public enum Verdict { ALLOW, DENY, CONFIRM_REQUIRED }

    private final Verdict verdict;
    private final String policyName;
    private final String reason;

    public static PolicyResult allow(String policyName) {
        return new PolicyResult(Verdict.ALLOW, policyName, null);
    }

    public static PolicyResult allow(String policyName, String reason) {
        return new PolicyResult(Verdict.ALLOW, policyName, reason);
    }

    public static PolicyResult deny(String policyName, String reason) {
        return new PolicyResult(Verdict.DENY, policyName, reason);
    }

    public static PolicyResult confirmRequired(String policyName, String reason) {
        return new PolicyResult(Verdict.CONFIRM_REQUIRED, policyName, reason);
    }

    public boolean isAllowed() { return verdict == Verdict.ALLOW; }
    public boolean isDenied() { return verdict == Verdict.DENY; }
    public boolean isConfirmRequired() { return verdict == Verdict.CONFIRM_REQUIRED; }
}
