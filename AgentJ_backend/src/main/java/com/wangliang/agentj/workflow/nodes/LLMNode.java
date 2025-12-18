package com.wangliang.agentj.workflow.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM Node - invokes a language model with a prompt template
 * Supports variable substitution in prompts
 */
@Slf4j
public class LLMNode implements NodeAction {

    private final ChatClient chatClient;
    private final LLMNodeConfig config;

    public LLMNode(ChatClient.Builder chatClientBuilder, LLMNodeConfig config) {
        this.chatClient = chatClientBuilder.build();
        this.config = config;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("LLM Node [{}] executing with prompt template", config.getNodeId());
        
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
        
        // Determine system prompt: runtime input > node config > default
        String systemPrompt = determineSystemPrompt(state);
        log.debug("Using system prompt: {}", systemPrompt);
        
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
    
    /**
     * Determine system prompt with priority:
     * 1. Runtime input from state (passed during execution)
     * 2. Node configuration systemPrompt
     * 3. Default prompt
     */
    private String determineSystemPrompt(OverAllState state) {
        // Check for runtime system prompt from execution input
        Object runtimePrompt = state.value("systemPrompt").orElse(null);
        if (runtimePrompt != null && !runtimePrompt.toString().trim().isEmpty()) {
            return runtimePrompt.toString();
        }
        
        // Fall back to node configuration
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().trim().isEmpty()) {
            return config.getSystemPrompt();
        }
        
        // Default prompt
        return "You are a helpful assistant.";
    }

    @Data
    @Builder
    public static class LLMNodeConfig {
        private String nodeId;
        private String promptTemplate;
        private String systemPrompt;
        private String outputKey;
        private Map<String, Object> variables;
        private String modelName; // optional: for model switching
        private Double temperature; // optional: for temperature control
    }
}
