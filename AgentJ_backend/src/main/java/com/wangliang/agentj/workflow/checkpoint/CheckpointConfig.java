package com.wangliang.agentj.workflow.checkpoint;

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工作流检查点器配置
 *
 * 根据官方文档实现检查点支持：
 * - MemorySaver: 内存存储（默认）
 *
 * @author AgentJ
 * @see <a href="https://java2ai.com/en/docs/frameworks/graph-core/core/persistence">官方持久化文档</a>
 */
@Slf4j
@Configuration
public class CheckpointConfig {

    /**
     * 默认检查点器配置
     * 使用MemorySaver作为默认实现
     *
     * 获取状态示例：
     * <pre>
     * RunnableConfig config = RunnableConfig.builder().threadId("unique-id").build();
     * StateSnapshot state = graph.getState(config);
     * List<StateSnapshot> history = graph.getStateHistory(config);
     * </pre>
     */
    @Bean
    public SaverConfig defaultSaverConfig() {
        log.info("Initializing default CheckpointSaver with MemorySaver");
        return SaverConfig.builder()
                .register(new MemorySaver())
                .build();
    }

    /**
     * Redis检查点器配置（可选）
     * 当配置 agentj.workflow.checkpoint.type=redis 时启用
     *
     * 注意：需要添加 Redis 依赖和配置
     */
    @Configuration
    @ConditionalOnProperty(name = "agentj.workflow.checkpoint.type", havingValue = "redis")
    public static class RedisCheckpointConfiguration {

        // Redis检查点器将在后续实现
        // 目前使用 MemorySaver 即可满足基本需求
    }
}
