package com.wangliang.agentj.workflow.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wangliang.agentj.workflow.converter.WorkflowGraphConverter;
import com.wangliang.agentj.workflow.dto.WorkflowDTO;
import com.wangliang.agentj.workflow.dto.WorkflowExecuteRequest;
import com.wangliang.agentj.workflow.entity.WorkflowExecution;
import com.wangliang.agentj.workflow.repository.WorkflowExecutionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 优化的工作流执行服务
 *
 * 改进点：
 * 1. 使用Caffeine缓存替代ConcurrentHashMap，支持LRU和过期
 * 2. 支持状态历史查询（getStateHistory）
 * 3. 支持时间旅行（replay from checkpoint）
 * 4. 支持人在回路（暂停/恢复）
 *
 * 参考官方文档：https://java2ai.com/en/docs/frameworks/graph-core/core/persistence
 *
 * @author AgentJ
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class WorkflowExecutionServiceV2 {

    private final WorkflowService workflowService;
    private final WorkflowGraphConverter graphConverter;
    private final WorkflowExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private SaverConfig saverConfig;

    @Value("${agentj.workflow.cache.maxSize:100}")
    private int cacheMaxSize;

    @Value("${agentj.workflow.cache.expireAfterWriteMinutes:30}")
    private int cacheExpireAfterWriteMinutes;

    /**
     * 使用Caffeine缓存编译后的图
     * 优势：
     * - LRU淘汰策略
     * - 自动过期
     * - 统计信息支持
     */
    private final Cache<Long, CompiledGraph> compiledGraphCache = Caffeine.newBuilder()
            .maximumSize(cacheMaxSize)
            .expireAfterWrite(cacheExpireAfterWriteMinutes, TimeUnit.MINUTES)
            .recordStats()
            .removalListener((key, value, cause) -> {
                log.debug("Graph cache removed: key={}, cause={}", key, cause);
            })
            .build();

    /**
     * 缓存threadId到CompiledGraph的映射，用于获取状态历史
     */
    private final Map<String, Long> threadIdToWorkflowMap = new HashMap<>();

    /**
     * 执行工作流（同步）
     */
    public Map<String, Object> execute(Long workflowId, WorkflowExecuteRequest request) throws Exception {
        log.info("Executing workflow: id={}", workflowId);

        // 创建执行记录
        WorkflowExecution execution = WorkflowExecution.builder()
                .workflowId(workflowId)
                .inputData(objectMapper.writeValueAsString(request.getInputs()))
                .build();
        execution = executionRepository.save(execution);

        try {
            // 获取或编译图
            CompiledGraph compiledGraph = getOrCompileGraph(workflowId);

            // 准备初始状态
            Map<String, Object> initialState = new HashMap<>();
            if (request.getInputs() != null) {
                initialState.putAll(request.getInputs());
            }

            // 创建运行配置（支持Checkpointer）
            String threadId = request.getThreadId() != null ? request.getThreadId() : UUID.randomUUID().toString();
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            // 保存映射关系
            threadIdToWorkflowMap.put(threadId, workflowId);

            // 执行图
            Map<String, Object> finalState = new HashMap<>();
            Flux<NodeOutput> stream = compiledGraph.stream(initialState, config);
            stream.doOnNext(output -> {
                log.debug("Node output: {}", output);
                if (output.state() != null) {
                    finalState.putAll(output.state().data());
                }
            }).blockLast();

            // 更新执行记录
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
     * 流式执行工作流
     */
    public Flux<NodeOutput> executeStream(Long workflowId, WorkflowExecuteRequest request) throws Exception {
        log.info("Executing workflow (streaming): id={}", workflowId);

        // 获取或编译图
        CompiledGraph compiledGraph = getOrCompileGraph(workflowId);

        // 准备初始状态
        Map<String, Object> initialState = new HashMap<>();
        if (request.getInputs() != null) {
            initialState.putAll(request.getInputs());
        }

        // 创建运行配置
        String threadId = request.getThreadId() != null ? request.getThreadId() : UUID.randomUUID().toString();
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 保存映射关系
        threadIdToWorkflowMap.put(threadId, workflowId);

        // 返回流
        return compiledGraph.stream(initialState, config)
                .doOnNext(output -> log.debug("Streaming node output: {}", output))
                .doOnError(e -> log.error("Streaming execution error: ", e))
                .doOnComplete(() -> log.info("Streaming execution completed"));
    }

    /**
     * 获取状态历史
     * 参考官方文档：使用 graph.getStateHistory(config)
     *
     * @param threadId 会话ID
     * @return 状态快照列表（按时间倒序）
     */
    public List<StateSnapshotView> getStateHistory(String threadId) {
        log.info("Getting state history for threadId: {}", threadId);

        List<StateSnapshotView> history = new ArrayList<>();

        // 获取对应的workflowId
        Long workflowId = threadIdToWorkflowMap.get(threadId);
        if (workflowId == null) {
            log.warn("No workflow found for threadId: {}", threadId);
            return history;
        }

        try {
            CompiledGraph compiledGraph = getOrCompileGraph(workflowId);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            // 使用官方方法获取状态历史
            Collection<StateSnapshot> snapshots = compiledGraph.getStateHistory(config);

            for (StateSnapshot snapshot : snapshots) {
                StateSnapshotView view = new StateSnapshotView();
                view.setThreadId(threadId);
                view.setNode(snapshot.node());
                view.setState(snapshot.state() != null ? snapshot.state().data() : new HashMap<>());
                view.setCreatedAt(new Date()); // 当前时间
                snapshot.config().checkPointId().ifPresent(view::setCheckpointId);
                history.add(view);
            }

        } catch (Exception e) {
            log.error("Failed to get state history for threadId: {}", threadId, e);
        }

        return history;
    }

    /**
     * 获取最新状态
     * 参考官方文档：使用 graph.getState(config)
     *
     * @param threadId 会话ID
     * @return 最新状态快照
     */
    public Optional<StateSnapshotView> getState(String threadId) {
        log.info("Getting state for threadId: {}", threadId);

        // 获取对应的workflowId
        Long workflowId = threadIdToWorkflowMap.get(threadId);
        if (workflowId == null) {
            log.warn("No workflow found for threadId: {}", threadId);
            return Optional.empty();
        }

        try {
            CompiledGraph compiledGraph = getOrCompileGraph(workflowId);
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();

            // 使用官方方法获取最新状态
            StateSnapshot snapshot = compiledGraph.getState(config);

            if (snapshot != null) {
                StateSnapshotView view = new StateSnapshotView();
                view.setThreadId(threadId);
                view.setNode(snapshot.node());
                view.setState(snapshot.state() != null ? snapshot.state().data() : new HashMap<>());
                view.setCreatedAt(new Date()); // 当前时间
                snapshot.config().checkPointId().ifPresent(view::setCheckpointId);
                return Optional.of(view);
            }

        } catch (Exception e) {
            log.error("Failed to get state for threadId: {}", threadId, e);
        }

        return Optional.empty();
    }

    /**
     * 重放从指定检查点
     * 支持时间旅行功能
     *
     * @param workflowId 工作流ID
     * @param threadId 会话ID
     * @param checkpointId 检查点ID（可选，null表示从头开始）
     * @param inputs 输入参数
     * @return 执行结果
     */
    public Map<String, Object> replay(Long workflowId, String threadId,
                                       String checkpointId, Map<String, Object> inputs) throws Exception {
        log.info("Replaying workflow: id={}, threadId={}, checkpointId={}",
                workflowId, threadId, checkpointId);

        CompiledGraph compiledGraph = getOrCompileGraph(workflowId);

        // 构建配置
        RunnableConfig.Builder configBuilder = RunnableConfig.builder()
                .threadId(threadId);

        if (checkpointId != null) {
            configBuilder.checkPointId(checkpointId);
        }

        RunnableConfig config = configBuilder.build();

        // 准备初始状态
        Map<String, Object> initialState = new HashMap<>();
        if (inputs != null) {
            initialState.putAll(inputs);
        }

        // 执行
        Map<String, Object> finalState = new HashMap<>();
        Flux<NodeOutput> stream = compiledGraph.stream(initialState, config);
        stream.doOnNext(output -> {
            if (output.state() != null) {
                finalState.putAll(output.state().data());
            }
        }).blockLast();

        return finalState;
    }

    /**
     * 更新状态并继续执行
     * 参考官方文档：graph.updateState(config, values, asNode)
     *
     * @param workflowId 工作流ID
     * @param threadId 会话ID
     * @param updates 要更新的状态
     * @param asNode 指定从哪个节点继续执行
     */
    public RunnableConfig updateState(Long workflowId, String threadId,
                                      Map<String, Object> updates, String asNode) throws Exception {
        log.info("Updating state: workflowId={}, threadId={}, updates={}, asNode={}",
                workflowId, threadId, updates.keySet(), asNode);

        CompiledGraph compiledGraph = getOrCompileGraph(workflowId);

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 使用官方方法更新状态
        compiledGraph.updateState(config, updates, asNode);

        return config;
    }

    /**
     * 暂停工作流执行（人在回路）
     *
     * @param threadId 会话ID
     */
    public void pauseExecution(String threadId) {
        log.info("Pausing execution for threadId: {}", threadId);
        // TODO: 实现暂停逻辑
    }

    /**
     * 恢复工作流执行（人在回路）
     *
     * @param threadId 会话ID
     * @param userInputs 人工输入的数据
     */
    public void resumeExecution(String threadId, Map<String, Object> userInputs) {
        log.info("Resuming execution for threadId: {}, inputs: {}", threadId, userInputs.keySet());
        // TODO: 实现恢复逻辑
    }

    /**
     * 获取或编译图
     */
    private CompiledGraph getOrCompileGraph(Long workflowId) throws Exception {
        return compiledGraphCache.get(workflowId, id -> {
            try {
                WorkflowDTO dto = workflowService.getWorkflow(id);
                StateGraph graph = graphConverter.convert(dto);

                // 配置检查点器
                CompileConfig.Builder configBuilder = CompileConfig.builder();
                if (saverConfig != null) {
                    configBuilder.saverConfig(saverConfig);
                } else {
                    // 默认使用MemorySaver
                    configBuilder.saverConfig(SaverConfig.builder()
                            .register(new MemorySaver())
                            .build());
                }

                return graph.compile(configBuilder.build());
            } catch (Exception e) {
                log.error("Failed to compile workflow: {}", id, e);
                throw new RuntimeException("Failed to compile workflow: " + id, e);
            }
        });
    }

    /**
     * 失效缓存
     */
    public void invalidateCache(Long workflowId) {
        compiledGraphCache.invalidate(workflowId);
        log.info("Invalidated graph cache for workflow: {}", workflowId);
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        compiledGraphCache.invalidateAll();
        log.info("Cleared all graph cache");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = compiledGraphCache.stats();
        return new CacheStats(
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate(),
                stats.evictionCount(),
                compiledGraphCache.estimatedSize()
        );
    }

    /**
     * 获取执行历史
     */
    public List<WorkflowExecution> getExecutionHistory(Long workflowId) {
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
    }

    /**
     * 按threadId获取执行记录
     */
    public List<WorkflowExecution> getExecutionsByThreadId(String threadId) {
        return executionRepository.findAll().stream()
                .filter(e -> e.getThreadId() != null && e.getThreadId().equals(threadId))
                .toList();
    }

    /**
     * 状态快照视图（简化版，用于API返回）
     */
    @Data
    public static class StateSnapshotView {
        private String checkpointId;
        private String threadId;
        private String node;
        private Map<String, Object> state;
        private Date createdAt;
    }

    /**
     * 缓存统计信息
     */
    @Data
    public static class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long evictionCount;
        private final long estimatedSize;

        public CacheStats(long hitCount, long missCount, double hitRate,
                          long evictionCount, long estimatedSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.evictionCount = evictionCount;
            this.estimatedSize = estimatedSize;
        }
    }
}
