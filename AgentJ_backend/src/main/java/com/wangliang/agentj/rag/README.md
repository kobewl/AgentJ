# RAG (Retrieval-Augmented Generation) 系统

## 概述

RAG系统是一个集成了文档加载、查询增强、相关性评分、质量评估和答案生成的完整框架。该系统采用模块化设计，支持多种文件格式，提供灵活的查询处理和高质量的回答生成。

## 系统架构

### 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                    RAG System Core                        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │Document     │  │Query        │  │Relevance    │      │
│  │Loader       │  │Enhancement  │  │Scoring      │      │
│  │             │  │             │  │             │      │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘      │
│         │                │                │               │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐      │
│  │Quality      │  │Answer       │  │Statistics   │      │
│  │Assessment   │  │Generation   │  │& Monitoring │      │
│  │             │  │             │  │             │      │
│  └─────────────┘  └─────────────┘  └─────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 模块结构

```
com.wangliang.agentj.rag/
├── common/                    # 通用工具和模型
│   └── models/
│       └── Document.java      # 文档数据模型
├── documentloader/            # 文档加载器模块
│   ├── loaders/               # 文档加载器接口和实现
│   ├── parsers/               # 文档解析器接口和实现
│   ├── models/                # 文档相关模型
│   └── config/                # 文档加载配置
├── queryenhancement/          # 查询增强模块
│   ├── rewriters/             # 查询重写器
│   ├── keywordextractors/     # 关键词提取器
│   ├── intentrecognizers/     # 意图识别器
│   ├── contextexpanders/      # 上下文扩展器
│   └── models/
│       └── Query.java         # 查询数据模型
├── relevancescoring/          # 相关性评分模块
│   ├── algorithms/            # 评分算法实现
│   ├── models/                # 评分相关模型
│   ├── trainers/              # 模型训练器
│   └── evaluators/            # 算法评估器
├── qualityassessment/         # 质量评估模块
│   ├── evaluators/            # 质量评估器
│   ├── metrics/               # 质量指标
│   ├── reports/               # 评估报告
│   ├── filters/               # 质量过滤器
│   └── models/
│       └── QualityAssessmentResult.java
└── RAGSystem.java             # 系统核心接口
```

## 功能特性

### 1. 文档加载器 (Document Loader)

**功能职责**：
- 支持多种文件格式（PDF、Markdown、Word、文本等）
- 提供统一的文档加载接口
- 实现文档内容解析和标准化
- 支持批量文档处理

**主要组件**：
- `DocumentLoader`：文档加载器接口
- `DocumentParser`：文档解析器接口
- `PdfDocumentParser`：PDF文档解析实现
- `MarkdownDocumentParser`：Markdown文档解析实现
- `WordDocumentParser`：Word文档解析实现

**使用示例**：
```java
// 创建文档加载器
DocumentLoader loader = new DefaultDocumentLoader();

// 加载PDF文档
Document pdfDoc = loader.loadDocument("example.pdf");

// 加载Markdown文档
Document mdDoc = loader.loadDocument("README.md");
```

### 2. 查询增强 (Query Enhancement)

**功能职责**：
- 查询重写和优化
- 关键词提取和扩展
- 用户意图识别
- 上下文信息增强

**主要组件**：
- `QueryRewriter`：查询重写器接口
- `KeywordExtractor`：关键词提取器接口
- `IntentRecognizer`：意图识别器接口
- `Query`：查询数据模型

**使用示例**：
```java
// 创建查询增强处理器
QueryRewriter rewriter = new SynonymQueryRewriter();
KeywordExtractor extractor = new TfIdfKeywordExtractor();

// 处理用户查询
Query query = new Query("什么是机器学习");
query = rewriter.rewrite(query);
List<String> keywords = extractor.extractKeywords(query);
```

### 3. 相关性评分 (Relevance Scoring)

**功能职责**：
- 计算文档与查询的相关性
- 支持多种评分算法（BM25、余弦相似度等）
- 提供可扩展的算法框架
- 支持算法组合和权重调整

**主要组件**：
- `RelevanceScoringAlgorithm`：评分算法接口
- `BM25Algorithm`：BM25算法实现
- `CosineSimilarityAlgorithm`：余弦相似度算法实现
- `RelevanceScore`：评分结果模型

**使用示例**：
```java
// 创建评分算法
RelevanceScoringAlgorithm bm25 = new BM25Algorithm();
RelevanceScoringAlgorithm cosine = new CosineSimilarityAlgorithm();

// 计算相关性
List<Document> documents = // 获取文档列表
Query query = // 获取查询对象
List<RelevanceScore> scores = bm25.score(documents, query);
```

### 4. 质量评估 (Quality Assessment)

**功能职责**：
- 评估文档内容质量
- 提供多维度质量指标
- 支持自定义质量规则
- 生成质量评估报告

**主要组件**：
- `QualityEvaluator`：质量评估器接口
- `QualityMetric`：质量指标接口
- `ContentCompletenessMetric`：内容完整性指标
- `TextReadabilityMetric`：文本可读性指标
- `DocumentStructureMetric`：文档结构指标
- `LanguageQualityMetric`：语言质量指标
- `ContentRelevanceMetric`：内容相关性指标

**使用示例**：
```java
// 创建质量评估器
QualityEvaluator evaluator = new ComprehensiveQualityEvaluator();

// 评估文档质量
List<Document> documents = // 获取文档列表
List<QualityAssessmentResult> results = evaluator.evaluate(documents);
```

### 5. 系统核心 (RAG System Core)

**功能职责**：
- 协调各模块工作
- 提供统一的系统接口
- 管理系统配置和状态
- 收集系统统计信息

**主要组件**：
- `RAGSystem`：系统核心接口
- `DefaultRAGSystem`：默认实现类
- `RAGConfiguration`：系统配置
- `RAGSystemStatus`：系统状态
- `RAGStatistics`：系统统计

**使用示例**：
```java
// 创建RAG系统
RAGSystem ragSystem = new DefaultRAGSystem();

// 配置系统
RAGConfiguration config = new RAGConfiguration();
config.setQueryEnhancementEnabled(true);
config.setQualityAssessmentEnabled(true);
ragSystem.updateConfiguration(config);

// 执行RAG流程
String userQuery = "什么是人工智能";
String answer = ragSystem.executeRAG(userQuery, 5);
```

## 配置选项

### 系统配置 (RAGConfiguration)

```java
RAGConfiguration config = new RAGConfiguration();

// 启用/禁用功能模块
config.setQueryEnhancementEnabled(true);      // 查询增强
config.setQualityAssessmentEnabled(true);     // 质量评估
config.setCachingEnabled(true);                // 缓存机制

// 设置算法参数
config.setBm25K1(1.2);                       // BM25 k1参数
config.setBm25B(0.75);                        // BM25 b参数
config.setMaxKeywords(10);                     // 最大关键词数
config.setMinQualityScore(0.7);               // 最小质量分数
```

### 性能优化

- **缓存机制**：支持查询结果缓存，减少重复计算
- **批量处理**：支持文档批量加载和处理
- **并行计算**：支持多线程评分和质量评估
- **内存管理**：提供文档存储和缓存管理

## 扩展性

### 添加新的文档解析器

```java
public class CustomDocumentParser implements DocumentParser {
    @Override
    public Document parse(File file) {
        // 实现自定义解析逻辑
        return new Document();
    }
    
    @Override
    public boolean supports(String fileType) {
        return "custom".equals(fileType);
    }
}
```

### 添加新的评分算法

```java
public class CustomScoringAlgorithm implements RelevanceScoringAlgorithm {
    @Override
    public List<RelevanceScore> score(List<Document> documents, Query query) {
        // 实现自定义评分逻辑
        return new ArrayList<>();
    }
    
    @Override
    public String getAlgorithmName() {
        return "CustomAlgorithm";
    }
}
```

### 添加新的质量指标

```java
public class CustomQualityMetric implements QualityMetric {
    @Override
    public double calculate(Document document) {
        // 实现自定义质量计算
        return 0.0;
    }
    
    @Override
    public String getMetricName() {
        return "CustomMetric";
    }
}
```

## 错误处理

系统提供完善的错误处理机制：
- **文档加载错误**：格式不支持、文件损坏等
- **解析错误**：编码问题、结构异常等
- **评分错误**：算法异常、参数错误等
- **质量评估错误**：指标计算异常等

## 监控和统计

系统提供详细的统计信息：
- **查询统计**：总查询数、成功数、失败数
- **性能统计**：平均响应时间、吞吐量
- **质量统计**：平均质量分数、高质量文档比例
- **缓存统计**：缓存命中率、缓存大小

## 最佳实践

1. **文档预处理**：在加载前对文档进行预处理，提高解析质量
2. **查询优化**：使用查询增强功能提高检索准确性
3. **质量过滤**：设置合适的质量阈值，过滤低质量文档
4. **缓存策略**：合理配置缓存，平衡内存使用和性能
5. **算法选择**：根据业务场景选择合适的评分算法
6. **参数调优**：根据实际情况调整算法参数

## 注意事项

- 确保文档格式正确，避免解析错误
- 合理设置质量评估阈值，避免过滤过多文档
- 定期清理缓存，避免内存溢出
- 监控系统性能，及时调整配置参数
- 根据业务需求选择合适的扩展策略

## 更新日志

### v1.0.0 (2025-12-14)
- 初始版本发布
- 实现完整的RAG系统框架
- 支持多种文档格式
- 提供查询增强和质量评估功能
- 支持多种相关性评分算法