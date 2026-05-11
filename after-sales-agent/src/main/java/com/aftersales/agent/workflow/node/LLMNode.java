package com.aftersales.agent.workflow.node;

import com.aftersales.agent.workflow.NodeResult;
import com.aftersales.agent.workflow.WorkflowContext;
import com.aftersales.agent.workflow.WorkflowNode;
import com.aftersales.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Map;
import java.util.function.Function;

/**
 * LLM 决策节点。
 *
 * 调用 LLM 进行不确定性决策，LLM 返回一个分类标签作为分支。
 * 典型场景：判断是否需要人工审核、选择处理策略等。
 */
public class LLMNode implements WorkflowNode {

    private static final Logger log = LoggerFactory.getLogger(LLMNode.class);

    private final String id;
    private final String description;
    private final ChatClient chatClient;
    private final String systemPrompt;
    private final Function<WorkflowContext, String> userPromptBuilder;
    private final String[] validBranches; // LLM 必须输出其中之一

    public LLMNode(String id, String description, ChatClient chatClient,
                   String systemPrompt, Function<WorkflowContext, String> userPromptBuilder,
                   String... validBranches) {
        this.id = id;
        this.description = description;
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.userPromptBuilder = userPromptBuilder;
        this.validBranches = validBranches;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getType() { return "LLMNode"; }

    @Override
    public NodeResult execute(WorkflowContext context) {
        try {
            String userPrompt = userPromptBuilder.apply(context);
            ChatResponse chatResp = chatClient.prompt()
                    .system(systemPrompt).user(userPrompt).call().chatResponse();
            String llmResponse = chatResp.getResult().getOutput().getContent();
            log.info("LLMNode [{}] 响应: {}", id, llmResponse);

            String branch = parseBranch(llmResponse);
            return NodeResult.branch(branch, Map.of(
                    "nodeId", id,
                    "description", description,
                    "llmResponse", llmResponse,
                    "branch", branch
            ));

        } catch (Exception e) {
            log.warn("LLMNode [{}] 执行失败: {}", id, e.getMessage());
            // LLM 不可用时降级到第一个有效分支
            String fallback = validBranches.length > 0 ? validBranches[0] : "ERROR";
            return NodeResult.branch(fallback, Map.of(
                    "nodeId", id,
                    "error", e.getMessage(),
                    "branch", fallback,
                    "fallback", true
            ));
        }
    }

    @SuppressWarnings("unchecked")
    private String parseBranch(String llmResponse) {
        try {
            String json = llmResponse.trim().replaceAll("```json|```", "").trim();
            Map<String, Object> map = JsonUtils.fromJson(json, Map.class);
            String decision = map.get("decision") != null ? map.get("decision").toString() : null;
            if (decision != null) {
                for (String valid : validBranches) {
                    if (valid.equalsIgnoreCase(decision)) return valid;
                }
            }
        } catch (Exception ignored) {}
        // 尝试从原始文本中匹配
        for (String valid : validBranches) {
            if (llmResponse.toUpperCase().contains(valid.toUpperCase())) return valid;
        }
        return validBranches.length > 0 ? validBranches[0] : "UNKNOWN";
    }
}
