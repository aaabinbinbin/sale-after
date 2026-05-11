package com.aftersales.infra.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置。
 *
 * 定义知识构建任务所需的 Exchange、Queue、Binding。
 * Exchange: after-sales.knowledge.exchange (Topic)
 * Queue:    after-sales.knowledge.build.queue
 * Routing:  after-sales.knowledge.build
 */
@Configuration
public class RabbitConfig {

    // ====== Exchange ======
    public static final String KNOWLEDGE_EXCHANGE = "after-sales.knowledge.exchange";

    // ====== Queue ======
    public static final String KNOWLEDGE_BUILD_QUEUE = "after-sales.knowledge.build.queue";

    // ====== Routing Key ======
    public static final String KNOWLEDGE_BUILD_ROUTING_KEY = "after-sales.knowledge.build";

    /** Topic Exchange */
    @Bean
    public TopicExchange knowledgeExchange() {
        return new TopicExchange(KNOWLEDGE_EXCHANGE);
    }

    /** 知识构建队列 */
    @Bean
    public Queue knowledgeBuildQueue() {
        return QueueBuilder.durable(KNOWLEDGE_BUILD_QUEUE).build();
    }

    /** 绑定 */
    @Bean
    public Binding knowledgeBuildBinding() {
        return BindingBuilder.bind(knowledgeBuildQueue())
                .to(knowledgeExchange())
                .with(KNOWLEDGE_BUILD_ROUTING_KEY);
    }

    /** JSON 消息转换器（替代默认的 SimpleMessageConverter） */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
