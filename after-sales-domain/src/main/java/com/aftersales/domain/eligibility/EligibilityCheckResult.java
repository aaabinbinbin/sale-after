package com.aftersales.domain.eligibility;

/**
 * 售后资格校验结果。
 *
 * 结构化返回校验成功/失败及失败原因。
 */
public class EligibilityCheckResult {

    private final boolean passed;
    private final String errorCode;
    private final String errorMessage;

    private EligibilityCheckResult(boolean passed, String errorCode, String errorMessage) {
        this.passed = passed;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /** 校验通过 */
    public static EligibilityCheckResult pass() {
        return new EligibilityCheckResult(true, null, null);
    }

    /** 校验失败 */
    public static EligibilityCheckResult fail(String errorCode, String errorMessage) {
        return new EligibilityCheckResult(false, errorCode, errorMessage);
    }

    public boolean isPassed() { return passed; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public String toString() {
        return passed ? "PASS" : "FAIL[" + errorCode + "]: " + errorMessage;
    }
}
