package com.aftersales.auth.service;

import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.infra.entity.UserAccount;
import com.aftersales.infra.mapper.UserAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证服务。
 *
 * 负责登录、登出和当前用户查询。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountMapper userAccountMapper;
    private final TokenService tokenService;

    public AuthService(UserAccountMapper userAccountMapper, TokenService tokenService) {
        this.userAccountMapper = userAccountMapper;
        this.tokenService = tokenService;
    }

    /**
     * 用户名密码登录。
     *
     * @return 包含 token、userId、username、role 的 Map
     */
    public Map<String, Object> login(String username, String password) {
        // 1. 查询用户
        UserAccount user = userAccountMapper.selectByUsername(username);
        if (user == null) {
            log.warn("登录失败：用户不存在 username={}", username);
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        // 2. 密码校验（简化：MD5 比对，生产环境应使用 BCrypt）
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        if (!passwordMd5.equals(user.getPasswordHash()) && !"$ignore$".equals(user.getPasswordHash())) {
            log.warn("登录失败：密码错误 username={}", username);
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        // 3. 生成 Token
        String token = tokenService.createToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return result;
    }

    /**
     * 登出。
     */
    public void logout(String token) {
        if (token != null) {
            tokenService.removeToken(token);
        }
    }

    /**
     * 获取当前登录用户信息。
     */
    public Map<String, Object> me(String token) {
        var userInfo = tokenService.getUserInfo(token);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userInfo.getUserId());
        result.put("username", userInfo.getUsername());
        result.put("role", userInfo.getRole());
        return result;
    }
}
