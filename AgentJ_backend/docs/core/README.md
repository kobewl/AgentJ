# 核心技术文档

> AgentJ 项目的核心技术深度解析，适合深入研究

---

## 📚 目录结构

### [01-architecture](01-architecture/)
架构设计文档
- **[AgentJ架构设计深度解析](01-architecture/为什么AgentJ使用模板系统-架构设计深度解析.md)** - 为什么使用模板系统

### [02-agent](02-agent/)
智能体系统文档
- **[BaseAgent完全解析](02-agent/BaseAgent完全解析.md)** - 智能体基类
- **[BaseAgent学习指南](02-agent/BaseAgent学习指南.md)** - 快速上手BaseAgent
- **[ReActAgent完全解析](02-agent/ReActAgent完全解析.md)** - ReAct智能体
- **[ReActAgent学习指南](02-agent/ReActAgent学习指南.md)** - 快速上手ReActAgent
- **[DynamicAgent核心解析](02-agent/DynamicAgent核心解析.md)** - 动态智能体
- **[DynamicAgent学习指南](02-agent/DynamicAgent学习指南.md)** - 快速上手DynamicAgent

### [03-planning](03-planning/)
计划与模板系统文档
- **[PlanDraftingService核心解析](03-planning/PlanDraftingService核心解析-计划草稿服务.md)** - 计划草稿服务
- **[PlanDraftingService完整流程](03-planning/PlanDraftingService完整流程-步骤生成与执行详解.md)** - 步骤生成与执行

### [04-tools](04-tools/)
工具系统文档
- **[Tool注解与ToolCallback完全解析](04-tools/Tool注解与ToolCallback完全解析.md)** - Spring AI工具机制
- **[AbstractBaseTool基类深度解析](04-tools/AbstractBaseTool基类深度解析.md)** - 工具基类
- **[ToolCallBiFunctionDef接口深度解析](04-tools/ToolCallBiFunctionDef接口深度解析.md)** - 函数式接口
- **[PlanningFactory核心解析](04-tools/PlanningFactory核心解析-为什么需要三种工具创建方式.md)** - 三种工具创建方式
- **[工具管理机制深度解析](04-tools/工具管理机制深度解析.md)** - 工具管理核心
- **[工具环境数据收集机制](04-tools/工具环境数据收集机制.md)** - 环境数据
- **[工具类功能完整解析](04-tools/工具类功能完整解析.md)** - 工具功能详解
- **[数据库工具学习指南](04-tools/数据库工具学习指南.md)** - 数据库工具

### [05-optimization](05-optimization/)
性能优化文档
- **[智能模板选择器-优化实现](05-optimization/智能模板选择器-优化实现完成.md)** - 性能优化99.5%
- **[智能模板选择器-性能与并发分析](05-optimization/智能模板选择器-性能与并发分析.md)** - 并发与性能
- **[更智能的模板选择方案](05-optimization/更智能的模板选择方案-基于模板元数据.md)** - 设计方案
- **[智能模板选择器-实现总结](05-optimization/智能模板选择器-实现总结.md)** - 实现总结

---

## 🎓 学习路径

### 路径1: 架构理解（适合系统架构师）
1. AgentJ架构设计深度解析
2. PlanningFactory核心解析
3. 智能模板选择器-优化实现

### 路径2: 智能体深入（适合AI工程师）
1. BaseAgent完全解析
2. ReActAgent完全解析
3. DynamicAgent核心解析

### 路径3: 工具系统（适合后端工程师）
1. Tool注解与ToolCallback完全解析
2. AbstractBaseTool基类深度解析
3. 工具管理机制深度解析

---

## 📝 简历亮点

### 技术深度
- ✅ 理解Spring AI的ToolCallback机制
- ✅ 掌握ReAct、Dynamic等多种智能体模式
- ✅ 熟悉计划生成与模板系统设计
- ✅ 具备性能优化实战经验（99.5%性能提升）

### 架构能力
- ✅ 模板系统设计 - 平衡灵活性和可控性
- ✅ 工厂模式应用 - 三种工具创建方式
- ✅ 智能选择系统 - LLM驱动的模板选择

---

> **更新时间**: 2025-01-20
> **文档状态**: ✅ 已整理完成
