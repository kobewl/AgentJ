# ReActAgent 完全解析

> 本文档详细分析 AgentJ 智能体体系中 ReActAgent 的设计思想和实现细节

## 📋 目录

- [1. ReAct 模式概述](#1-react-模式概述)
- [2. 类设计定位](#2-类设计定位)
- [3. 核心思想](#3-核心思想)
- [4. 代码结构分析](#4-代码结构分析)
- [5. 方法详解](#5-方法详解)
- [6. 执行流程](#6-执行流程)
- [7. 与 BaseAgent 的关系](#7-与-baseagent-的关系)
- [8. 与 DynamicAgent 的关系](#8-与-dynamicagent-的关系)
- [9. 设计模式分析](#9-设计模式分析)
- [10. 实战案例](#10-实战案例)
- [11. 学习总结](#11-学习总结)

---

## 1. ReAct 模式概述

### 1.1 什么是 ReAct？

**ReAct** = **Re**asoning（推理）+ **Act**ing（行动）

这是一种源自人工智能研究的智能体模式，最早由论文 *ReAct: Synergizing Reasoning and Acting in Language Models* 提出。

**核心思想**：
```
智能体不应该直接行动，而应该：
1. 先思考（Reasoning）：分析当前情况，决定下一步该做什么
2. 再行动（Acting）：执行思考后的操作
3. 循环迭代：观察结果 → 再次思考 → 再次行动...
```

### 1.2 ReAct vs 传统模式对比

| 模式 | 流程 | 优点 | 缺点 |
|------|------|------|------|
| **直接行动** | 输入 → 行动 → 输出 | 简单快速 | 缺乏思考，容易出错 |
| **计划-执行** | 制定完整计划 → 执行计划 | 有全局观 | 计划可能不适应变化 |
| **ReAct 模式** | 思考 → 行动 → 观察 → 思考 → 行动... | 灵活适应，可纠错 | 需要多轮交互 |

### 1.3 ReAct 在 AI 智能体中的应用

**经典场景**：
```
用户：帮我查询北京今天的天气

ReAct 循环：
┌─────────────────────────────────────┐
│ 第 1 轮                             │
│ 思考：用户要查天气，我需要调用天气API│
│ 行动：调用天气查询工具               │
│ 结果：北京今天晴，温度 15-25°C       │
└─────────────────────────────────────┘
           ↓
┌─────────────────────────────────────┐
│ 第 2 轮                             │
│ 思考：已获得天气信息，任务完成       │
│ 行动：调用终止工具                   │
│ 结果：返回给用户                     │
└─────────────────────────────────────┘
```

---

## 2. 类设计定位

### 2.1 继承关系

```java
public abstract class ReActAgent extends BaseAgent
```

**类层次结构**：
```
BaseAgent (抽象基类)
    ↓
ReActAgent (ReAct 模式抽象层)
    ↓
DynamicAgent (具体实现层)
```

**职责划分**：

| 类 | 职责 | 抽象程度 |
|---|------|---------|
| **BaseAgent** | 定义生命周期、资源管理、执行控制 | 高度抽象 |
| **ReActAgent** | 定义思考-行动循环模式 | 中度抽象 |
| **DynamicAgent** | 实现具体的工具调用逻辑 | 具体实现 |

### 2.2 类的作用

ReActAgent 是一个**中间抽象层**，它的作用是：

1. **定义执行模式**：将 BaseAgent 的 `step()` 拆分为 `think()` 和 `act()`
2. **简化子类实现**：子类只需关注"如何思考"和"如何行动"
3. **提供标准流程**：思考 → 判断 → 行动的标准循环

**设计优势**：
- ✅ 代码更清晰：将推理和行动分离
- ✅ 更易扩展：子类可以独立修改思考或行动逻辑
- ✅ 更易测试：可以单独测试思考逻辑和行动逻辑

---

## 3. 核心思想

### 3.1 思考-行动循环

```java
while (执行未完成) {
    // 1. 思考：分析当前情况
    if (需要行动) {
        // 2. 行动：执行操作
        执行操作();
    } else {
        // 无需行动，继续下一轮
    }
}
```

### 3.2 思考的目的

**think() 方法应该回答**：
- 当前任务完成了吗？
- 需要调用工具吗？
- 需要调用哪个工具？
- 工具的参数是什么？

**示例**：
```java
@Override
protected boolean think() {
    // 1. 收集环境信息
    collectAndSetEnvDataForTools();

    // 2. 调用 LLM 进行推理
    Message systemMessage = getThinkMessage();
    Message currentStepEnvMessage = currentStepEnvMessage();
    List<Message> historyMem = getAgentMemory();

    // 3. 调用 LLM
    ChatResponse response = chatClient.prompt()
        .messages(systemMessage, currentStepEnvMessage, historyMem)
        .toolCallbacks(getToolCallList())
        .call()
        .chatResponse();

    // 4. 判断是否需要行动
    List<ToolCall> toolCalls = response.getToolCalls();
    return !toolCalls.isEmpty();  // 有工具调用则需要行动
}
```

### 3.3 行动的目的

**act() 方法应该执行**：
- 调用 LLM 选择的工具
- 处理工具返回结果
- 更新智能体状态
- 返回执行结果

**示例**：
```java
@Override
protected AgentExecResult act() {
    // 1. 获取 LLM 选择的工具
    List<ToolCall> toolCalls = streamResult.getEffectiveToolCalls();

    // 2. 执行工具
    if (toolCalls.size() == 1) {
        return processSingleTool(toolCalls.get(0));
    } else {
        return processMultipleTools(toolCalls);
    }
}
```

---

## 4. 代码结构分析

### 4.1 完整代码（仅 82 行）

```java
package com.wangliang.agentj.agent;

import com.wangliang.agentj.config.LynxeProperties;
import com.wangliang.agentj.llm.LlmService;
import com.wangliang.agentj.recorder.service.PlanExecutionRecorder;
import com.wangliang.agentj.runtime.entity.vo.ExecutionStep;
import com.wangliang.agentj.runtime.service.PlanIdDispatcher;
import com.wangliang.agentj.runtime.service.TaskInterruptionCheckerService;

import java.util.Map;

/**
 * Base class for ReAct (Reasoning + Acting) pattern agents.
 * Implements an agent pattern where thinking (Reasoning) and acting (Acting)
 * are executed alternately.
 */
public abstract class ReActAgent extends BaseAgent {

    /**
     * Constructor
     */
    public ReActAgent(
        LlmService llmService,
        PlanExecutionRecorder planExecutionRecorder,
        LynxeProperties lynxeProperties,
        Map<String, Object> initialAgentSetting,
        ExecutionStep step,
        PlanIdDispatcher planIdDispatcher
    ) {
        super(llmService, planExecutionRecorder, lynxeProperties,
              initialAgentSetting, step, planIdDispatcher);
    }

    /**
     * Execute thinking process and determine whether action needs to be taken
     * @return true indicates action execution is needed
     */
    protected abstract boolean think();

    /**
     * Execute specific actions
     * @return description of action execution results
     */
    protected abstract AgentExecResult act();

    /**
     * Execute a complete think-act step
     */
    @Override
    public AgentExecResult step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return new AgentExecResult(
                    "Thinking complete - no action needed",
                    AgentState.IN_PROGRESS
                );
            }
            return act();
        }
        catch (TaskInterruptionCheckerService.TaskInterruptedException e) {
            return new AgentExecResult(
                "Agent execution interrupted: " + e.getMessage(),
                AgentState.INTERRUPTED
            );
        }
    }
}
```

### 4.2 代码精简的原因

ReActAgent 只有 82 行，但功能强大，原因是：

1. **继承 BaseAgent**：复用了大量基础能力
   - 生命周期管理（run() 方法）
   - 状态管理
   - 资源清理
   - 异常处理

2. **只关注核心逻辑**：只定义了思考-行动模式

3. **委托给子类**：具体的思考、行动逻辑由 DynamicAgent 实现

**设计哲学**：**简单即是美**

---

## 5. 方法详解

### 5.1 构造函数（第 25-29 行）

```java
public ReActAgent(
    LlmService llmService,
    PlanExecutionRecorder planExecutionRecorder,
    LynxeProperties lynxeProperties,
    Map<String, Object> initialAgentSetting,
    ExecutionStep step,
    PlanIdDispatcher planIdDispatcher
) {
    super(llmService, planExecutionRecorder, lynxeProperties,
          initialAgentSetting, step, planIdDispatcher);
}
```

**特点**：
- ✅ 简单的参数转发
- ✅ 没有额外的初始化逻辑
- ✅ 所有依赖都交给父类管理

**为什么这样设计？**
- ReActAgent 不需要维护额外状态
- 避免重复代码
- 保持类的简洁性

---

### 5.2 `think()` 方法（第 43 行）

```java
protected abstract boolean think();
```

**方法签名分析**：
- **访问级别**：`protected`（子类可见）
- **返回类型**：`boolean`（是否需要行动）
- **抽象方法**：子类必须实现

**返回值含义**：

| 返回值 | 含义 | 后续行为 |
|--------|------|----------|
| `true` | 需要行动 | 调用 `act()` 方法 |
| `false` | 无需行动 | 返回 IN_PROGRESS，继续下一轮 |

**子类实现要求**（根据注释）：

1. **分析当前状态和上下文**
   ```java
   // 收集工具状态
   collectAndSetEnvDataForTools();

   // 获取历史记忆
   List<Message> history = getAgentMemory();
   ```

2. **执行逻辑推理决定下一步行动**
   ```java
   // 调用 LLM 进行推理
   ChatResponse response = chatClient.prompt()
       .messages(systemMessage, userMessage, history)
       .toolCallbacks(toolCallbacks)
       .call()
       .chatResponse();

   // 判断是否选择了工具
   return !response.getToolCalls().isEmpty();
   ```

3. **返回是否需要执行行动**
   ```java
   // 如果 LLM 选择了工具，返回 true
   // 否则返回 false
   ```

---

### 5.3 `act()` 方法（第 56 行）

```java
protected abstract AgentExecResult act();
```

**方法签名分析**：
- **访问级别**：`protected`（子类可见）
- **返回类型**：`AgentExecResult`（执行结果和状态）
- **抽象方法**：子类必须实现

**返回值含义**：
```java
AgentExecResult {
    String result;      // 执行结果描述
    AgentState state;   // 执行后的状态
    List<AgentExecResult> results;  // 历史结果（可选）
}
```

**子类实现要求**（根据注释）：

1. **基于 think() 的决策执行具体操作**
   ```java
   // 获取 think() 中选择的工具
   List<ToolCall> toolCalls = getSelectedToolCalls();
   ```

2. **可以是工具调用、状态更新或其他行为**
   ```java
   // 执行工具调用
   ToolExecutionResult result = toolCallingManager
       .executeToolCalls(prompt, response);
   ```

3. **返回执行结果的描述**
   ```java
   return new AgentExecResult(
       result.getOutput(),
       shouldTerminate ? AgentState.COMPLETED : AgentState.IN_PROGRESS
   );
   ```

---

### 5.4 `step()` 方法（第 64-79 行）⭐ 核心方法

这是 **ReActAgent 最核心**的方法，实现了完整的思考-行动循环。

```java
@Override
public AgentExecResult step() {
    try {
        // 1. 思考阶段
        boolean shouldAct = think();

        // 2. 判断是否需要行动
        if (!shouldAct) {
            AgentExecResult result = new AgentExecResult(
                "Thinking complete - no action needed",
                AgentState.IN_PROGRESS
            );
            return result;
        }

        // 3. 行动阶段
        return act();

    }
    catch (TaskInterruptionCheckerService.TaskInterruptedException e) {
        // 4. 处理中断异常
        return new AgentExecResult(
            "Agent execution interrupted: " + e.getMessage(),
            AgentState.INTERRUPTED
        );
    }
}
```

**流程图**：
```
step() 开始
    ↓
try 块
    ↓
think() ───────────────────┐
    ↓                      │
    ├─→ true              │
    │   ↓                 │
    │   act()             │
    │   ↓                 │
    │   返回结果           │
    │                      │
    └─→ false             │
        ↓                 │
        返回 IN_PROGRESS   │
                           │
catch (TaskInterruptedException)│
    ↓                      │
    返回 INTERRUPTED       │
                           │
结束 ←────────────────────┘
```

**代码分析**：

#### 阶段 1：思考（第 66 行）
```java
boolean shouldAct = think();
```

**作用**：
- 调用子类实现的 `think()` 方法
- 让 LLM 分析当前情况
- 决定是否需要执行行动

**可能的返回值**：
- `true`：LLM 选择了工具，需要执行
- `false`：LLM 认为无需行动（可能任务已完成）

---

#### 阶段 2：判断（第 67-72 行）

```java
if (!shouldAct) {
    AgentExecResult result = new AgentExecResult(
        "Thinking complete - no action needed",
        AgentState.IN_PROGRESS
    );
    return result;
}
```

**作用**：
- 如果无需行动，返回 `IN_PROGRESS` 状态
- 不会终止执行，继续下一轮循环

**为什么返回 IN_PROGRESS 而不是 COMPLETED？**
- 可能有其他步骤需要执行
- 让 BaseAgent 的 `run()` 循环继续
- 由 LLM 决定何时调用 TerminateTool

---

#### 阶段 3：行动（第 73 行）

```java
return act();
```

**作用**：
- 调用子类实现的 `act()` 方法
- 执行 LLM 选择的工具
- 返回执行结果

**可能的返回状态**：
- `IN_PROGRESS`：工具执行成功，继续下一轮
- `COMPLETED`：调用了 TerminateTool，任务完成
- `FAILED`：工具执行失败

---

#### 阶段 4：异常处理（第 75-78 行）

```java
catch (TaskInterruptionCheckerService.TaskInterruptedException e) {
    return new AgentExecResult(
        "Agent execution interrupted: " + e.getMessage(),
        AgentState.INTERRUPTED
    );
}
```

**作用**：
- 捕获用户中断异常
- 返回 `INTERRUPTED` 状态
- 让 BaseAgent 停止执行并清理资源

**为什么捕获这个特定异常？**
- 用户点击"停止"按钮时抛出
- 需要优雅地停止执行
- 不能让异常导致程序崩溃

---

## 6. 执行流程

### 6.1 单步执行流程

```
BaseAgent.run() 调用 step()
    ↓
ReActAgent.step() 开始
    ↓
┌─────────────────────────────────┐
│ 1. think()                      │
│    - 调用 LLM 进行推理          │
│    - 分析当前状态               │
│    - 决定是否需要行动           │
└─────────────────────────────────┘
    ↓
    返回 boolean
    ↓
┌─────────────────────────────────┐
│ 2. 判断 shouldAct               │
│                                 │
│  if (shouldAct) {               │
│      → act()                    │
│  } else {                       │
│      → 返回 IN_PROGRESS         │
│  }                              │
└─────────────────────────────────┘
    ↓
返回 AgentExecResult
    ↓
BaseAgent 检查状态
    ↓
┌─────────────────────────────────┐
│ 3. 状态处理                     │
│                                 │
│  COMPLETED → 终止               │
│  FAILED → 终止                  │
│  INTERRUPTED → 终止             │
│  IN_PROGRESS → 继续循环         │
└─────────────────────────────────┘
```

### 6.2 多轮循环示例

**场景**：查询北京天气

```
┌────────────────────────────────────┐
│ 第 1 轮                            │
├────────────────────────────────────┤
│ think(): 需要调用天气查询工具       │
│ shouldAct = true                   │
│ act(): 调用 weatherQuery 工具       │
│ 返回: IN_PROGRESS                  │
│ 结果: "北京今天晴，15-25°C"         │
└────────────────────────────────────┘
            ↓
┌────────────────────────────────────┐
│ 第 2 轮                            │
├────────────────────────────────────┤
│ think(): 已获得天气信息，任务完成   │
│ shouldAct = false                  │
│ 返回: IN_PROGRESS                  │
│ 结果: "Thinking complete..."       │
└────────────────────────────────────┘
            ↓
┌────────────────────────────────────┐
│ 第 3 轮                            │
├────────────────────────────────────┤
│ think(): 需要终止任务               │
│ shouldAct = true                   │
│ act(): 调用 terminate 工具          │
│ 返回: COMPLETED                    │
│ 结果: "任务完成"                    │
└────────────────────────────────────┘
```

---

## 7. 与 BaseAgent 的关系

### 7.1 继承关系

```java
BaseAgent
    ↑
    |
ReActAgent extends BaseAgent
```

### 7.2 职责划分

| 方面 | BaseAgent | ReActAgent |
|------|-----------|------------|
| **生命周期** | ✅ 定义（run() 方法） | ❌ 不涉及 |
| **执行模式** | ❌ 不定义（抽象 step()） | ✅ 定义（think → act） |
| **资源管理** | ✅ 负责（finally 块） | ❌ 不涉及 |
| **异常处理** | ✅ 通用异常处理 | ✅ 中断异常处理 |
| **状态管理** | ✅ 负责转换 | ❌ 不涉及 |

### 7.3 方法覆盖关系

```java
// BaseAgent 定义
protected abstract AgentExecResult step();

// ReActAgent 实现
@Override
public AgentExecResult step() {
    // 具体实现
}
```

**为什么要覆盖 step()？**
- BaseAgent 只定义了"需要执行步骤"
- ReActAgent 定义了"如何执行步骤"（思考 → 行动）

---

## 8. 与 DynamicAgent 的关系

### 8.1 继承关系

```java
BaseAgent
    ↑
    |
ReActAgent
    ↑
    |
DynamicAgent extends ReActAgent
```

### 8.2 方法实现关系

```java
// ReActAgent 定义抽象方法
protected abstract boolean think();
protected abstract AgentExecResult act();

// DynamicAgent 实现这些方法
@Override
protected boolean think() {
    // 具体的思考逻辑（调用 LLM）
}

@Override
protected AgentExecResult act() {
    // 具体的行动逻辑（执行工具）
}
```

### 8.3 三层架构设计

```
┌─────────────────────────────────────┐
│ BaseAgent (生命周期层)               │
│ - run(): 定义执行循环                │
│ - 资源管理                          │
│ - 状态转换                          │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│ ReActAgent (模式层)                  │
│ - step(): 定义思考-行动模式          │
│ - think(): 抽象思考方法              │
│ - act(): 抽象行动方法                │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│ DynamicAgent (实现层)                │
│ - think(): 具体思考逻辑              │
│ - act(): 具体行动逻辑                │
│ - 工具调用                          │
│ - 记忆管理                          │
└─────────────────────────────────────┘
```

**设计优势**：
1. **职责分离**：每层只关注自己的职责
2. **易于扩展**：可以添加新的模式层（如 PlanAgent）
3. **易于测试**：可以单独测试每一层

---

## 9. 设计模式分析

### 9.1 模板方法模式的延续

```java
// BaseAgent 定义算法骨架
public AgentExecResult run() {
    while (currentStep < maxSteps) {
        AgentExecResult result = step();  // 调用子类实现
        // ...
    }
}

// ReActAgent 具体实现 step()
public AgentExecResult step() {
    boolean shouldAct = think();  // 再次调用子类
    return shouldAct ? act() : ...;
}

// DynamicAgent 实现 think() 和 act()
protected boolean think() { /* 具体逻辑 */ }
protected AgentExecResult act() { /* 具体逻辑 */ }
```

**多层模板方法**：
```
BaseAgent.run()
    ↓ (模板方法)
ReActAgent.step()
    ↓ (模板方法)
DynamicAgent.think() / act()
    ↓ (具体实现)
工具调用
```

---

### 9.2 策略模式

**思考策略**：
```java
protected abstract boolean think();  // 思考策略
```

**行动策略**：
```java
protected abstract AgentExecResult act();  // 行动策略
```

**不同的智能体可以有不同的策略**：
- `ToolCallAgent`：调用工具的策略
- `BrowserAgent`：操作浏览器的策略
- `DatabaseAgent`：查询数据库的策略

---

### 9.3 责任链模式

**异常处理的责任链**：
```
step()
  ↓
捕获 TaskInterruptedException
  ↓
返回 INTERRUPTED 状态
  ↓
BaseAgent.handleInterruptedExecution()
  ↓
清理资源
```

---

## 10. 实战案例

### 10.1 案例 1：简单 ReAct 智能体

**需求**：实现一个简单的 ReAct 智能体，打印思考和行动过程

```java
public class SimpleReActAgent extends ReActAgent {

    private int stepCount = 0;

    public SimpleReActAgent(
        LlmService llmService,
        PlanExecutionRecorder planExecutionRecorder,
        LynxeProperties lynxeProperties,
        Map<String, Object> initialAgentSetting,
        ExecutionStep step,
        PlanIdDispatcher planIdDispatcher
    ) {
        super(llmService, planExecutionRecorder, lynxeProperties,
              initialAgentSetting, step, planIdDispatcher);
    }

    @Override
    protected boolean think() {
        stepCount++;
        System.out.println("【思考】第 " + stepCount + " 轮：分析当前情况");
        return stepCount < 3;  // 前 2 轮需要行动
    }

    @Override
    protected AgentExecResult act() {
        System.out.println("【行动】执行操作");
        if (stepCount >= 3) {
            return new AgentExecResult("任务完成", AgentState.COMPLETED);
        }
        return new AgentExecResult("操作完成", AgentState.IN_PROGRESS);
    }

    @Override
    public String getName() {
        return "SimpleReActAgent";
    }

    @Override
    public String getDescription() {
        return "简单的 ReAct 智能体示例";
    }

    // ... 实现其他抽象方法
}
```

**执行输出**：
```
【思考】第 1 轮：分析当前情况
【行动】执行操作
【思考】第 2 轮：分析当前情况
【行动】执行操作
【思考】第 3 轮：分析当前情况
【行动】执行操作
```

---

### 10.2 案例 2：带工具调用的 ReAct 智能体

**需求**：实现一个能够调用工具的 ReAct 智能体

```java
public class ToolCallAgent extends ReActAgent {

    private List<ToolCall> selectedToolCalls;

    @Override
    protected boolean think() {
        // 1. 收集环境信息
        collectAndSetEnvDataForTools();

        // 2. 构建消息
        Message systemMessage = getThinkMessage();
        Message userMessage = getNextStepWithEnvMessage();
        List<Message> history = getAgentMemory();

        // 3. 调用 LLM
        ChatResponse response = chatClient.prompt()
            .messages(systemMessage, userMessage, history)
            .toolCallbacks(getToolCallList())
            .call()
            .chatResponse();

        // 4. 保存选择的工具
        selectedToolCalls = response.getToolCalls();

        // 5. 判断是否需要行动
        return !selectedToolCalls.isEmpty();
    }

    @Override
    protected AgentExecResult act() {
        // 执行工具调用
        ToolExecutionResult result = toolCallingManager
            .executeToolCalls(prompt, response);

        // 处理记忆
        processMemory(result);

        // 返回结果
        return new AgentExecResult(
            result.getOutput(),
            AgentState.IN_PROGRESS
        );
    }

    // ... 其他方法实现
}
```

**这个案例基本就是 DynamicAgent 的实现！**

---

## 11. 学习总结

### 11.1 ReActAgent 的核心价值

1. **定义标准模式**：将智能体的执行模式标准化为"思考-行动"
2. **简化子类实现**：子类只需实现 think() 和 act()
3. **提高可读性**：代码更符合人类思维逻辑
4. **易于扩展**：可以轻松添加新的智能体类型

### 11.2 设计亮点

| 亮点 | 说明 |
|------|------|
| **简洁性** | 仅 82 行代码，但功能完整 |
| **职责单一** | 只定义思考-行动模式 |
| **抽象适度** | 不过度抽象，也不暴露细节 |
| **异常处理** | 优雅处理用户中断 |
| **可扩展性** | 易于添加新的智能体类型 |

### 11.3 关键要点

1. **think() 返回 boolean**：表示是否需要行动
2. **act() 返回 AgentExecResult**：包含结果和状态
3. **step() 组合两者**：先思考，再行动
4. **异常处理**：捕获中断异常，返回 INTERRUPTED

### 11.4 与其他模式对比

| 模式 | 特点 | 适用场景 |
|------|------|----------|
| **ReAct** | 思考-行动循环 | 需要动态决策的任务 |
| **Plan-Execute** | 先计划后执行 | 任务明确，步骤固定 |
| **Direct** | 直接行动 | 简单任务 |

---

## 🎯 实战建议

### 学习步骤

1. **第 1 步**：理解 ReAct 模式的思想
2. **第 2 步**：阅读 ReActAgent 源码（82 行很简单）
3. **第 3 步**：实现一个简单的 ReAct 智能体
4. **第 4 步**：阅读 DynamicAgent 的实现
5. **第 5 步**：实现自己的 ReAct 智能体

### 实践项目

1. **天气查询智能体**：
   - think(): 判断是否需要调用天气 API
   - act(): 调用天气 API

2. **文件搜索智能体**：
   - think(): 判断搜索条件是否满足
   - act(): 执行文件搜索

3. **数据库查询智能体**：
   - think(): 解析自然语言查询
   - act(): 执行 SQL 查询

---

## 📚 延伸阅读

- [BaseAgent 完全解析](./BaseAgent完全解析.md)
- [DynamicAgent 完全解析](./DynamicAgent完全解析.md)
- [工具系统架构](./工具系统架构.md)
- ReAct 论文：*ReAct: Synergizing Reasoning and Acting in Language Models*

---

**总结**：ReActAgent 是一个设计精妙的中间层，它将智能体的执行模式标准化为"思考-行动"循环，为整个智能体体系提供了清晰的架构基础。虽然代码只有 82 行，但它的设计思想和影响是深远的。
