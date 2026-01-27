# AgentJ Backend

AgentJ 后端服务，基于 Spring Boot 3.5.8 + Spring AI 构建。

## 快速启动

### 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- 阿里云 DashScope API Key

### 配置步骤

1. **配置数据库**

编辑 `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agentj?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

2. **配置 AI 模型**

```yaml
spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key
      chat:
        options:
          model: qwen-flash
```

3. **启动服务**

```bash
mvn spring-boot:run
```

### 访问

- API 地址: http://localhost:8080
- Swagger 文档: http://localhost:8080/swagger-ui.html

## 模块说明

### codegen - AI 代码生成模块 ⭐

基于 Spring AI 实现的 AI 代码生成功能：

- **功能**: 根据自然语言描述生成 HTML/CSS/JS 代码
- **流式响应**: SSE 实时输出生成结果
- **可视化编辑**: iframe 内嵌 + postMessage 通信
- **对话记忆**: 上下文感知的增量修改

**相关文件**:
- `controller/CodeGenController.java` - SSE 流式接口
- `service/CodeGenService.java` - 核心生成逻辑
- `service/FileStorageService.java` - 文件存储管理
- `resources/prompts/codegen/html-system.txt` - 系统提示词

**API 接口**:
- `GET /api/codegen/generate/stream` - SSE 流式生成代码
- `POST /api/codegen/app` - 创建应用
- `GET /api/codegen/app` - 获取应用列表
- `GET /static/html/{deployKey}/*` - 访问部署的静态页面

### 其他核心模块

- **advisor/** - Spring AI 中间件配置
- **agent/** - 动态 Agent 实现
- **conversation/** - 对话管理
- **llm/** - LLM 服务封装
- **workflow/** - 工作流引擎
- **rag/** - 检索增强生成
- **knowledge/** - 知识库管理

## 依赖说明

### Spring AI

```xml
<spring-ai.version>1.1.0-M4</spring-ai.version>
<spring-ai-alibaba.version>1.1.0.0-M5</spring-ai-alibaba.version>
```

支持阿里云 DashScope 模型。

### 其他依赖

- Spring Data JPA - 数据访问
- MyBatis Plus - 增强 MyBatis
- Redis - 缓存和会话
- Knife4j - API 文档增强

## 配置项

### application.yml

```yaml
spring:
  application:
    name: AgentJ_backend
  ai:
    dashscope:
      chat:
        options:
          model: qwen-flash
    memory:
      mysql:
        enabled: true

agentj:
  codegen:
    output-dir: ./tmp/code_output
    deploy-dir: ./tmp/code_deploy
    max-apps: 1000
  workflow:
    checkpoint:
      type: redis
    store:
      type: redis
```

## 开发指南

### 添加新的代码生成类型

1. 在 `CodeGenTypeEnum` 中添加新类型
2. 创建对应的 Prompt 模板
3. 在 `CodeGenService` 中添加处理逻辑

### 自定义 Prompt 模板

将 Prompt 文件放置在 `resources/prompts/codegen/` 目录下，然后在 Service 中加载使用。

## 部署

### Docker 构建

```bash
docker build -t agentj-backend:latest .
```

### Docker 运行

```bash
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=... \
  -e SPRING_AI_DASHSCOPE_API_KEY=... \
  agentj-backend:latest
```

## 许可证

Apache License 2.0
