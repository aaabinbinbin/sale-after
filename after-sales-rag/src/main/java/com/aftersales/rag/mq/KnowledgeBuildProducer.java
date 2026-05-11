package com.aftersales.rag.mq;

import com.aftersales.common.util.IdGenerator;
import com.aftersales.common.util.JsonUtils;
import com.aftersales.infra.mq.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 知识构建生产者。
 *
 * 售后完成后发送消息到 RabbitMQ，异步触发知识构建。
 */
@Component
public class KnowledgeBuildProducer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBuildProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public KnowledgeBuildProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送知识构建任务。
     *
     * @param afterSalesNo 售后单号
     */
    public void sendBuildTask(String afterSalesNo) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("afterSalesNo", afterSalesNo);
        message.put("taskNo", IdGenerator.genBuildTaskNo());
        message.put("timestamp", System.currentTimeMillis());

        String json = JsonUtils.toJson(message);
        rabbitTemplate.convertAndSend(
                RabbitConfig.KNOWLEDGE_EXCHANGE,
                RabbitConfig.KNOWLEDGE_BUILD_ROUTING_KEY,
                json);

        log.info("知识构建消息已发送 afterSalesNo={} taskNo={}", afterSalesNo, message.get("taskNo"));
    }
}
