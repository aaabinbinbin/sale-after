package com.aftersales.agent.workflow.node;

import com.aftersales.agent.workflow.NodeResult;
import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.function.Function;

/**
 * 事件发布节点。
 *
 * 发布 Spring Domain Event / 发送 MQ 消息，不阻塞流程。
 */
public class EventNode implements WorkflowNode {

    private static final Logger log = LoggerFactory.getLogger(EventNode.class);

    private final String id;
    private final String eventName;
    private final ApplicationEventPublisher eventPublisher;
    private final Function<WorkflowContext, Object> eventFactory;

    public EventNode(String id, String eventName, ApplicationEventPublisher eventPublisher,
                     Function<WorkflowContext, Object> eventFactory) {
        this.id = id;
        this.eventName = eventName;
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getType() { return "EventNode"; }

    @Override
    public NodeResult execute(WorkflowContext context) {
        try {
            Object event = eventFactory.apply(context);
            eventPublisher.publishEvent(event);
            log.info("EventNode [{}] 发布事件: {}", id, eventName);
            return NodeResult.next(null, Map.of(
                    "eventName", eventName,
                    "published", true
            ));
        } catch (Exception e) {
            log.error("EventNode [{}] 发布事件失败: {}", id, e.getMessage());
            return NodeResult.fail("事件发布失败: " + e.getMessage());
        }
    }
}
