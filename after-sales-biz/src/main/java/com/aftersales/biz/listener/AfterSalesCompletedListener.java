package com.aftersales.biz.listener;

import com.aftersales.domain.event.AfterSalesCompletedEvent;
import com.aftersales.rag.mq.KnowledgeBuildProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 售后完成事件监听器。
 *
 * 售后完成后异步触发知识构建：发布 RabbitMQ 消息 → Consumer 消费 → 构建案例文档。
 */
@Component
public class AfterSalesCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(AfterSalesCompletedListener.class);

    private final KnowledgeBuildProducer knowledgeBuildProducer;

    public AfterSalesCompletedListener(KnowledgeBuildProducer knowledgeBuildProducer) {
        this.knowledgeBuildProducer = knowledgeBuildProducer;
    }

    /**
     * 监听售后完成事件，触发异步知识构建。
     */
    @EventListener
    public void onAfterSalesCompleted(AfterSalesCompletedEvent event) {
        log.info("售后完成事件触发知识构建 afterSalesNo={} type={}",
                event.getAfterSalesNo(), event.getAfterSalesType());
        try {
            knowledgeBuildProducer.sendBuildTask(event.getAfterSalesNo());
        } catch (Exception e) {
            log.error("知识构建消息发送失败 afterSalesNo={}: {}", event.getAfterSalesNo(), e.getMessage());
        }
    }
}
