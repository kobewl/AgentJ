package com.wangliang.agentj.workflow.config;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for workflow module
 */
@Configuration
public class WorkflowConfig {

    /**
     * Tool registry for ToolNode - collects all available ToolCallbacks
     */
    @Bean
    public Map<String, ToolCallback> workflowToolRegistry(
            @Autowired(required = false) List<ToolCallback> toolCallbacks) {
        if (toolCallbacks == null) {
            return Collections.emptyMap();
        }
        Map<String, ToolCallback> registry = new HashMap<>();
        for (ToolCallback callback : toolCallbacks) {
            // Use getToolDefinition().name() as ToolCallback interface doesn't have getName()
            String name = callback.getToolDefinition().name();
            registry.put(name, callback);
        }
        return registry;
    }
}
