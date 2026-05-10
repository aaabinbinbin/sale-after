package com.aftersales.auth.service;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.JsonUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务。
 *
 * Token 存储在 Redis 中，key 格式：auth:token:{token}
 * value 为 JSON 格式的用户信息，设置过期时间。
 */
@Service
public class TokenService {

    private static final String TOKEN_PREFIX = "auth:token:";
    private static final long TOKEN_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;

    public TokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成 Token 并存入 Redis。
     */
    public String createToken(Long userId, String username, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        UserContext.UserInfo userInfo = new UserContext.UserInfo(userId, username, role);
        String value = JsonUtils.toJson(userInfo);
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, value, TOKEN_TTL_HOURS, TimeUnit.HOURS);
        return token;
    }

    /**
     * 根据 Token 获取用户信息。
     */
    public UserContext.UserInfo getUserInfo(String token) {
        String value = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (value == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }
        return JsonUtils.fromJson(value, UserContext.UserInfo.class);
    }

    /**
     * 删除 Token（登出）。
     */
    public void removeToken(String token) {
        redisTemplate.delete(TOKEN_PREFIX + token);
    }

    /**
     * 刷新 Token 过期时间。
     */
    public void refreshToken(String token) {
        redisTemplate.expire(TOKEN_PREFIX + token, TOKEN_TTL_HOURS, TimeUnit.HOURS);
    }

    /** 提取请求头中的 Token（Bearer token 或直接 token） */
    public static String extractToken(String header) {
        if (header == null || header.isBlank()) return null;
        if (header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }
}
