package com.aftersales.domain.eligibility;

/**
 * 售后资格校验器接口。
 *
 * 责任链模式，每个 Checker 只做一件事，返回结构化失败原因。
 */
public interface EligibilityChecker {

    /**
     * 执行校验。
     *
     * @param context 校验上下文
     * @return 校验结果，通过返回 pass()，失败返回 fail(errorCode, message)
     */
    EligibilityCheckResult check(EligibilityContext context);
}
