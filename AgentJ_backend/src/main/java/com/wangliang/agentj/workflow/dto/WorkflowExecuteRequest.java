package com.wangliang.agentj.workflow.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * Request DTO for workflow execution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecuteRequest {

    /**
     * Input variables for the workflow
     */
    private Map<String, Object> inputs;

    /**
     * Whether to stream the execution output
     */
    private boolean stream;

    /**
     * Thread ID for maintaining conversation state across executions
     * If not provided, a new thread ID will be generated
     */
    private String threadId;

    /**
     * Store ID for long-term memory (optional)
     */
    private String storeId;
}
