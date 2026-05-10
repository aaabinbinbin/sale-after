package com.aftersales.auth.interceptor;

import java.lang.annotation.*;

/**
 * 标记方法或 Controller 跳过认证拦截。
 *
 * 用于登录接口等不需要认证的端点。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoAuth {
}
