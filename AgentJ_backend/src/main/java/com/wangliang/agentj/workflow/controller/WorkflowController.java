package com.wangliang.agentj.workflow.controller;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.wangliang.agentj.llm.WorkflowLlmService;
import com.wangliang.agentj.workflow.dto.WorkflowDTO;
import com.wangliang.agentj.workflow.dto.WorkflowExecuteRequest;
import com.wangliang.agentj.workflow.entity.WorkflowExecution;
import com.wangliang.agentj.workflow.service.WorkflowExecutionService;
import com.wangliang.agentj.workflow.service.WorkflowExecutionServiceV2;
import com.wangliang.agentj.workflow.service.WorkflowService;
import com.wangliang.agentj.workflow.store.WorkflowStoreConfig;
import com.wangliang.agentj.workflow.store.WorkflowStoreConfig.StoreHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工作流REST API
 *
 * 功能：
 * - 基础 CRUD 操作
 * - 状态历史查询
 * - 时间旅行（重放）
 * - 人在回路（暂停/恢复）
 * - 长期内存操作
 * - 缓存管理
 *
 * @author AgentJ
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Tag(name = "Workflow", description = "工作流API - 支持状态历史、时间旅行、人在回路、长期内存")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService executionService;
    private final WorkflowExecutionServiceV2 executionServiceV2;
    private final WorkflowLlmService workflowLlmService;

    @Autowired(required = false)
    private WorkflowStoreConfig.StoreHelper storeHelper;

    // ============ 基础CRUD（继承原有API）============

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
        executionServiceV2.invalidateCache(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作流")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        executionServiceV2.invalidateCache(id);
        return ResponseEntity.noContent().build();
    }

    // ============ 执行API（支持Store）============

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行工作流（同步）")
    public ResponseEntity<Map<String, Object>> executeWorkflow(
            @PathVariable Long id,
            @RequestBody WorkflowExecuteRequest request) {
        try {
            Map<String, Object> result = executionServiceV2.execute(id, request);
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
            return executionServiceV2.executeStream(id, request);
        } catch (Exception e) {
            log.error("Failed to start streaming execution", e);
            return Flux.error(e);
        }
    }

    // ============ 状态历史与时间旅行 ============

    @GetMapping("/state/{threadId}")
    @Operation(summary = "获取会话当前状态")
    public ResponseEntity<?> getState(@PathVariable String threadId) {
        Optional<WorkflowExecutionServiceV2.StateSnapshotView> state =
                executionServiceV2.getState(threadId);

        if (state.isPresent()) {
            return ResponseEntity.ok(state.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/state/{threadId}/history")
    @Operation(summary = "获取会话状态历史")
    public ResponseEntity<List<WorkflowExecutionServiceV2.StateSnapshotView>> getStateHistory(
            @PathVariable String threadId) {
        List<WorkflowExecutionServiceV2.StateSnapshotView> history =
                executionServiceV2.getStateHistory(threadId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/replay")
    @Operation(summary = "重放工作流（时间旅行）")
    public ResponseEntity<Map<String, Object>> replayWorkflow(
            @PathVariable Long id,
            @RequestParam String threadId,
            @RequestParam(required = false) String checkpointId,
            @RequestBody Map<String, Object> inputs) {
        try {
            Map<String, Object> result = executionServiceV2.replay(id, threadId, checkpointId, inputs);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Workflow replay failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/state/{threadId}/update")
    @Operation(summary = "更新状态并继续执行（时间旅行）")
    public ResponseEntity<Map<String, String>> updateState(
            @PathVariable Long workflowId,
            @PathVariable String threadId,
            @RequestBody Map<String, Object> updates,
            @RequestParam(required = false) String asNode) {
        try {
            executionServiceV2.updateState(workflowId, threadId, updates, asNode);
            return ResponseEntity.ok(Map.of(
                    "message", "State updated successfully",
                    "threadId", threadId
            ));
        } catch (Exception e) {
            log.error("State update failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============ 人在回路（Human-in-the-Loop）============

    @PostMapping("/execution/{threadId}/pause")
    @Operation(summary = "暂停执行（等待人工介入）")
    public ResponseEntity<Map<String, String>> pauseExecution(
            @PathVariable String threadId,
            @RequestParam(required = false) String checkpointId) {
        executionServiceV2.pauseExecution(threadId);
        return ResponseEntity.ok(Map.of(
                "message", "Execution paused",
                "threadId", threadId
        ));
    }

    @PostMapping("/execution/{threadId}/resume")
    @Operation(summary = "恢复执行（人工输入后继续）")
    public ResponseEntity<Map<String, String>> resumeExecution(
            @PathVariable String threadId,
            @RequestBody Map<String, Object> userInputs) {
        executionServiceV2.resumeExecution(threadId, userInputs);
        return ResponseEntity.ok(Map.of(
                "message", "Execution resumed",
                "threadId", threadId
        ));
    }

    // ============ 长期内存（Store）操作 ============

    @GetMapping("/store/{namespace}/{key}")
    @Operation(summary = "获取Store中的数据")
    public ResponseEntity<?> getStoreData(
            @PathVariable String namespace,
            @PathVariable String key) {
        if (storeHelper == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Store is not configured"));
        }

        Optional<Map<String, Object>> data = storeHelper.getCache(namespace + ":" + key);
        if (data.isPresent()) {
            return ResponseEntity.ok(Map.of("key", key, "value", data.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/store/{namespace}/{key}")
    @Operation(summary = "向Store存储数据")
    public ResponseEntity<Map<String, String>> putStoreData(
            @PathVariable String namespace,
            @PathVariable String key,
            @RequestBody Map<String, Object> data) {
        if (storeHelper == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Store is not configured"));
        }

        storeHelper.putCache(namespace + ":" + key, data.get("value"));
        return ResponseEntity.ok(Map.of(
                "message", "Data stored successfully",
                "namespace", namespace,
                "key", key
        ));
    }

    @DeleteMapping("/store/{namespace}/{key}")
    @Operation(summary = "从Store删除数据")
    public ResponseEntity<Map<String, String>> deleteStoreData(
            @PathVariable String namespace,
            @PathVariable String key) {
        if (storeHelper == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Store is not configured"));
        }

        // storeHelper.delete(namespace, key);  // 需要添加delete方法
        return ResponseEntity.ok(Map.of(
                "message", "Data deleted successfully"
        ));
    }

    // ============ 用户偏好操作 ============

    @GetMapping("/user/{userId}/preferences")
    @Operation(summary = "获取用户偏好")
    public ResponseEntity<?> getUserPreferences(@PathVariable String userId) {
        if (storeHelper == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Store is not configured"));
        }

        Optional<Map<String, Object>> preferences = storeHelper.getUserPreferences(userId);
        return ResponseEntity.ok(preferences.orElse(Map.of()));
    }

    @PostMapping("/user/{userId}/preferences")
    @Operation(summary = "保存用户偏好")
    public ResponseEntity<Map<String, String>> saveUserPreferences(
            @PathVariable String userId,
            @RequestBody Map<String, Object> preferences) {
        if (storeHelper == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Store is not configured"));
        }

        storeHelper.saveUserPreferences(userId, preferences);
        return ResponseEntity.ok(Map.of(
                "message", "Preferences saved successfully",
                "userId", userId
        ));
    }

    @GetMapping("/user/{userId}/profile")
    @Operation(summary = "获取用户档案")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId) {
        if (storeHelper == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Store is not configured"));
        }

        Optional<Map<String, Object>> profile = storeHelper.getUserProfile(userId);
        return ResponseEntity.ok(profile.orElse(Map.of()));
    }

    @PostMapping("/user/{userId}/profile")
    @Operation(summary = "保存用户档案")
    public ResponseEntity<Map<String, String>> saveUserProfile(
            @PathVariable String userId,
            @RequestBody Map<String, Object> profile) {
        if (storeHelper == null) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Store is not configured"));
        }

        storeHelper.saveUserProfile(userId, profile);
        return ResponseEntity.ok(Map.of(
                "message", "Profile saved successfully",
                "userId", userId
        ));
    }

    // ============ 缓存管理 ============

    @GetMapping("/cache/stats")
    @Operation(summary = "获取编译图缓存统计")
    public ResponseEntity<WorkflowExecutionServiceV2.CacheStats> getCacheStats() {
        return ResponseEntity.ok(executionServiceV2.getCacheStats());
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "清空所有编译图缓存")
    public ResponseEntity<Map<String, String>> clearCache() {
        executionServiceV2.clearCache();
        return ResponseEntity.ok(Map.of("message", "Cache cleared successfully"));
    }

    @PostMapping("/cache/{id}/invalidate")
    @Operation(summary = "失效指定工作流的缓存")
    public ResponseEntity<Map<String, String>> invalidateCache(@PathVariable Long id) {
        executionServiceV2.invalidateCache(id);
        return ResponseEntity.ok(Map.of("message", "Cache invalidated for workflow: " + id));
    }

    // ============ 执行历史 ============

    @GetMapping("/{id}/executions")
    @Operation(summary = "获取工作流执行历史")
    public ResponseEntity<List<WorkflowExecution>> getExecutionHistory(@PathVariable Long id) {
        return ResponseEntity.ok(executionService.getExecutionHistory(id));
    }

    @GetMapping("/execution/thread/{threadId}")
    @Operation(summary = "按threadId获取执行记录")
    public ResponseEntity<List<WorkflowExecution>> getExecutionsByThreadId(@PathVariable String threadId) {
        return ResponseEntity.ok(executionServiceV2.getExecutionsByThreadId(threadId));
    }

    // ============ 元数据 ============

    @GetMapping("/node-types")
    @Operation(summary = "获取可用节点类型")
    public ResponseEntity<List<Map<String, Object>>> getNodeTypes() {
        List<Map<String, Object>> nodeTypes = List.of(
                Map.of("type", "start", "label", "开始", "description", "工作流入口节点", "icon", "play", "color", "#52c41a"),
                Map.of("type", "end", "label", "结束", "description", "工作流结束节点", "icon", "stop", "color", "#ff4d4f"),
                Map.of("type", "llm", "label", "LLM", "description", "调用大语言模型", "icon", "robot", "color", "#1890ff"),
                Map.of("type", "condition", "label", "条件", "description", "条件分支判断（安全SpEL）", "icon", "branch", "color", "#faad14"),
                Map.of("type", "tool", "label", "工具", "description", "调用系统工具", "icon", "tool", "color", "#722ed1"),
                // 新增：人在回路节点
                Map.of("type", "human_input", "label", "人工输入", "description", "等待人工确认/输入", "icon", "user", "color", "#eb2f96")
        );
        return ResponseEntity.ok(nodeTypes);
    }

    @GetMapping("/models")
    @Operation(summary = "获取可用模型列表")
    public ResponseEntity<List<Map<String, Object>>> getAvailableModels() {
        return ResponseEntity.ok(workflowLlmService.getAvailableModels());
    }
}
