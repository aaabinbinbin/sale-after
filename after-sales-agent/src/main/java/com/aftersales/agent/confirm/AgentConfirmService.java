package com.aftersales.agent.confirm;

import com.aftersales.common.context.UserContext;
import com.aftersales.common.exception.BusinessException;
import com.aftersales.common.exception.ErrorCode;
import com.aftersales.common.util.IdGenerator;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.infra.entity.AgentConfirmAction;
import com.aftersales.infra.mapper.AgentConfirmActionMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agent 确认服务。
 *
 * 高风险动作生成 confirmToken，存储到 Redis（快速校验 + TTL）和 MySQL（审计记录）。
 */
@Service
public class AgentConfirmService {

    private static final String CONFIRM_PREFIX = "agent:confirm:";

    private final StringRedisTemplate redisTemplate;
    private final AgentConfirmActionMapper confirmActionMapper;

    public AgentConfirmService(StringRedisTemplate redisTemplate,
                                AgentConfirmActionMapper confirmActionMapper) {
        this.redisTemplate = redisTemplate;
        this.confirmActionMapper = confirmActionMapper;
    }

    /**
     * 生成确认 Token。
     *
     * @param traceId      Trace ID
     * @param actionType   动作类型
     * @param actionPayload 动作参数 JSON
     * @param ttlMinutes   过期时间（分钟）
     * @return confirmToken
     */
    public String generateToken(String traceId, String actionType, Map<String, Object> actionPayload, int ttlMinutes) {
        String token = IdGenerator.genConfirmToken();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("confirmToken", token);
        payload.put("traceId", traceId);
        payload.put("userId", UserContext.getUserId());
        payload.put("actionType", actionType);
        payload.put("payload", actionPayload);
        payload.put("expireAt", LocalDateTime.now().plusMinutes(ttlMinutes).toString());

        // 存入 Redis（带 TTL）
        String json = JsonUtils.toJson(payload);
        redisTemplate.opsForValue().set(CONFIRM_PREFIX + token, json, ttlMinutes, TimeUnit.MINUTES);

        // 存入 MySQL（审计记录）
        AgentConfirmAction action = new AgentConfirmAction();
        action.setConfirmToken(token);
        action.setTraceId(traceId);
        action.setUserId(UserContext.getUserId());
        action.setActionType(actionType);
        action.setActionPayload(JsonUtils.toJson(actionPayload));
        action.setStatus("WAIT_CONFIRM");
        action.setExpireAt(LocalDateTime.now().plusMinutes(ttlMinutes));
        confirmActionMapper.insert(action);

        return token;
    }

    /**
     * 验证并获取确认动作。
     */
    public Map<String, Object> validateAndGet(String confirmToken) {
        // 1. 从 Redis 读取
        String json = redisTemplate.opsForValue().get(CONFIRM_PREFIX + confirmToken);
        if (json == null) {
            // 检查 MySQL
            AgentConfirmAction action = confirmActionMapper.selectByToken(confirmToken);
            if (action == null) throw new BusinessException(ErrorCode.AGENT_CONFIRM_TOKEN_INVALID);
            if ("CONFIRMED".equals(action.getStatus())) throw new BusinessException(ErrorCode.AGENT_CONFIRM_TOKEN_INVALID, "已确认");
            if (action.getExpireAt().isBefore(LocalDateTime.now())) throw new BusinessException(ErrorCode.AGENT_CONFIRM_TOKEN_EXPIRED);
            json = action.getActionPayload();
        }
        return JsonUtils.fromJson(json, Map.class);
    }

    /**
     * 标记已确认。
     */
    public void markConfirmed(String confirmToken) {
        redisTemplate.delete(CONFIRM_PREFIX + confirmToken);
        AgentConfirmAction action = confirmActionMapper.selectByToken(confirmToken);
        if (action != null) {
            confirmActionMapper.updateStatus(confirmToken, "CONFIRMED");
        }
    }
}
