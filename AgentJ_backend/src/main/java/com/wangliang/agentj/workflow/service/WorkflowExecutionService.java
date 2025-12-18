package com.wangliang.agentj.workflow.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangliang.agentj.workflow.converter.WorkflowGraphConverter;
import com.wangliang.agentj.workflow.dto.WorkflowDTO;
import com.wangliang.agentj.workflow.dto.WorkflowExecuteRequest;
import com.wangliang.agentj.workflow.entity.WorkflowExecution;
import com.wangliang.agentj.workflow.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for workflow execution using Spring AI Alibaba Graph
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {

    private final WorkflowService workflowService;
    private final WorkflowGraphConverter graphConverter;
    private final WorkflowExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    // Cache for compiled graphs
    private final Map<Long, CompiledGraph> compiledGraphCache = new ConcurrentHashMap<>();

    /**
     * Execute a workflow synchronously
     */
    public Map<String, Object> execute(Long workflowId, WorkflowExecuteRequest request) throws Exception {
        log.info("Executing workflow: id={}", workflowId);

        // Create execution record
        WorkflowExecution execution = WorkflowExecution.builder()
                .workflowId(workflowId)
                .inputData(objectMapper.writeValueAsString(request.getInputs()))
                .build();
        execution = executionRepository.save(execution);

        try {
            // Get or compile the graph
            CompiledGraph compiledGraph = getOrCompileGraph(workflowId);

            // Prepare initial state
            Map<String, Object> initialState = new HashMap<>();
            if (request.getInputs() != null) {
                initialState.putAll(request.getInputs());
            }

            // Create runtime config
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(UUID.randomUUID().toString())
                    .build();

            // Execute the graph
            Map<String, Object> finalState = new HashMap<>();
            Flux<NodeOutput> stream = compiledGraph.stream(initialState, config);
            stream.doOnNext(output -> {
                log.debug("Node output: {}", output);
                if (output.state() != null) {
                    finalState.putAll(output.state().data());
                }
            }).blockLast();

            // Update execution record
            execution.markCompleted(objectMapper.writeValueAsString(finalState));
            executionRepository.save(execution);

            log.info("Workflow execution completed: executionId={}", execution.getId());
            return finalState;

        } catch (Exception e) {
            log.error("Workflow execution failed: workflowId={}", workflowId, e);
            execution.markFailed(e.getMessage());
            executionRepository.save(execution);
            throw e;
        }
    }

    /**
     * Execute a workflow with streaming output
     */
    public Flux<NodeOutput> executeStream(Long workflowId, WorkflowExecuteRequest request) throws Exception {
        log.info("Executing workflow (streaming): id={}", workflowId);

        // Get or compile the graph
        CompiledGraph compiledGraph = getOrCompileGraph(workflowId);

        // Prepare initial state
        Map<String, Object> initialState = new HashMap<>();
        if (request.getInputs() != null) {
            initialState.putAll(request.getInputs());
        }

        // Create runtime config
        RunnableConfig config = RunnableConfig.builder()
                .threadId(UUID.randomUUID().toString())
                .build();

        // Return the stream
        return compiledGraph.stream(initialState, config)
                .doOnNext(output -> log.debug("Streaming node output: {}", output))
                .doOnError(e -> log.error("Streaming execution error: ", e))
                .doOnComplete(() -> log.info("Streaming execution completed"));
    }

    /**
     * Get or compile a graph for a workflow
     */
    private CompiledGraph getOrCompileGraph(Long workflowId) throws Exception {
        return compiledGraphCache.computeIfAbsent(workflowId, id -> {
            try {
                WorkflowDTO dto = workflowService.getWorkflow(id);
                StateGraph graph = graphConverter.convert(dto);

                // Configure compilation with memory saver
                CompileConfig compileConfig = CompileConfig.builder()
                        .saverConfig(SaverConfig.builder()
                                .register(new MemorySaver())
                                .build())
                        .build();

                return graph.compile(compileConfig);
            } catch (Exception e) {
                throw new RuntimeException("Failed to compile workflow: " + id, e);
            }
        });
    }

    /**
     * Invalidate cached graph when workflow is updated
     */
    public void invalidateCache(Long workflowId) {
        compiledGraphCache.remove(workflowId);
        log.info("Invalidated graph cache for workflow: {}", workflowId);
    }

    /**
     * Get execution history for a workflow
     */
    public java.util.List<WorkflowExecution> getExecutionHistory(Long workflowId) {
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
    }
}
