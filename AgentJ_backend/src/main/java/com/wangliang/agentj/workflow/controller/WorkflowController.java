package com.wangliang.agentj.workflow.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.wangliang.agentj.llm.WorkflowLlmService;
import com.wangliang.agentj.workflow.dto.WorkflowDTO;
import com.wangliang.agentj.workflow.dto.WorkflowExecuteRequest;
import com.wangliang.agentj.workflow.entity.WorkflowExecution;
import com.wangliang.agentj.workflow.service.WorkflowExecutionService;
import com.wangliang.agentj.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * REST API for workflow management and execution
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Tag(name = "Workflow", description = "工作流设计与执行 API")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService executionService;
    private final WorkflowLlmService workflowLlmService;

    // ============ CRUD Operations ============

    @GetMapping
    @Operation(summary = "获取所有工作流")
    public ResponseEntity<List<WorkflowDTO>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个工作流")
    public ResponseEntity<WorkflowDTO> getWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @PostMapping
    @Operation(summary = "创建工作流")
    public ResponseEntity<WorkflowDTO> createWorkflow(@RequestBody WorkflowDTO dto) {
        return ResponseEntity.ok(workflowService.createWorkflow(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工作流")
    public ResponseEntity<WorkflowDTO> updateWorkflow(@PathVariable Long id, @RequestBody WorkflowDTO dto) {
        WorkflowDTO updated = workflowService.updateWorkflow(id, dto);
        // Invalidate cache when workflow is updated
        executionService.invalidateCache(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作流")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        executionService.invalidateCache(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布工作流")
    public ResponseEntity<WorkflowDTO> publishWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.publishWorkflow(id));
    }

    // ============ Execution Operations ============

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行工作流（同步）")
    public ResponseEntity<Map<String, Object>> executeWorkflow(
            @PathVariable Long id,
            @RequestBody WorkflowExecuteRequest request) {
        try {
            Map<String, Object> result = executionService.execute(id, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "执行工作流（流式）")
    public Flux<NodeOutput> executeWorkflowStream(
            @PathVariable Long id,
            @RequestBody WorkflowExecuteRequest request) {
        try {
            return executionService.executeStream(id, request);
        } catch (Exception e) {
            log.error("Failed to start streaming execution", e);
            return Flux.error(e);
        }
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "获取工作流执行历史")
    public ResponseEntity<List<WorkflowExecution>> getExecutionHistory(@PathVariable Long id) {
        return ResponseEntity.ok(executionService.getExecutionHistory(id));
    }

    // ============ Node Types ============

    @GetMapping("/node-types")
    @Operation(summary = "获取可用节点类型")
    public ResponseEntity<List<Map<String, Object>>> getNodeTypes() {
        List<Map<String, Object>> nodeTypes = List.of(
                Map.of(
                        "type", "start",
                        "label", "开始",
                        "description", "工作流入口节点",
                        "icon", "play",
                        "color", "#52c41a"),
                Map.of(
                        "type", "end",
                        "label", "结束",
                        "description", "工作流结束节点",
                        "icon", "stop",
                        "color", "#ff4d4f"),
                Map.of(
                        "type", "llm",
                        "label", "LLM",
                        "description", "调用大语言模型",
                        "icon", "robot",
                        "color", "#1890ff",
                        "configSchema", Map.of(
                                "modelName", Map.of("type", "select", "label", "选择模型"),
                                "systemPrompt", Map.of("type", "textarea", "label", "系统提示词"),
                                "promptTemplate", Map.of("type", "textarea", "label", "提示词模板", "required", true),
                                "temperature",
                                Map.of("type", "slider", "label", "温度", "min", 0, "max", 2, "step", 0.1, "default",
                                        0.7),
                                "topP",
                                Map.of("type", "slider", "label", "Top P", "min", 0, "max", 1, "step", 0.1, "default",
                                        1.0),
                                "outputKey", Map.of("type", "text", "label", "输出变量名", "default", "llm_output"))),
                Map.of(
                        "type", "condition",
                        "label", "条件",
                        "description", "条件分支判断",
                        "icon", "branch",
                        "color", "#faad14",
                        "configSchema", Map.of(
                                "expression", Map.of("type", "code", "label", "条件表达式", "language", "javascript"),
                                "trueTarget", Map.of("type", "text", "label", "真值目标"),
                                "falseTarget", Map.of("type", "text", "label", "假值目标"))),
                Map.of(
                        "type", "tool",
                        "label", "工具",
                        "description", "调用系统工具",
                        "icon", "tool",
                        "color", "#722ed1",
                        "configSchema", Map.of(
                                "toolName", Map.of("type", "select", "label", "选择工具", "required", true),
                                "parameterMapping", Map.of("type", "keyvalue", "label", "参数映射"),
                                "outputKey", Map.of("type", "text", "label", "输出变量名", "default", "tool_output"))));
        return ResponseEntity.ok(nodeTypes);
    }

    // ============ Model Operations ============

    @GetMapping("/models")
    @Operation(summary = "获取可用模型列表")
    public ResponseEntity<List<Map<String, Object>>> getAvailableModels() {
        return ResponseEntity.ok(workflowLlmService.getAvailableModels());
    }
}
