package com.aftersales.agent.tool;

import com.aftersales.agent.memory.MemoryEntry;
import com.aftersales.agent.memory.MemoryManager;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 客户领域工具聚合。
 *
 * 提供客户画像、偏好、历史行为等聚合查询。
 */
@Component
public class CustomerDomainTools {

    private static final Logger log = LoggerFactory.getLogger(CustomerDomainTools.class);

    private final MemoryManager memoryManager;

    public CustomerDomainTools(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    /**
     * 获取客户摘要（偏好 + 历史行为）。
     */
    public Map<String, Object> getCustomerSummary(String userId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("userId", userId);

        // 长期偏好
        Map<Object, Object> prefs = memoryManager.recallPreferences(userId);
        summary.put("preferences", prefs);

        // 业务记忆
        List<MemoryEntry> businessMemories = memoryManager.recallBusiness(userId);
        summary.put("businessSummary", businessMemories.stream()
                .map(m -> Map.of("key", m.getKey(), "content", m.getContent()))
                .toList());

        log.info("CustomerDomainTools: 客户摘要构建完成 userId={}", userId);
        return summary;
    }
}
