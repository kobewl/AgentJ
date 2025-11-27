# AgentJ RAG 设计与流程说明

面向本项目（AgentJ_backend）的 RAG（Retrieval-Augmented Generation）实现原理、流程与配置说明。

## 核心组件
- **文档读取**：`src/main/java/com/wangliang/agentj/rag/DocumentReader.java`  
  - 读取 `classpath:document` 下的 Markdown，按文件拆分段落，附加文件名元数据。
- **向量化与入库**：`src/main/java/com/wangliang/agentj/rag/DocumentVectorService.java`  
  - 注入 Spring AI 的 `VectorStore`（Qdrant 实现），应用启动时分批（<=25 条）把文档向量写入集合 `knowledge_base`。
- **向量数据库**：Qdrant  
  - 监听 gRPC 端口 `6334`（Spring AI 使用）和 HTTP 端口 `6333`（Dashboard）。
- **问答服务**：`src/main/java/com/wangliang/agentj/llm/ConversationService.java`  
  - 组合对话记忆与 RAG Advisor，实现基于知识库的问答。
- **大模型与 Embedding 提供者**：DashScope（通义系）  
  - Chat 使用 `qwen-flash`（可在配置调整）；Embedding 使用 `text-embedding-v4`，维度 1536。

## 启动与数据流
1. **读取文档**  
   - `DocumentReader.loadAllMarkdownFromDirectory()` 递归读取 `src/main/resources/document` 下的 Markdown 文件。  
   - 使用 `MarkdownDocumentReader` 切分段落，并附带元数据（例如 `filename`）。
2. **生成向量并写入 Qdrant**  
   - 应用启动后，`DocumentVectorService.loadDocumentsToVectorStore()` 被 `@PostConstruct` 调用。  
   - 读取到的文档按 25 条一批调用 `vectorStore.add(batch)`，由 `spring-ai-starter-vector-store-qdrant` 完成：  
     - 调用 DashScope Embedding API 获取向量。  
     - 写入 Qdrant 集合 `knowledge_base`，包含 payload（文本、元数据）。
3. **检索与生成**  
   - `ConversationService.init()` 创建 `RetrievalAugmentationAdvisor`，内部使用 `VectorStoreDocumentRetriever`：  
     - 配置相似度阈值 `0.50`（可调）；传入 Qdrant VectorStore。  
   - `doChatWithRag` 调用时，先基于用户问题检索相似文档，再将文档作为上下文交给 Chat 模型生成回答。  
   - 对话记忆由 `MessageWindowChatMemory` 维护，实现上下文轮次。

## 关键配置
- `src/main/resources/application.yml`（全局）和 `application-dev.yml`（本地）：
  - `spring.ai.vectorstore.qdrant.host/port/use-tls/collection-name`：Qdrant 连接信息。
  - `dashscope.api-key`：DashScope API Key。
  - `dashscope.embedding.model` / `dimension`：向量模型及维度。
  - `dashscope.embedding-store.type: qdrant`：启用 Qdrant 向量库。
- Qdrant 集合名：`knowledge_base`，维度应与 embedding 模型一致（1536）。

## 运行步骤（本地）
1. 启动 Qdrant（默认 6333 HTTP，6334 gRPC）。  
2. 配置 `application-dev.yml` 中的 DashScope API Key、Qdrant host/port。  
3. 将待入库的 Markdown 放入 `src/main/resources/document`。  
4. 启动 Spring Boot：`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`。  
   - 启动日志中可见“开始向 Qdrant 写入 X 条文档”“已写入文档批次...”。
5. 打开 Qdrant Dashboard（http://localhost:6333），集合 `knowledge_base` 应出现 points。

## 自定义与扩展
- **分批大小**：`DocumentVectorService.MAX_BATCH_SIZE`（默认 25，受 DashScope 批次限制）。  
- **相似度阈值**：`ConversationService.init()` 中 `similarityThreshold(0.50)`，可通过配置化或调参。  
- **模型切换**：`application*.yml` 的 `spring.ai.dashscope.chat.options.model` 与 `dashscope.embedding.model` 可切换到其他通义模型或维度；Qdrant 集合维度需一致。  
- **文档源**：可改造 `DocumentReader` 支持多目录、分片策略（例如按标题/小节切分），或增加元数据字段（标签、时间戳、来源 URL）。  
- **多路召回**：可以组合关键词检索（BM25）与向量检索，或对 Qdrant 使用过滤 payload 的条件查询。  
- **对话记忆**：当前使用窗口记忆，可替换为基于向量的长期记忆或持久化存储。

## 常见问题排查
- **集合为空**：检查 `document` 目录是否有 Markdown、启动日志是否执行向量写入、Qdrant 端口/鉴权是否正确。  
- **维度不匹配**：确保 DashScope embedding 维度与 Qdrant 集合配置一致。  
- **批量限制报错**：DashScope Embedding 批次上限 25，已通过分批解决；如继续报错，降低 `MAX_BATCH_SIZE`。  
- **检索不到结果**：调低相似度阈值；检查文档切分粒度与内容覆盖度。  
- **网络/鉴权问题**：确认 DashScope API Key、Qdrant API Key（若开启）正确可用。

## 代码入口快速索引
- 向量入库：`com.wangliang.agentj.rag.DocumentVectorService`  
- 文档读取：`com.wangliang.agentj.rag.DocumentReader`  
- RAG 问答：`com.wangliang.agentj.llm.ConversationService`  
- 配置：`src/main/resources/application.yml`、`application-dev.yml`
