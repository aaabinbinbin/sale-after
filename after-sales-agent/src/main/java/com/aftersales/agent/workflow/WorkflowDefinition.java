package com.aftersales.agent.workflow;

import java.util.*;

/**
 * 工作流定义。
 *
 * 描述一个有向图：节点集合 + 边映射。
 * 边 key 为 "nodeId" 或 "nodeId:branch"，value 为下一节点 ID（null = 终止）。
 * 使用 Builder 模式构建。
 */
public class WorkflowDefinition {

    private final String name;
    private final String startNodeId;
    private final Map<String, WorkflowNode> nodes;
    private final Map<String, String> edges; // "nodeId" or "nodeId:branch" -> nextNodeId (null=end)

    private WorkflowDefinition(String name, String startNodeId,
                               Map<String, WorkflowNode> nodes, Map<String, String> edges) {
        this.name = name;
        this.startNodeId = startNodeId;
        this.nodes = Collections.unmodifiableMap(nodes);
        this.edges = Collections.unmodifiableMap(edges);
    }

    /**
     * 根据当前节点和分支标签获取下一节点 ID。
     *
     * 优先级：先匹配 "nodeId:branch"，再匹配 "nodeId"，都没有则返回 null（终止）。
     */
    public String resolveNext(String nodeId, String branch) {
        if (branch != null) {
            String key = nodeId + ":" + branch;
            if (edges.containsKey(key)) return edges.get(key);
        }
        return edges.get(nodeId);
    }

    public String getName() { return name; }
    public String getStartNodeId() { return startNodeId; }
    public Map<String, WorkflowNode> getNodes() { return nodes; }
    public Map<String, String> getEdges() { return edges; }

    // ====== Builder ======

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private String startNodeId;
        private final Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
        private final Map<String, String> edges = new LinkedHashMap<>();

        private Builder(String name) { this.name = name; }

        public Builder startWith(String nodeId) {
            this.startNodeId = nodeId;
            return this;
        }

        public Builder node(WorkflowNode node) {
            nodes.put(node.getId(), node);
            return this;
        }

        public Builder edge(String fromNodeId, String branch, String toNodeId) {
            String key = branch != null ? fromNodeId + ":" + branch : fromNodeId;
            edges.put(key, toNodeId);
            return this;
        }

        /** 无条件边 */
        public Builder edge(String fromNodeId, String toNodeId) {
            return edge(fromNodeId, null, toNodeId);
        }

        public WorkflowDefinition build() {
            Objects.requireNonNull(name, "name required");
            Objects.requireNonNull(startNodeId, "startNodeId required");
            if (!nodes.containsKey(startNodeId)) {
                throw new IllegalArgumentException("startNodeId '" + startNodeId + "' not in node definitions");
            }
            return new WorkflowDefinition(name, startNodeId, nodes, edges);
        }
    }
}
