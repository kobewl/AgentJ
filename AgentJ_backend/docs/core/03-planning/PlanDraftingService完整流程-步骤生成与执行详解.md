# PlanDraftingService 完整流程 - 步骤生成与执行详解

> 回答关键问题：步骤如何生成？为什么用JSON格式？用户看到什么？

## 🔥 核心发现

### 关键误解纠正

**❌ 之前的错误理解**：
```
DynamicAgent → PlanDraftingService → 生成步骤 → 执行
```

**✅ 实际的正确流程**：
```
LynxeController → PlanDraftingService → 生成步骤 → DynamicAgent执行
```

**核心发现**：`PlanDraftingService` 不是被 `DynamicAgent` 调用的，而是被 **`LynxeController`** 在计划执行**之前**调用的！

---

## 📊 完整流程图解

### 1. 步骤生成与执行的完整时序图

```
用户发送任务请求
    ↓
LynxeController.executePlan()
    ↓
解析 planTemplateId (如 "auto-browser")
    ↓
判断: planTemplateId.startsWith("auto-") ?
    ├─ 是 → 自动规划模式
    │   ↓
    │   applyDraftedPlanIfNeeded() [LynxeController:1063-1115]
    │   ↓
    │   planDraftingService.draftPlanSteps() [生成 3-6 个文本步骤]
    │   ↓
    │   转换: List<String> → List<ExecutionStep>
    │   ↓
    │   dynamicPlan.setSteps(newSteps)
    │   ↓
    └─ 否 → 使用模板中的预定义步骤
        ↓
    [此时计划已包含完整的 ExecutionStep 列表]
        ↓
DynamicAgentExecutionPlan 执行
    ↓
DynamicAgent 逐步执行 ExecutionStep
    ↓
每一步: AI (llmService.chat()) → 选择工具 → 执行
    ↓
PlanFinalizer 生成最终响应
```

### 2. 代码位置标注

**关键调用点**: `LynxeController.java:586`
```java
// Draft a user-specific plan for supported templates before execution
applyDraftedPlanIfNeeded(plan, planTemplateId, replacementParams);
```

**步骤生成方法**: `LynxeController.java:1063-1115`
```java
private void applyDraftedPlanIfNeeded(PlanInterface plan, String planTemplateId,
        Map<String, Object> replacementParams) {
    // 1. 条件检查
    if (!planTemplateId.startsWith("auto-")) {
        return;  // 仅处理 "auto-" 模板
    }

    // 2. 提取用户输入
    String task = resolveTaskInput(replacementParams, dynamicPlan);
    String context = resolveParam(replacementParams, "context");
    String goal = resolveParam(replacementParams, "goal");

    // 3. 调用 PlanDraftingService 生成文本步骤
    List<String> draftedSteps = planDraftingService.draftPlanSteps(task, context, goal, 6);

    // 4. 转换为 ExecutionStep 对象
    for (int i = 0; i < draftedSteps.size(); i++) {
        String stepText = draftedSteps.get(i);
        String requirement = buildStepRequirement(stepText, agentTag, isLast, taskIsChinese);

        ExecutionStep step = new ExecutionStep();
        step.setStepRequirement(requirement);           // 步骤要求
        step.setAgentName(baseStep.getAgentName());     // Agent类型
        step.setModelName(baseStep.getModelName());     // 模型名称
        step.setTerminateColumns(baseStep.getTerminateColumns());
        step.setSelectedToolKeys(baseStep.getSelectedToolKeys());
        newSteps.add(step);
    }

    // 5. 替换计划中的步骤
    dynamicPlan.setSteps(newSteps);
}
```

---

## 🎯 问题1：步骤是如何生成并传递给AI的？

### 分步详解

#### Step 1: 用户发起请求
```json
POST /api/agent/executePlan
{
  "planTemplateId": "auto-browser",
  "replacementParams": {
    "task": "帮我在淘宝上搜索 iPhone 15 并截图",
    "context": "需要比较价格",
    "goal": "找到最优惠的价格"
  }
}
```

#### Step 2: LynxeController 接收并判断
```java
// LynxeController.java:586
applyDraftedPlanIfNeeded(plan, planTemplateId, replacementParams);
```

**判断条件**:
```java
if (!planTemplateId.startsWith("auto-")) {
    return;  // 不是自动模式，直接使用模板中的步骤
}
```

#### Step 3: PlanDraftingService 生成文本步骤
```java
// LynxeController.java:1082
List<String> draftedSteps = planDraftingService.draftPlanSteps(task, context, goal, 6);
```

**LLM 返回的 JSON 格式**:
```json
[
  "打开淘宝网站",
  "在搜索框输入 'iPhone 15'",
  "点击搜索按钮",
  "截取搜索结果页面",
  "分析并比较价格"
]
```

**得到 List<String>**:
```java
draftedSteps = [
  "打开淘宝网站",
  "在搜索框输入 'iPhone 15'",
  "点击搜索按钮",
  "截取搜索结果页面",
  "分析并比较价格"
]
```

#### Step 4: 转换为 ExecutionStep 对象
```java
// LynxeController.java:1094-1109
for (int i = 0; i < draftedSteps.size(); i++) {
    String stepText = draftedSteps.get(i);  // "打开淘宝网站"

    // 构建步骤要求（添加 Agent 标签）
    String requirement = buildStepRequirement(stepText, "BROWSER_AGENT", isLast, taskIsChinese);
    // 结果: "[BROWSER_AGENT] 打开淘宝网站"

    // 创建 ExecutionStep 对象
    ExecutionStep step = new ExecutionStep();
    step.setStepRequirement(requirement);     // "[BROWSER_AGENT] 打开淘宝网站"
    step.setAgentName("DynamicAgent");        // 使用模板配置的Agent
    step.setModelName("gpt-4o");             // 使用模板配置的模型
    step.setTerminateColumns(null);
    step.setSelectedToolKeys(null);
    newSteps.add(step);
}
```

**buildStepRequirement 方法详情** (LynxeController.java:1178-1193):
```java
private String buildStepRequirement(String stepText, String agentTag, boolean isLast, boolean taskIsChinese) {
    String trimmed = stepText.trim();
    String requirement;

    // 如果已经有标签，直接使用
    if (trimmed.startsWith("[") && trimmed.contains("]")) {
        requirement = trimmed;
    }
    else {
        // 添加 Agent 类型标签
        requirement = "[" + agentTag + "] " + trimmed;
        // 结果: "[BROWSER_AGENT] 打开淘宝网站"
    }

    // 最后一步自动添加 terminate 提示
    if (isLast && !requirement.toLowerCase().contains("terminate")) {
        requirement = taskIsChinese
                ? requirement + " 完成后调用 terminate。"
                : requirement + " Call terminate when finished.";
        // 结果: "[BROWSER_AGENT] 分析并比较价格 完成后调用 terminate。"
    }

    return requirement;
}
```

#### Step 5: 替换计划中的步骤
```java
// LynxeController.java:1112
dynamicPlan.setSteps(newSteps);
```

此时，`DynamicAgentExecutionPlan` 对象中包含了完整的步骤列表:
```java
DynamicAgentExecutionPlan {
    steps: [
        ExecutionStep { stepRequirement: "[BROWSER_AGENT] 打开淘宝网站" },
        ExecutionStep { stepRequirement: "[BROWSER_AGENT] 在搜索框输入 'iPhone 15'" },
        ExecutionStep { stepRequirement: "[BROWSER_AGENT] 点击搜索按钮" },
        ExecutionStep { stepRequirement: "[BROWSER_AGENT] 截取搜索结果页面" },
        ExecutionStep { stepRequirement: "[BROWSER_AGENT] 分析并比较价格 完成后调用 terminate。" }
    ]
}
```

#### Step 6: DynamicAgent 执行步骤
```java
// DynamicAgent.java
for (ExecutionStep step : plan.getSteps()) {
    String requirement = step.getStepRequirement();

    // 将步骤要求发送给 AI
    ChatResponse response = llmService.chat(
        prompt = requirement,
        tools = availableTools,
        ...
    );

    // AI 根据要求选择工具并执行
    // 例如: 调用 browser_use_tool 访问淘宝
}
```

---

## 🎯 问题2: 为什么生成JSON格式？用户看到什么步骤？

### 2.1 为什么是 JSON 格式？

#### 原因 1: LLM 的标准输出格式

**LLM 天然倾向于输出结构化数据**，JSON 是最通用的格式。

```java
// PlanDraftingService.java 的 Prompt
String systemPrompt = """
    You are a planning assistant.
    Return ONLY a JSON array of concise step strings (3-6 steps).
    No markdown, no numbering, no extra text.
    """;
```

**如果没有 JSON 格式约束**，LLM 可能返回:
```
好的，我来帮你规划这个任务：

第一步：首先打开淘宝网站
第二步：然后在搜索框输入...
第三步：接下来点击...
...
```

这种格式**难以解析**，需要复杂的正则表达式或文本处理。

**有了 JSON 格式约束**，LLM 返回:
```json
["打开淘宝网站", "在搜索框输入 'iPhone 15'", "点击搜索按钮", "截取搜索结果页面", "分析并比较价格"]
```

**一行代码即可解析**:
```java
List<String> steps = objectMapper.readValue(jsonString,
    new TypeReference<List<String>>(){});
```

#### 原因 2: 双重容错机制

**JSON 解析优先，逐行解析作为后备**:

```java
// PlanDraftingService.java:77-88
try {
    // 策略1: JSON 数组解析
    List<String> steps = objectMapper.readValue(trimmedResponse,
        new TypeReference<List<String>>(){});
    if (!steps.isEmpty()) {
        return steps;
    }
} catch (Exception e) {
    logger.warn("JSON parse failed, trying line-by-line parsing");
}

// 策略2: 逐行解析（容错）
return parseLinesAsFallback(trimmedResponse);
```

**容错示例**:
```java
// LLM 返回了带编号的文本（JSON解析失败）
"1. 打开淘宝网站\n2. 输入搜索关键词\n3. 点击搜索"

// 逐行解析成功
parseLinesAsFallback() 返回:
["打开淘宝网站", "输入搜索关键词", "点击搜索"]
```

#### 原因 3: 类型安全与验证

```java
// JSON 解析后可以进行严格的类型检查
List<String> steps = objectMapper.readValue(jsonString,
    new TypeReference<List<String>>(){});

// 验证步骤数量
int desiredMaxSteps = Math.max(3, Math.min(maxSteps, 6));
if (steps.size() > desiredMaxSteps) {
    steps = steps.subList(0, desiredMaxSteps);
}

// 验证步骤非空
for (String step : steps) {
    if (!StringUtils.hasText(step.trim())) {
        continue;  // 跳过空步骤
    }
}
```

### 2.2 用户看到什么步骤？

#### 关键区分：两种不同的步骤表示

| 维度 | 文本步骤 (List<String>) | ExecutionStep 对象 |
|------|------------------------|-------------------|
| **来源** | PlanDraftingService (LLM生成) | LynxeController 转换后 |
| **格式** | 纯文本字符串 | 结构化对象 |
| **内容** | "打开淘宝网站" | "[BROWSER_AGENT] 打开淘宝网站" |
| **生命周期** | 临时，仅用于转换 | 持久化到执行计划中 |
| **给谁看** | ❌ 用户看不到 | ✅ 用户可以看到 |

#### 用户看到的步骤

**场景1: 前端实时展示 (SSE流式输出)**

```java
// LynxeController.java:1200+
@PostMapping(value = "/taskStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamTask(@RequestBody Map<String, Object> request) {
    // 将 ExecutionStep.stepRequirement 实时推送到前端
}
```

**前端展示**:
```
当前执行步骤: [BROWSER_AGENT] 打开淘宝网站
状态: 执行中...
进度: 1/5
```

**场景2: 执行记录查询**

```java
// PlanExecutionRecorder 记录了每一步的执行详情
ExecutionStep {
    stepRequirement: "[BROWSER_AGENT] 打开淘宝网站",
    agentName: "DynamicAgent",
    modelName: "gpt-4o",
    toolCalls: [
        { toolName: "browser_use_tool", arguments: {...} }
    ],
    result: "成功打开淘宝网站"
}
```

**场景3: 最终报告生成**

```java
// PlanFinalizer.java
String finalResponse = llmService.chat(
    prompt = "根据以下执行步骤生成最终报告...",
    context = allExecutionSteps  // 包含 ExecutionStep 列表
);
```

**用户看到的报告**:
```
任务执行完成！

执行步骤:
1. ✅ [BROWSER_AGENT] 打开淘宝网站
2. ✅ [BROWSER_AGENT] 在搜索框输入 'iPhone 15'
3. ✅ [BROWSER_AGENT] 点击搜索按钮
4. ✅ [BROWSER_AGENT] 截取搜索结果页面
5. ✅ [BROWSER_AGENT] 分析并比较价格

最终结果:
找到最优惠的价格为 ¥5999...
```

---

## 🔍 关键对比：文本步骤 vs ExecutionStep

### 完整对比表

| 维度 | List<String> (文本步骤) | ExecutionStep (结构化步骤) |
|------|------------------------|---------------------------|
| **生成者** | PlanDraftingService.draftPlanSteps() | LynxeController.applyDraftedPlanIfNeeded() |
| **数据来源** | LLM 生成的 JSON 数组 | 从文本步骤转换而来 |
| **示例** | `"打开淘宝网站"` | `"[BROWSER_AGENT] 打开淘宝网站 完成后调用 terminate。"` |
| **结构** | 简单字符串 | 包含多个字段的对象 |
| **字段** | 无 | stepRequirement, agentName, modelName, terminateColumns, selectedToolKeys |
| **Agent标签** | ❌ 无 | ✅ 自动添加 `[AGENT_TYPE]` |
| **Terminate提示** | ❌ 无 | ✅ 最后一步自动添加 |
| **存储位置** | 临时变量 (局部变量) | DynamicAgentExecutionPlan.steps |
| **谁使用它** | 仅用于转换 | DynamicAgent 执行引擎 |
| **用户可见性** | ❌ 用户看不到 | ✅ 通过SSE/记录/报告展示 |
| **持久化** | ❌ 不持久化 | ✅ 保存到执行记录中 |
| **生命周期** | 毫秒级（转换用） | 任务执行全程 |

### 转换代码详解

```java
// 从文本步骤到 ExecutionStep 的完整转换

// 输入: List<String>
List<String> draftedSteps = [
    "打开淘宝网站",
    "在搜索框输入 'iPhone 15'",
    "点击搜索按钮",
    "截取搜索结果页面",
    "分析并比较价格"
];

// 转换过程
List<ExecutionStep> newSteps = new ArrayList<>();
for (int i = 0; i < draftedSteps.size(); i++) {
    String stepText = draftedSteps.get(i);  // 取出一个文本步骤

    // 1. 添加 Agent 标签
    String agentTag = resolveAgentTag(planTemplateId, baseStep);
    // 结果: "BROWSER_AGENT"

    // 2. 构建完整的要求字符串
    boolean isLast = (i == draftedSteps.size() - 1);
    String requirement = buildStepRequirement(stepText, agentTag, isLast, taskIsChinese);
    // 中间步骤: "[BROWSER_AGENT] 打开淘宝网站"
    // 最后步骤: "[BROWSER_AGENT] 分析并比较价格 完成后调用 terminate。"

    // 3. 创建 ExecutionStep 对象
    ExecutionStep step = new ExecutionStep();
    step.setStepRequirement(requirement);           // 设置步骤要求
    step.setAgentName(baseStep.getAgentName());     // 继承Agent类型
    step.setModelName(baseStep.getModelName());     // 继承模型名称
    step.setTerminateColumns(baseStep.getTerminateColumns());  // 继承终止条件
    step.setSelectedToolKeys(baseStep.getSelectedToolKeys());  // 继承工具选择

    newSteps.add(step);
}

// 输出: List<ExecutionStep>
// [
//   ExecutionStep { stepRequirement: "[BROWSER_AGENT] 打开淘宝网站" },
//   ExecutionStep { stepRequirement: "[BROWSER_AGENT] 在搜索框输入 'iPhone 15'" },
//   ExecutionStep { stepRequirement: "[BROWSER_AGENT] 点击搜索按钮" },
//   ExecutionStep { stepRequirement: "[BROWSER_AGENT] 截取搜索结果页面" },
//   ExecutionStep { stepRequirement: "[BROWSER_AGENT] 分析并比较价格 完成后调用 terminate。" }
// ]
```

---

## 💡 设计亮点与面试要点

### 1. 为什么在 Controller 层生成步骤？

**问题**：为什么不让 DynamicAgent 自己调用 PlanDraftingService？

**答案**：
1. **关注点分离**: Controller 负责**请求处理**，Agent 负责**步骤执行**
2. **执行前准备**: 步骤必须在执行**之前**完全确定
3. **计划不变性**: 一旦执行开始，步骤不应再改变

**时序对比**:

❌ **错误方式 (Agent调用)**:
```
用户请求 → DynamicAgent.start()
            → 发现没有步骤
            → 调用 PlanDraftingService
            → 生成步骤
            → 开始执行步骤 1
```
问题: Agent职责过重，执行逻辑和准备逻辑耦合

✅ **正确方式 (Controller调用)**:
```
用户请求 → LynxeController
            → 判断需要自动规划
            → 调用 PlanDraftingService
            → 生成并设置步骤
            → 传递给 DynamicAgent
            → 直接执行步骤
```
优势: 职责清晰，Agent只需关注执行

### 2. JSON格式的鲁棒性设计

**三层容错机制**:

```java
// 第一层: JSON数组解析
try {
    return objectMapper.readValue(json, List<String>.class);
} catch (Exception e) {
    // 第二层: 逐行解析
    List<String> lines = parseLinesAsFallback(json);
    if (!lines.isEmpty()) {
        return lines;
    }
}

// 第三层: 降级到默认步骤
return buildFallbackPlan(maxSteps);
```

**面试回答模板**:
> "我们设计了三层容错机制：第一层优先解析JSON数组（高效），第二层降级到逐行解析（兼容性），第三层使用默认步骤（兜底）。这样即使LLM输出格式不稳定，系统也能正常运行。"

### 3. 步骤格式化的智能设计

**自动添加 Agent 标签**:
```java
// planTemplateId → agentTag 映射
"auto-browser" → "BROWSER_AGENT"
"auto-database" → "DATABASE_AGENT"
其他 → "GENERAL_AGENT"
```

**最后一步自动添加 terminate**:
```java
if (isLast && !requirement.toLowerCase().contains("terminate")) {
    requirement += taskIsChinese
        ? " 完成后调用 terminate。"
        : " Call terminate when finished.";
}
```

**智能判断任务语言**:
```java
private boolean containsChinese(String text) {
    return text.matches(".*\\p{IsHan}+.*");  // Unicode中文字符检测
}
```

### 4. 两种步骤表示的分工

**List<String> (文本步骤)**:
- 🎯 **用途**: 临时数据传输
- 🏢 **位置**: PlanDraftingService → LynxeController
- ⏱️ **生命周期**: 毫秒级
- 👁️ **用户可见性**: 不可见

**ExecutionStep (结构化步骤)**:
- 🎯 **用途**: 正式执行对象
- 🏢 **位置**: DynamicAgentExecutionPlan
- ⏱️ **生命周期**: 任务全程
- 👁️ **用户可见性**: 可见 (SSE/记录/报告)

**面试回答模板**:
> "我们设计了两种步骤表示：文本步骤用于LLM生成和临时传输，ExecutionStep用于正式执行和持久化。文本步骤是纯文本，ExecutionStep包含Agent类型、模型名称、终止条件等完整上下文。这种分离设计既保证了LLM的灵活性，又确保了执行引擎的完整性。"

---

## 📝 简历亮点

### 亮点1: 理解Agent系统的分层架构
```
负责AI Agent系统的规划模块开发，设计了Controller层预处理、Agent层执行的
分层架构，实现了步骤生成（PlanDraftingService）与步骤执行（DynamicAgent）
的解耦，提升了代码可维护性。
```

### 亮点2: LLM输出格式化与容错
```
设计了基于JSON格式的LLM步骤生成机制，通过三层容错策略（JSON解析、
逐行解析、默认步骤）确保系统鲁棒性，即使LLM输出格式不稳定也能正常运行。
```

### 亮点3: 步骤表示的转换与增强
```
实现了从LLM生成的文本步骤（List<String>）到可执行的结构化步骤
（ExecutionStep）的自动转换，包括Agent类型标注、语言检测、终止提示
添加等智能化处理，提升了用户体验。
```

---

## ❓ 面试问题与回答

### Q1: PlanDraftingService 是谁调用的？在什么时候调用？

**A**:
```
PlanDraftingService 不是被 DynamicAgent 调用的，而是被 LynxeController 在
计划执行之前调用的。

具体流程：
1. 用户发送请求到 LynxeController.executePlan()
2. Controller 解析 planTemplateId（如 "auto-browser"）
3. 如果 planTemplateId 以 "auto-" 开头，调用 applyDraftedPlanIfNeeded()
4. 该方法内部调用 planDraftingService.draftPlanSteps() 生成步骤
5. 将生成的步骤设置到 DynamicAgentExecutionPlan 中
6. 然后才启动 DynamicAgent 执行这些步骤

关键代码位置：LynxeController.java:586 和 1063-1115
```

### Q2: 为什么步骤要生成JSON格式？

**A**:
```
主要有三个原因：

1. LLM的标准输出格式：LLM天然倾向于输出结构化数据，JSON是最通用的格式。
   有明确的格式约束后，LLM会返回标准的JSON数组，便于解析。

2. 类型安全与验证：JSON解析后可以进行严格的类型检查和数量限制，
   确保步骤在3-6个范围内，并过滤空步骤。

3. 容错机制：我们设计了双重解析策略——JSON数组解析优先，逐行解析作为
   后备。即使LLM返回的JSON格式有问题，也能通过逐行解析兜底。

代码示例：
try {
    // 策略1: JSON数组解析
    List<String> steps = objectMapper.readValue(json, List.class);
    return steps;
} catch (Exception e) {
    // 策略2: 逐行解析（容错）
    return parseLinesAsFallback(json);
}
```

### Q3: 用户看到的是什么步骤？文本步骤还是ExecutionStep？

**A**:
```
用户看到的是 ExecutionStep 对象，而不是文本步骤（List<String>）。

具体流程：
1. PlanDraftingService 生成文本步骤（List<String>）—— 用户看不到
2. LynxeController 将文本步骤转换为 ExecutionStep 对象
3. ExecutionStep 通过三种方式展示给用户：
   - SSE流式输出：实时显示当前执行步骤
   - 执行记录：保存到数据库供查询
   - 最终报告：PlanFinalizer 生成用户友好的报告

关键区别：
- 文本步骤：临时变量，仅用于转换，生命周期毫秒级
- ExecutionStep：持久化对象，包含完整上下文，用户可见
```

### Q4: 为什么不让 DynamicAgent 自己调用 PlanDraftingService？

**A**:
```
这是基于关注点分离的设计原则：

1. 职责分离：
   - Controller层：负责请求处理、参数解析、步骤准备
   - Agent层：负责步骤执行、工具调用、结果处理

2. 执行前准备：
   - 步骤必须在执行之前完全确定
   - Agent不应该在执行过程中改变自己的步骤列表

3. 计划不变性：
   - 一旦执行开始，步骤不应再改变
   - 这样可以保证执行的可预测性和可追溯性

如果让Agent自己调用PlanDraftingService，会导致Agent职责过重，
执行逻辑和准备逻辑耦合，降低代码可维护性。
```

### Q5: buildStepRequirement 方法做了哪些智能处理？

**A**:
```
buildStepRequirement 方法实现了三种智能处理：

1. Agent类型标注：
   根据planTemplateId自动添加Agent标签：
   - "auto-browser" → "[BROWSER_AGENT] 步骤描述"
   - "auto-database" → "[DATABASE_AGENT] 步骤描述"
   - 其他 → "[GENERAL_AGENT] 步骤描述"

2. 语言检测与本地化：
   通过Unicode中文字符检测任务语言：
   - 中文任务：最后一步添加 "完成后调用 terminate。"
   - 英文任务：最后一步添加 "Call terminate when finished."

3. 防重复添加：
   如果步骤描述已经包含Agent标签或terminate提示，则跳过添加。

代码：
private boolean containsChinese(String text) {
    return text.matches(".*\\p{IsHan}+.*");  // Unicode中文检测
}
```

### Q6: 步骤生成失败会怎样？

**A**:
```
我们设计了三层容错机制：

第一层：JSON数组解析
  优先尝试解析标准的JSON数组格式

第二层：逐行解析
  如果JSON解析失败，按行分割文本作为步骤

第三层：默认步骤
  如果前两层都失败，返回通用的默认步骤：
  - "Understand the user's requirements"
  - "Analyze the task and break it down"
  - "Execute the necessary steps to complete the task"

实际代码：
return buildFallbackPlan(desiredMaxSteps);

这样即使LLM完全不可用，系统也能继续运行，虽然步骤比较通用，
但不会导致整个任务失败。
```

### Q7: auto- 模板和普通模板有什么区别？

**A**:
```
auto- 模板：
- planTemplateId 以 "auto-" 开头（如 "auto-browser"）
- 触发自动规划流程：调用 PlanDraftingService 生成步骤
- 步骤是根据用户任务动态生成的，更加个性化
- 适用于复杂、不确定的任务

普通模板：
- planTemplateId 不以 "auto-" 开头
- 使用预定义的固定步骤
- 步骤在模板配置时确定，执行时不变
- 适用于标准化、重复性的任务

判断代码：
if (planTemplateId.startsWith("auto-")) {
    // 自动规划模式
    applyDraftedPlanIfNeeded(plan, planTemplateId, replacementParams);
} else {
    // 模板模式，直接使用预定义步骤
}
```

---

## 🎓 总结

### 核心要点

1. **步骤生成者**: LynxeController（不是DynamicAgent）
2. **生成时机**: 执行之前（LynxeController.java:586）
3. **JSON原因**: LLM标准输出、类型安全、容错机制
4. **用户可见**: ExecutionStep对象（通过SSE/记录/报告）
5. **文本步骤**: 临时变量，仅用于转换

### 关键代码位置

- **步骤生成**: `PlanDraftingService.java:47-91`
- **步骤转换**: `LynxeController.java:1063-1115`
- **调用点**: `LynxeController.java:586`
- **格式化**: `LynxeController.java:1178-1193`

### 架构设计

```
┌─────────────────────────────────────────────────────┐
│                  用户请求                            │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│              LynxeController                         │
│  ┌─────────────────────────────────────────────┐    │
│  │ applyDraftedPlanIfNeeded()                 │    │
│  │  → 判断: planTemplateId.startsWith("auto-")?│    │
│  │  → 调用: PlanDraftingService.draftPlanSteps()│   │
│  │  → 转换: List<String> → ExecutionStep       │    │
│  │  → 设置: dynamicPlan.setSteps(newSteps)     │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│         DynamicAgentExecutionPlan                    │
│  steps: [ExecutionStep, ExecutionStep, ...]         │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│              DynamicAgent                            │
│  ┌─────────────────────────────────────────────┐    │
│  │ for each ExecutionStep:                     │    │
│  │  → llmService.chat(stepRequirement)         │    │
│  │  → 选择工具 → 执行 → 返回结果               │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│            PlanFinalizer                             │
│  ┌─────────────────────────────────────────────┐    │
│  │ 生成最终报告（包含所有ExecutionStep）        │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│               用户响应                               │
│  (包含格式化的ExecutionStep列表)                     │
└─────────────────────────────────────────────────────┘
```

---

> **文档版本**: v1.0
> **最后更新**: 2025-01-20
> **作者**: Claude + 用户共同学习
> **相关文档**:
> - `PlanDraftingService核心解析-计划草稿服务.md`
> - `PlanningFactory核心解析-为什么需要三种工具创建方式.md`
> - `Tool注解与ToolCallback完全解析.md`
