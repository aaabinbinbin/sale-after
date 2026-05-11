package com.aftersales.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 配置。
 *
 * 使用阿里百炼平台（OpenAI 兼容接口）。
 * 主 Agent: qwen3.6-flash
 * Embedding: qwen3-vl-rerank
 * 多模态: qwen3.5-omni-flash-realtime（通过单独 client 调用）
 *
 * API Key 从环境变量 MY_API_KEY 读取，Base URL 从 MY_BASE_URL 读取。
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    /** ChatClient —— Agent 主模型，用于意图路由 + 循环决策 + 答案生成 */
    @Bean
    public ChatClient agentChatClient(ChatModel chatModel) {
        log.info("Agent ChatClient 已初始化, model=qwen3.6-flash");
        return ChatClient.builder(chatModel).build();
    }

    /** Embedding 模型是否可用 */
    @Bean
    public boolean embeddingReady(EmbeddingModel embeddingModel) {
        if (embeddingModel != null) {
            log.info("EmbeddingModel 已就绪, model=qwen3-vl-rerank");
            return true;
        }
        log.warn("EmbeddingModel 未配置，RAG 将使用 MySQL fallback");
        return false;
    }
}
