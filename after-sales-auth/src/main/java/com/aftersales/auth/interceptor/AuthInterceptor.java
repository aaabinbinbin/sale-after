package com.aftersales.auth.interceptor;

import com.aftersales.auth.service.TokenService;
import com.aftersales.common.context.UserContext;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器。
 *
 * 从请求头 Authorization 读取 Token，解析用户信息并设置 UserContext。
 * 标注 @NoAuth 的方法跳过认证。
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 非 Controller 方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 标记了 @NoAuth 的方法跳过认证
        if (handlerMethod.getMethodAnnotation(NoAuth.class) != null
                || handlerMethod.getBeanType().getAnnotation(NoAuth.class) != null) {
            return true;
        }

        // 读取 Token
        String header = request.getHeader("Authorization");
        String token = TokenService.extractToken(header);
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        // 解析用户信息并设置上下文
        try {
            UserContext.UserInfo userInfo = tokenService.getUserInfo(token);
            UserContext.set(userInfo);
            tokenService.refreshToken(token);
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "Token 已过期，请重新登录");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal 防止内存泄漏
        UserContext.clear();
    }
}
