package com.aftersales.auth.config;

import com.aftersales.auth.interceptor.AuthInterceptor;
import com.aftersales.auth.service.TokenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * 注册认证拦截器，拦截所有请求（除 /api/auth/login）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenService tokenService;

    public WebMvcConfig(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(tokenService))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }
}
