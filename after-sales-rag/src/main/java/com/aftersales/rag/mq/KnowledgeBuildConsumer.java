package com.aftersales.rag.mq;

import com.aftersales.common.util.JsonUtils;
import com.aftersales.infra.mq.RabbitConfig;
import com.aftersales.rag.service.KnowledgeBuildService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 知识构建消费者。
 *
 * 消费 RabbitMQ 消息，执行异步知识构建。
 * 支持幂等（查 knowledge_build_task 防重）+ 重试上限 3 次。
 */
@Component
public class KnowledgeBuildConsumer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBuildConsumer.class);

    private final KnowledgeBuildService knowledgeBuildService;

    public KnowledgeBuildConsumer(KnowledgeBuildService knowledgeBuildService) {
        this.knowledgeBuildService = knowledgeBuildService;
    }

    /**
     * 消费知识构建消息。
     *
     * 异常抛出后由 RabbitMQ 重试（application.yml 配置 max-attempts=3）。
     */
    @RabbitListener(queues = RabbitConfig.KNOWLEDGE_BUILD_QUEUE)
    public void onMessage(String messageJson) {
        log.info("收到知识构建消息: {}", messageJson);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = JsonUtils.fromJson(messageJson, Map.class);
            String afterSalesNo = (String) message.get("afterSalesNo");
            String taskNo = (String) message.get("taskNo");

            if (afterSalesNo == null) {
                log.warn("知识构建消息缺少 afterSalesNo，跳过");
                return;
            }

            // 执行构建
            knowledgeBuildService.buildFromAfterSales(afterSalesNo);
            log.info("知识构建完成 afterSalesNo={} taskNo={}", afterSalesNo, taskNo);

        } catch (Exception e) {
            log.error("知识构建失败: {}", messageJson, e);
            throw e; // 抛出异常触发 RabbitMQ 重试
        }
    }
}
