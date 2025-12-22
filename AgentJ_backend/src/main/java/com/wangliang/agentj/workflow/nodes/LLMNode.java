package com.wangliang.agentj.workflow.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.wangliang.agentj.llm.WorkflowLlmService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM Node - invokes a language model with a prompt template
 * Supports variable substitution in prompts and configurable AI parameters
 */
@Slf4j
public class LLMNode implements NodeAction {

    private final WorkflowLlmService workflowLlmService;
    private final LLMNodeConfig config;

    public LLMNode(WorkflowLlmService workflowLlmService, LLMNodeConfig config) {
        this.workflowLlmService = workflowLlmService;
        this.config = config;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("LLM Node [{}] executing with model: {}, temp: {}, topP: {}",
                config.getNodeId(), config.getModelName(), config.getTemperature(), config.getTopP());

        // Build prompt with variable substitution
        String promptText = config.getPromptTemplate();
        Map<String, Object> variables = new HashMap<>(state.data());

        // Add any node-specific variables
        if (config.getVariables() != null) {
            variables.putAll(config.getVariables());
        }

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        String resolvedPrompt = promptTemplate.render(variables);

        log.debug("Resolved prompt: {}", resolvedPrompt);

        // Get system prompt from node config only (removed runtime override)
        String systemPrompt = config.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = "You are a helpful assistant.";
        }
        log.debug("Using system prompt: {}", systemPrompt);

        // Create ChatClient with configured model and parameters
        ChatClient chatClient = workflowLlmService.createChatClient(
                config.getModelName(),
                config.getTemperature(),
                config.getTopP());

        // Call LLM
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(resolvedPrompt)
                .call()
                .content();

        log.info("LLM response received, length: {}", response != null ? response.length() : 0);

        // Store result in state with configured output key
        Map<String, Object> result = new HashMap<>();
        String outputKey = config.getOutputKey() != null ? config.getOutputKey() : "llm_output";
        result.put(outputKey, response);
        result.put("_current_node", config.getNodeId());

        return result;
    }

    @Data
    @Builder
    public static class LLMNodeConfig {
        private String nodeId;
        private String promptTemplate;
        private String systemPrompt;
        private String outputKey;
        private Map<String, Object> variables;
        private String modelName; // Model name for selection
        private Double temperature; // Temperature (0-2)
        private Double topP; // Top P (0-1)
    }
}
