package com.aftersales.api.controller;

import com.aftersales.auth.interceptor.NoAuth;
import com.aftersales.auth.service.AuthService;
import com.aftersales.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口控制器。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录。
     */
    @NoAuth
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Map<String, Object> data = authService.login(username, password);
        return Result.ok(data);
    }

    /**
     * 用户登出。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = com.aftersales.auth.service.TokenService.extractToken(header);
        authService.logout(token);
        return Result.ok();
    }

    /**
     * 获取当前登录用户信息。
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = com.aftersales.auth.service.TokenService.extractToken(header);
        Map<String, Object> data = authService.me(token);
        return Result.ok(data);
    }
}
