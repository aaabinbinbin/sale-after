package com.aftersales.agent.workflow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class NodeResult {

    private final boolean success;
    private final String nextNodeId;
    private final String branch;
    private final Map<String, Object> output;
    private final String error;

    public static NodeResult next(String nextNodeId) {
        return new NodeResult(true, nextNodeId, null, Collections.emptyMap(), null);
    }

    public static NodeResult next(String nextNodeId, Map<String, Object> output) {
        return new NodeResult(true, nextNodeId, null, output, null);
    }

    public static NodeResult branch(String branch, Map<String, Object> output) {
        return new NodeResult(true, null, branch, output, null);
    }

    public static NodeResult end(Map<String, Object> output) {
        return new NodeResult(true, null, null, output, null);
    }

    public static NodeResult end() {
        return new NodeResult(true, null, null, Collections.emptyMap(), null);
    }

    public static NodeResult fail(String error) {
        return new NodeResult(false, null, null, Collections.emptyMap(), error);
    }
}
