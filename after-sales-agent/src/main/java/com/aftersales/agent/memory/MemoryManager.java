package com.aftersales.agent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 记忆管理器。
 *
 * 统一管理 5 层记忆：
 * - ShortTermMemory → Redis（TTL 30min）
 * - LongTermMemory  → MySQL + Redis 缓存
 * - BusinessMemory  → MySQL 聚合查询
 * - WorkflowMemory  → Redis
 * - SemanticMemory  → RAG 向量检索（委托给 RagRetrievalService）
 */
@Component
public class MemoryManager {

    private static final Logger log = LoggerFactory.getLogger(MemoryManager.class);

    private static final String SHORT_TERM_PREFIX = "mem:short:";
    private static final String WORKFLOW_PREFIX = "mem:workflow:";
    private static final String LONG_TERM_PREFIX = "mem:long:";

    private final StringRedisTemplate redisTemplate;

    // 短期记忆 TTL（30分钟）
    private static final int SHORT_TERM_TTL_MINUTES = 30;

    // 会话记忆：内存缓存（ConcurrentHashMap）
    private final Map<String, List<MemoryEntry>> sessionMemories = new ConcurrentHashMap<>();

    public MemoryManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== ShortTermMemory ====================

    /** 保存当前会话的对话 */
    public void rememberConversation(String sessionId, String role, String content) {
        String key = SHORT_TERM_PREFIX + sessionId;
        String entry = System.currentTimeMillis() + "|" + role + "|" + content;

        redisTemplate.opsForList().rightPush(key, entry);
        redisTemplate.expire(key, SHORT_TERM_TTL_MINUTES, TimeUnit.MINUTES);

        // 只保留最近 20 轮
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > 40) { // user+assistant = 1 轮 = 2 条
            redisTemplate.opsForList().trim(key, size - 40, -1);
        }
    }

    /** 获取当前会话的最近对话 */
    public List<String> recallConversation(String sessionId, int maxRounds) {
        String key = SHORT_TERM_PREFIX + sessionId;
        List<String> all = redisTemplate.opsForList().range(key, 0, -1);
        if (all == null || all.isEmpty()) return List.of();

        int from = Math.max(0, all.size() - maxRounds * 2);
        return all.subList(from, all.size());
    }

    // ==================== LongTermMemory ====================

    /** 保存用户长期偏好 */
    public void rememberPreference(String userId, String prefType, String prefValue) {
        String key = LONG_TERM_PREFIX + userId;
        redisTemplate.opsForHash().put(key, prefType, prefValue);
    }

    /** 获取用户长期偏好 */
    public Map<Object, Object> recallPreferences(String userId) {
        String key = LONG_TERM_PREFIX + userId;
        return redisTemplate.opsForHash().entries(key);
    }

    // ==================== BusinessMemory ====================

    /** 记录业务记忆（如售后历史摘要） */
    public void rememberBusiness(String userId, String key, String summary) {
        MemoryEntry entry = MemoryEntry.builder()
                .id(UUID.randomUUID().toString().substring(0, 8))
                .type(MemoryType.BUSINESS)
                .key(key)
                .content(summary)
                .build();
        sessionMemories.computeIfAbsent(userId, k -> new ArrayList<>()).add(entry);
    }

    /** 获取业务记忆 */
    public List<MemoryEntry> recallBusiness(String userId) {
        return sessionMemories.getOrDefault(userId, List.of()).stream()
                .filter(e -> e.getType() == MemoryType.BUSINESS)
                .filter(e -> !e.isExpired())
                .collect(Collectors.toList());
    }

    // ==================== WorkflowMemory ====================

    /** 保存工作流断点状态 */
    public void saveWorkflowState(String workflowId, Map<String, Object> state) {
        String key = WORKFLOW_PREFIX + workflowId;
        state.forEach((k, v) ->
                redisTemplate.opsForHash().put(key, k, v != null ? v.toString() : ""));
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    /** 恢复工作流状态 */
    public Map<Object, Object> loadWorkflowState(String workflowId) {
        String key = WORKFLOW_PREFIX + workflowId;
        return redisTemplate.opsForHash().entries(key);
    }

    /** 清除工作流状态 */
    public void clearWorkflowState(String workflowId) {
        redisTemplate.delete(WORKFLOW_PREFIX + workflowId);
    }

    // ==================== SemanticMemory ====================

    /**
     * 语义记忆委托给 RAG 模块。
     * 此方法返回提示：调用方应使用 RagRetrievalService.search() 获取语义相关内容。
     */
    public String buildSemanticQuery(String userInput, String intent) {
        return userInput + " " + intent;
    }

    // ==================== 汇总 ====================

    /** 汇总所有层记忆为一个 prompt 片段 */
    public String buildMemoryPrompt(String sessionId, String userId) {
        StringBuilder sb = new StringBuilder();

        List<String> conversations = recallConversation(sessionId, 5);
        if (!conversations.isEmpty()) {
            sb.append("【对话历史】\n");
            conversations.forEach(c -> sb.append("  ").append(c).append("\n"));
        }

        Map<Object, Object> prefs = recallPreferences(userId);
        if (!prefs.isEmpty()) {
            sb.append("【用户偏好】\n");
            prefs.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        }

        return sb.toString();
    }
}
