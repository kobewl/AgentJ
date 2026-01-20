# AgentJ 后端文档目录

> 本目录包含AgentJ后端项目的核心技术文档和学习指南

---

## 📚 文档结构

```
docs/
├── README.md                    # 本文档 - 文档总目录
├── core/                        # 核心技术文档（深度解析）
│   ├── 01-architecture/         # 架构设计
│   ├── 02-agent/                # 智能体系统
│   ├── 03-planning/             # 计划与模板
│   ├── 04-tools/                # 工具系统
│   └── 05-optimization/         # 性能优化
├── guides/                      # 学习指南（按学习路径）
│   ├── quick-start.md           # 快速入门
│   ├── agent-system.md          # 智能体系统学习
│   ├── tool-system.md           # 工具系统学习
│   └── planning-system.md       # 计划系统学习
└── archive/                     # 历史文档（已归档）
```

---

## 🎯 快速导航

### 核心技术文档（适合深入理解）

#### 架构设计
- **[AgentJ架构设计深度解析](core/01-architecture/AgentJ架构设计深度解析.md)** - 为什么使用模板系统
- **[PlanningFactory核心解析](core/04-tools/PlanningFactory核心解析-为什么需要三种工具创建方式.md)** - 三种工具创建方式

#### 智能体系统
- **[BaseAgent完全解析](core/02-agent/BaseAgent完全解析.md)** - 智能体基类
- **[ReActAgent完全解析](core/02-agent/ReActAgent完全解析.md)** - ReAct智能体
- **[DynamicAgent核心解析](core/02-agent/DynamicAgent核心解析.md)** - 动态智能体

#### 计划与模板
- **[PlanDraftingService核心解析](core/03-planning/PlanDraftingService核心解析-计划草稿服务.md)** - 计划草稿服务
- **[智能模板选择器](core/05-optimization/智能模板选择器-完全指南.md)** - 模板自动选择

#### 工具系统
- **[Tool注解与ToolCallback完全解析](core/04-tools/Tool注解与ToolCallback完全解析.md)** - Spring AI工具机制
- **[AbstractBaseTool基类深度解析](core/04-tools/AbstractBaseTool基类深度解析.md)** - 工具基类
- **[工具管理机制深度解析](core/04-tools/工具管理机制深度解析.md)** - 工具管理

### 学习指南（适合系统学习）

- **[快速入门指南](guides/quick-start.md)** - 5分钟了解AgentJ核心概念
- **[智能体系统学习](guides/agent-system.md)** - 从零掌握智能体系统
- **[工具系统学习](guides/tool-system.md)** - 工具接口、实现与管理
- **[计划系统学习](guides/planning-system.md)** - 计划生成与模板系统

### 性能优化

- **[智能模板选择器-优化实现](core/05-optimization/智能模板选择器-优化实现.md)** - 性能优化99.5%

---

## 📖 推荐阅读路径

### 路径1: 快速了解（面试准备）
1. [AgentJ架构设计深度解析](core/01-architecture/AgentJ架构设计深度解析.md) - 了解架构思想
2. [智能模板选择器-完全指南](core/05-optimization/智能模板选择器-完全指南.md) - 了解最新特性

### 路径2: 系统学习（技能提升）
1. [智能体系统学习](guides/agent-system.md) - 智能体基础
2. [工具系统学习](guides/tool-system.md) - 工具机制
3. [计划系统学习](guides/planning-system.md) - 计划与模板

### 路径3: 深入研究（技术专家）
1. [BaseAgent完全解析](core/02-agent/BaseAgent完全解析.md) - 智能体核心
2. [PlanningFactory核心解析](core/04-tools/PlanningFactory核心解析-为什么需要三种工具创建方式.md) - 工厂模式
3. [智能模板选择器-优化实现](core/05-optimization/智能模板选择器-优化实现.md) - 性能优化

---

## 🎓 简历与面试亮点

### 技术亮点

#### 1. 架构设计能力
- ✅ **模板系统设计** - 在"完全自主"和"模板引导"之间取得平衡
- ✅ **三种工具创建方式** - 静态工厂、动态创建、Spring Bean的灵活运用
- ✅ **智能模板选择** - 基于LLM的语义理解，零硬编码规则

#### 2. 性能优化能力
- ✅ **性能提升99.5%** - 从1170ms优化到5ms
- ✅ **三级缓存策略** - 规则匹配 + 结果缓存 + LLM兜底
- ✅ **并发支持** - volatile + ConcurrentHashMap + 双重检查锁

#### 3. 框架整合能力
- ✅ **Spring AI整合** - ToolCallback、ChatClient、Function Calling
- ✅ **智能体模式** - ReAct、Dynamic、Configurable三种模式
- ✅ **计划生成** - LLM驱动的执行步骤生成

### 面试常见问题

#### Q1: AgentJ为什么要使用模板系统，而不是把所有工具都给AI？
**答案要点**：
1. **可控性** - 模板限制了可用工具范围，降低AI误用工具的风险
2. **成本优化** - 引导模式下可以精准控制LLM调用次数
3. **可解释性** - 预定义步骤让执行过程更透明，用户知道AI要做什么
4. **灵活性** - 我们同时提供两种模式：autonomous（完全自主）和guided（模板引导），用户可以根据场景选择

#### Q2: 你们是怎么实现智能模板选择的？
**答案要点**：
1. **零硬编码** - 不像传统系统那样维护大量if-else规则
2. **元数据驱动** - 从模板JSON中提取description、suitableFor等元数据
3. **LLM语义理解** - 让LLM理解用户任务和模板描述，进行语义匹配
4. **性能优化** - 三级缓存（规则1ms + 缓存0ms + LLM 500ms），平均5ms响应

#### Q3: 你是如何将性能从1170ms优化到5ms的？
**答案要点**：
1. **问题分析** - 原系统每次都查数据库（170ms）+ 调用LLM（1000ms）
2. **优化方案**：
   - 模板元数据缓存（5分钟TTL）- 启动时加载，后续0ms
   - 选择结果缓存（30分钟TTL）- 相同输入直接返回
   - 快速规则匹配 - URL、淘宝、京东等常见场景1ms返回
3. **技术细节**：
   - volatile保证多线程可见性
   - ConcurrentHashMap线程安全
   - 双重检查锁避免重复刷新
4. **效果** - 99.5%任务<1ms，LLM调用减少95%

#### Q4: ToolCallback和Spring的@Tool注解有什么区别？
**答案要点**：
1. **@Tool注解** - Spring AI提供的声明式方式，自动注册为函数调用
2. **ToolCallback** - 编程式接口，更灵活，可以动态创建和管理
3. **AgentJ使用ToolCallback的原因**：
   - 需要运行时动态管理工具（selectedToolKeys白名单）
   - 需要工具分组（service_group）
   - 需要工具环境数据注入（context）
4. **互补关系** - 简单场景用@Tool，复杂场景用ToolCallback

---

## 🔍 核心概念速查

### 智能体类型

| 类型 | 特点 | 适用场景 |
|------|------|----------|
| **BaseAgent** | 智能体基类，提供基础能力 | 所有智能体的基础 |
| **ReActAgent** | ReAct循环：推理→行动→观察 | 需要推理链的任务 |
| **DynamicAgent** | 动态工具选择，Configurable配置 | 复杂多步骤任务 |
| **ConfigurableDynaAgent** | 支持selectedToolKeys白名单 | 模板引导模式 |

### 执行模式

| 模式 | 工具范围 | 步骤来源 | 适用场景 |
|------|----------|----------|----------|
| **autonomous** | 所有工具 | AI自主决定 | 探索性任务、复杂问题 |
| **guided** | selectedToolKeys限制 | 模板定义（可触发自动规划） | 明确目标的任务 |

### 工具创建方式

| 方式 | 代码示例 | 使用场景 |
|------|----------|----------|
| **静态工厂方法** | `BrowserTool::new` | 无状态工具 |
| **直接new** | `new FileBrowserTool()` | 需要依赖注入 |
| **Spring Bean** | `@Autowired ToolService` | 复杂业务逻辑 |

---

## 📝 文档贡献

### 文档规范

1. **核心技术文档** (`core/`)
   - 深度解析，包含源码分析
   - 适合技术研究
   - 包含架构图、流程图

2. **学习指南** (`guides/`)
   - 循序渐进，适合学习
   - 包含示例、练习
   - 突出重点和难点

3. **文档命名**
   - 使用清晰的标题
   - 包含版本号或日期
   - 避免使用临时文件名

### 归档策略

文档归档到 `archive/` 的条件：
- 功能已完全替代
- 问题已解决且不再相关
- 过程性的临时文档

---

## 📊 文档统计

- **核心技术文档**: 10篇
- **学习指南**: 4篇
- **归档文档**: 8篇
- **总文档数**: 22篇

---

> **最后更新**: 2025-01-20
> **维护者**: AgentJ Team
> **文档状态**: ✅ 已整理完成
