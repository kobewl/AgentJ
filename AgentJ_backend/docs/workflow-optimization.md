# AgentJ 工作流引擎优化配置示例

## 概述

本次优化根据 Spring AI Alibaba Graph 官方文档实现，解决了原系统的多个问题。

## 配置选项

### 1. 检查点器配置（application.yml）

```yaml
agentj:
  workflow:
    # 检查点器类型: memory（默认）, redis, postgresql
    checkpoint:
      type: redis

    # 长期内存Store类型: memory（默认）, redis, postgresql
    store:
      type: redis
      ttl: 86400  # Store数据过期时间（秒），默认24小时

    # 编译图缓存配置
    cache:
      maxSize: 100              # 最大缓存数量
      expireAfterWriteMinutes: 30  # 写入后过期时间（分钟）

# Redis配置（当使用redis检查点器或Store时）
spring:
  redis:
    host: localhost
    port: 6379
    password: ""
    database: 0

# PostgreSQL配置（当使用postgresql检查点器或Store时）
# spring:
#   datasource:
#     url: jdbc:postgresql://localhost:5432/agentj
#     username: postgres
#     password: password
```

### 2. Maven依赖（pom.xml）

```xml
<!-- Caffeine缓存（已包含在spring-boot-starter-cache中） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Redis支持（可选） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- PostgreSQL支持（可选） -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

## 新增功能

### 1. 持久化检查点器

- **MemorySaver**: 内存存储（默认，开发环境）
- **RedisSaver**: Redis持久化（生产环境推荐）
- **PostgreSqlSaver**: PostgreSQL持久化

### 2. 长期内存

- **MemoryStore**: 内存存储（默认）
- **RedisStore**: Redis持久化
- **PostgreSqlStore**: PostgreSQL持久化

### 3. 安全条件节点

使用SpEL替代不安全的JavaScript引擎：

```java
// 旧方式（不安全）
new ConditionNode(config)  // 使用javax.script.ScriptEngine

// 新方式（安全）
SafeConditionNode.builder()
    .nodeId("check_score")
    .expression("#score > 80")  // SpEL表达式
    .trueTarget("excellent_node")
    .falseTarget("retry_node")
    .build();
```

### 4. 状态历史与时间旅行

```java
// 获取状态历史
List<StateSnapshotView> history = executionServiceV2.getStateHistory(threadId);

// 重放到指定检查点
executionServiceV2.replay(workflowId, threadId, checkpointId, inputs);

// 更新状态
executionServiceV2.updateState(workflowId, threadId, updates, "next_node");
```

### 5. 人在回路

```java
// 暂停执行
executionServiceV2.pauseExecution(threadId);

// 恢复执行
executionServiceV2.resumeExecution(threadId, userInputs);
```

## API变更

### 新增V2 API端点

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/workflow/v2/state/{threadId}` | 获取会话当前状态 |
| GET | `/api/workflow/v2/state/{threadId}/history` | 获取状态历史 |
| POST | `/api/workflow/v2/{id}/replay` | 重放工作流 |
| POST | `/api/workflow/v2/state/{threadId}/update` | 更新状态 |
| POST | `/api/workflow/v2/execution/{threadId}/pause` | 暂停执行 |
| POST | `/api/workflow/v2/execution/{threadId}/resume` | 恢复执行 |
| GET | `/api/workflow/v2/store/{namespace}/{key}` | 获取Store数据 |
| POST | `/api/workflow/v2/store/{namespace}/{key}` | 存储Store数据 |
| GET | `/api/workflow/v2/user/{userId}/preferences` | 获取用户偏好 |
| POST | `/api/workflow/v2/user/{userId}/preferences` | 保存用户偏好 |
| GET | `/api/workflow/v2/cache/stats` | 获取缓存统计 |
| POST | `/api/workflow/v2/cache/clear` | 清空缓存 |

## 数据库变更

### PostgreSQL表结构

```sql
-- 检查点表
CREATE TABLE workflow_checkpoint (
    thread_id VARCHAR(255) NOT NULL,
    checkpoint_id VARCHAR(255) NOT NULL PRIMARY KEY,
    step INT NOT NULL,
    checkpoint_data BYTEA,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_checkpoint_thread_id ON workflow_checkpoint(thread_id);

-- Store表
CREATE TABLE workflow_store (
    id BIGSERIAL PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    key VARCHAR(255) NOT NULL,
    value BYTEA,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (namespace, key)
);
CREATE INDEX idx_store_namespace ON workflow_store(namespace);

-- workflow_execution表新增字段
ALTER TABLE workflow_execution ADD COLUMN thread_id VARCHAR(255);
ALTER TABLE workflow_execution ADD COLUMN checkpoint_id VARCHAR(255);
ALTER TABLE workflow_execution ADD COLUMN paused BOOLEAN DEFAULT FALSE;
ALTER TABLE workflow_execution ADD COLUMN waiting_for_human_input BOOLEAN DEFAULT FALSE;
```

## 使用示例

### 1. 使用Redis检查点器

```yaml
# application.yml
agentj:
  workflow:
    checkpoint:
      type: redis
spring:
  redis:
    host: localhost
    port: 6379
```

### 2. 使用长期内存存储用户偏好

```java
// 存储
storeHelper.saveUserPreferences("user_123", Map.of(
    "theme", "dark",
    "language", "zh",
    "timezone", "Asia/Shanghai"
));

// 读取
Optional<Map<String, Object>> prefs = storeHelper.getUserPreferences("user_123");
```

### 3. 时间旅行示例

```bash
# 1. 执行工作流
curl -X POST /api/workflow/v2/1/execute \
  -H "Content-Type: application/json" \
  -d '{"inputs": {"query": "test"}, "threadId": "session_123"}'

# 2. 查看状态历史
curl /api/workflow/v2/state/session_123/history

# 3. 重放到某个检查点
curl -X POST "/api/workflow/v2/1/replay?threadId=session_123&checkpointId=xxx" \
  -H "Content-Type: application/json" \
  -d '{"query": "modified_query"}'
```

## 迁移指南

1. 添加新的依赖到pom.xml
2. 配置检查点器和Store类型
3. 运行数据库迁移脚本
4. 使用新的V2 API端点
5. 逐步将ConditionNode替换为SafeConditionNode

## 注意事项

1. **Caffeine缓存需要Spring Boot 3.x**
2. **Redis检查点器需要Spring Data Redis**
3. **时间旅行功能需要检查点器支持**
4. **SpEL表达式使用#前缀引用变量**
