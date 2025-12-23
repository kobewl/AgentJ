# AgentJ

<div align="center">

![AgentJ Logo](https://img.shields.io/badge/AgentJ-AI%20Platform-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-green)
![Vue 3](https://img.shields.io/badge/Vue-3.4.37-brightgreen)
![TypeScript](https://img.shields.io/badge/TypeScript-5.5.4-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

**一个功能强大的AI智能体工作台平台**

[快速开始](#快速开始) · [功能特性](#功能特性) · [架构设计](#架构设计) · [文档](#文档)

</div>

## 项目简介

AgentJ 是一个基于 Spring AI 和 Vue 3 的 AI 智能体工作台平台，集成了对话管理、知识库检索、工作流编排、计划模板、定时任务等核心功能，支持多种大模型接入和 RAG（检索增强生成）能力。

## 功能特性

### 核心功能

- 🤖 **AI 对话系统** - 支持流式对话、多会话管理、Markdown 渲染和代码高亮
- 📚 **知识库管理** - 支持文档上传、向量化存储、智能检索和问答
- 🔄 **工作流设计器** - 可视化工作流编排，支持节点拖拽、连线编辑
- 📋 **计划模板** - 预定义执行计划，支持参数映射和版本管理
- ⏰ **定时任务** - 动态 Cron 任务调度，支持任务状态监控
- 💾 **记忆管理** - 用户个人记忆存储，支持长期记忆和上下文关联
- 🔧 **配置中心** - 统一配置管理，支持热更新和参数验证
- 🎯 **统一代理** - 多代理协作，支持工具调用和执行计划

### 技术特性

- 🔍 **RAG 系统** - 基于 Qdrant 的向量检索，支持多种文档格式
- 🌐 **多模型支持** - 集成 DashScope（通义千问）、支持自定义模型
- 📊 **执行追踪** - 完整的执行记录和步骤可视化
- 🔐 **身份认证** - JWT Token 认证，支持权限控制
- 📱 **响应式设计** - 移动端适配，支持深色模式
- ♿ **无障碍优化** - ARIA 属性、键盘导航、跳转链接

## 架构设计

### 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 3)                       │
├─────────────────────────────────────────────────────────────┤
│  ChatDialog  │  WorkflowDesigner  │  KnowledgeManage        │
│  UnifiedAgent│  PlanTemplates     │  CronTasks              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      后端层 (Spring Boot)                   │
├─────────────────────────────────────────────────────────────┤
│  Agent       │  Conversation  │  Knowledge  │  Planning    │
│  Runtime     │  Service       │  Service    │  Service      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        数据层                                │
├─────────────────────────────────────────────────────────────┤
│  MySQL/PostgreSQL  │  Qdrant  │  H2  │  File System        │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈

#### 后端技术栈

- **框架**: Spring Boot 3.5.8
- **语言**: Java 21
- **AI 框架**: Spring AI 1.1.0-M4
- **大模型**: DashScope（通义千问）
- **向量数据库**: Qdrant
- **ORM**: MyBatis Plus 3.5.9
- **API 文档**: Knife4j 4.4.0
- **工具库**: Hutool 5.8.38, Lombok 1.18.42

#### 前端技术栈

- **框架**: Vue 3.4.37
- **语言**: TypeScript 5.5.4
- **构建工具**: Vite 5.4.10
- **UI 组件**: Element Plus 2.9.2
- **路由**: Vue Router 4.3.2
- **HTTP 客户端**: Axios 1.7.7
- **Markdown**: Marked 17.0.1
- **代码高亮**: Highlight.js 11.11.1
- **工作流**: Vue Flow 1.48.1

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- Maven 3.6+
- MySQL 8.0+ / PostgreSQL 12+
- Qdrant 1.7+

### 后端启动

1. **克隆项目**

```bash
git clone https://github.com/yourusername/AgentJ.git
cd AgentJ/AgentJ_backend
```

2. **配置数据库**

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE agentj CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **配置应用**

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agentj
    username: root
    password: your_password
  ai:
    dashscope:
      api-key: your_dashscope_api_key
      embedding:
        model: text-embedding-v4
      chat:
        options:
          model: qwen-flash
  vectorstore:
    qdrant:
      host: localhost
      port: 6334
      collection-name: knowledge_base
```

4. **启动 Qdrant**

```bash
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant:latest
```

5. **启动应用**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 前端启动

1. **进入前端目录**

```bash
cd AgentJ/AgentJ_frontend
```

2. **安装依赖**

```bash
npm install
```

3. **配置 API 地址**

编辑 `.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080
```

4. **启动开发服务器**

```bash
npm run dev
```

5. **访问应用**

打开浏览器访问 `http://localhost:5173`

### 生产构建

#### 后端打包

```bash
cd AgentJ_backend
./mvnw clean package -DskipTests
java -jar target/AgentJ_backend-0.0.1-SNAPSHOT.jar
```

#### 前端打包

```bash
cd AgentJ_frontend
npm run build
```

## 项目结构

```
AgentJ/
├── AgentJ_backend/              # 后端项目
│   ├── src/main/java/
│   │   └── com/wangliang/agentj/
│   │       ├── agent/          # 智能体核心
│   │       ├── auth/           # 身份认证
│   │       ├── config/         # 配置管理
│   │       ├── conversation/   # 对话管理
│   │       ├── cron/           # 定时任务
│   │       ├── knowledge/      # 知识库
│   │       ├── llm/            # 大模型服务
│   │       ├── planning/       # 计划模板
│   │       ├── rag/            # RAG 系统
│   │       ├── runtime/        # 运行时执行
│   │       └── mcp/            # MCP 服务
│   ├── src/main/resources/
│   │   ├── application.yml     # 主配置文件
│   │   ├── application-dev.yml # 开发环境配置
│   │   └── document/           # 知识库文档
│   └── sql/                    # 数据库脚本
└── AgentJ_frontend/            # 前端项目
    ├── src/
    │   ├── api/                # API 接口
    │   ├── components/         # 公共组件
    │   ├── layouts/            # 布局组件
    │   ├── pages/              # 页面组件
    │   ├── router/             # 路由配置
    │   ├── styles/             # 全局样式
    │   └── utils/              # 工具函数
    ├── index.html
    ├── package.json
    └── vite.config.ts
```

## 核心模块说明

### RAG 系统

AgentJ 集成了完整的 RAG（检索增强生成）系统，支持：

- **文档加载**: 支持多种文档格式（Markdown、PDF、Word 等）
- **向量化存储**: 基于 Qdrant 的向量数据库
- **智能检索**: 支持相似度检索和关键词检索
- **质量评估**: 多维度文档质量评估
- **查询增强**: 查询重写、关键词提取、意图识别

详细文档请参考 [RAG 系统文档](AgentJ_backend/docs/rag-overview.md)

### 工作流设计器

可视化工作流编排工具，支持：

- 节点拖拽和连线
- 多种节点类型（开始、结束、工具、条件等）
- 工作流执行和监控
- 执行步骤可视化

### 知识库管理

- 知识库创建和管理
- 文档上传和解析
- 向量化存储和检索
- 基于知识库的智能问答

## 配置说明

### 后端配置

主要配置文件位于 `src/main/resources/`：

- `application.yml`: 主配置文件
- `application-dev.yml`: 开发环境配置
- `application-prod.yml`: 生产环境配置

### 前端配置

主要配置文件：

- `vite.config.ts`: Vite 构建配置
- `tsconfig.json`: TypeScript 配置
- `.env.development`: 开发环境变量
- `.env.production`: 生产环境变量

## API 文档

启动后端服务后，访问以下地址查看 API 文档：

- Swagger UI: `http://localhost:8080/doc.html`
- Knife4j UI: `http://localhost:8080/doc.html`

## 开发指南

### 添加新的智能体

1. 在 `agent/` 目录下创建新的智能体类
2. 继承 `BaseAgent` 或实现相应接口
3. 在配置文件中注册智能体
4. 在前端添加对应的 UI 组件

### 添加新的工具

1. 在 `agent/model/Tool.java` 中定义工具接口
2. 实现工具逻辑
3. 在智能体中注册工具
4. 在工作流设计器中添加工具节点

### 添加新的页面

1. 在 `src/pages/` 下创建新的 Vue 组件
2. 在 `router/index.ts` 中添加路由
3. 在 `layouts/AppLayout.vue` 中添加菜单项

## 测试

### 后端测试

```bash
cd AgentJ_backend
./mvnw test
```

### 前端测试

```bash
cd AgentJ_frontend
npm run test
```

## 部署

### Docker 部署

```bash
# 构建后端镜像
cd AgentJ_backend
docker build -t agentj-backend .

# 构建前端镜像
cd AgentJ_frontend
docker build -t agentj-frontend .

# 启动服务
docker-compose up -d
```

### 传统部署

参考 [生产构建](#生产构建) 章节。

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- 项目主页: [https://github.com/yourusername/AgentJ](https://github.com/yourusername/AgentJ)
- 问题反馈: [https://github.com/yourusername/AgentJ/issues](https://github.com/yourusername/AgentJ/issues)
- 邮箱: your.email@example.com

## 致谢

感谢以下开源项目：

- [Spring AI](https://spring.io/projects/spring-ai)
- [Vue 3](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [Qdrant](https://qdrant.tech/)
- [DashScope](https://dashscope.aliyun.com/)

---

<div align="center">

**如果这个项目对您有帮助，请给我们一个 Star ⭐**

Made with ❤️ by AgentJ Team

</div>
