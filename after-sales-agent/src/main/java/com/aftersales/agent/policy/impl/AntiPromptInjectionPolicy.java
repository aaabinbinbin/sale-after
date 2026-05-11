package com.aftersales.agent.policy.impl;

import com.aftersales.agent.policy.Policy;
import com.aftersales.agent.policy.PolicyContext;
import com.aftersales.agent.policy.PolicyResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 防 Prompt 注入策略：检测 Tool 参数中是否含注入攻击。
 *
 * 检查以下模式：
 * - 包含 "ignore previous"、"system prompt" 等注入关键词
 * - 参数中出现 LLM 指令格式
 * - 参数长度异常（超过合理范围）
 */
@Component
public class AntiPromptInjectionPolicy implements Policy {

    /** 注入关键词 */
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "ignore\\s+(previous|all|above|your|the)\\s+(instructions?|rules?|prompts?|constraints?)" +
            "|system\\s*prompt" +
            "|<\\|im_start\\|>" +
            "|<\\|im_end\\|>" +
            "|</?system>" +
            "|DAN\\s*mode" +
            "|jailbreak",
            Pattern.CASE_INSENSITIVE
    );

    /** 参数值最大长度 */
    private static final int MAX_PARAM_LENGTH = 5000;

    @Override
    public String getName() { return "AntiPromptInjectionPolicy"; }

    @Override
    public int getPriority() { return 1; } // 与权限检查同级，最先执行

    @Override
    public PolicyResult check(PolicyContext context) {
        Map<String, Object> params = context.getToolParams();
        if (params == null || params.isEmpty()) {
            return PolicyResult.allow(getName());
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue().toString() : "";

            // 长度检查
            if (value.length() > MAX_PARAM_LENGTH) {
                return PolicyResult.deny(getName(),
                        "参数 [" + entry.getKey() + "] 长度异常(" + value.length() + " > " + MAX_PARAM_LENGTH + ")");
            }

            // 注入关键词检查
            if (INJECTION_PATTERN.matcher(value).find()) {
                return PolicyResult.deny(getName(),
                        "参数 [" + entry.getKey() + "] 包含疑似注入内容");
            }
        }

        return PolicyResult.allow(getName());
    }
}
