package com.wangliang.agentj.workflow.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool Node - executes a registered tool/function
 * Supports passing parameters from workflow state
 */
@Slf4j
public class ToolNode implements NodeAction {

    private final ToolNodeConfig config;
    private final Map<String, ToolCallback> toolRegistry;

    public ToolNode(ToolNodeConfig config, Map<String, ToolCallback> toolRegistry) {
        this.config = config;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("Tool Node [{}] executing tool: {}", config.getNodeId(), config.getToolName());
        
        ToolCallback tool = toolRegistry.get(config.getToolName());
        if (tool == null) {
            throw new IllegalStateException("Tool not found: " + config.getToolName());
        }
        
        // Build tool input from state and config
        Map<String, Object> toolInput = new HashMap<>();
        
        // Map state variables to tool parameters
        if (config.getParameterMapping() != null) {
            for (Map.Entry<String, String> mapping : config.getParameterMapping().entrySet()) {
                String paramName = mapping.getKey();
                String stateKey = mapping.getValue();
                Object value = state.value(stateKey).orElse(null);
                if (value != null) {
                    toolInput.put(paramName, value);
                }
            }
        }
        
        // Add static parameters from config
        if (config.getStaticParameters() != null) {
            toolInput.putAll(config.getStaticParameters());
        }
        
        log.debug("Tool input: {}", toolInput);
        
        // Execute tool
        String toolInputJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toolInput);
        String result = tool.call(toolInputJson);
        
        log.info("Tool execution completed, result length: {}", result != null ? result.length() : 0);
        
        // Store result in state
        Map<String, Object> output = new HashMap<>();
        String outputKey = config.getOutputKey() != null ? config.getOutputKey() : "tool_output";
        output.put(outputKey, result);
        output.put("_current_node", config.getNodeId());
        
        return output;
    }

    @Data
    @Builder
    public static class ToolNodeConfig {
        private String nodeId;
        private String toolName;
        private Map<String, String> parameterMapping; // tool param -> state key
        private Map<String, Object> staticParameters;
        private String outputKey;
    }
}
