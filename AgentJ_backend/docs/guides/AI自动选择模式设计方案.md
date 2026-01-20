# AI自动选择模式设计方案

> 目标：让AI根据用户任务自动选择最合适的执行模式，无需用户手动选择

---

## 🎯 设计目标

**用户视角**:
```
用户：帮我查一下淘宝上iPhone 15的价格
    ↓
系统：自动判断 → 选择 guided-browser-research 模板
    ↓
执行：使用浏览器工具完成任务
```

**不再需要**:
- ❌ 用户手动选择模板
- ❌ 用户了解各种模式的区别
- ❌ 用户输入 planTemplateId

---

## 🏗️ 架构设计

### 方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **方案A: 规则匹配** | 快速、成本低、可控 | 不够智能、覆盖有限 | ⭐⭐⭐⭐ |
| **方案B: LLM判断** | 智能准确、灵活 | 成本高、速度慢 | ⭐⭐⭐ |
| **方案C: 混合方案** | 平衡速度和准确性 | 实现复杂 | ⭐⭐⭐⭐⭐ |

**推荐**: 方案C（混合方案）

---

## 🚀 方案C：混合方案实现

### 核心思路

```
用户输入任务
    ↓
1️⃣ 规则匹配（关键词）
    ├─ 匹配成功 → 直接返回模板
    └─ 匹配失败 ↓
2️⃣ LLM智能判断
    └─ 返回最合适的模板
```

### 优势

1. **快速**: 80%常见任务通过规则匹配，毫秒级响应
2. **准确**: 20%复杂任务通过LLM判断，准确率高
3. **可控**: 规则优先，LLM兜底
4. **成本低**: 大部分情况不调用LLM

---

## 📋 实现细节

### 1. 规则匹配配置

**关键词映射表**:
```java
Map<String, String> KEYWORD_MAPPING = {
    // 浏览器相关 → guided-browser-research
    "网页": "guided-browser-research",
    "网站": "guided-browser-research",
    "淘宝": "guided-browser-research",
    "京东": "guided-browser-research",
    "浏览器": "guided-browser-research",
    "搜索": "guided-browser-research",
    "网址": "guided-browser-research",

    // 数据库相关 → guided-database（未来扩展）
    "数据库": "guided-database",
    "查询": "guided-database",
    "数据表": "guided-database",

    // 分析相关 → guided-general
    "分析": "guided-general",
    "统计": "guided-general",
    "报告": "guided-general",
    "导出": "guided-general"
}
```

### 2. LLM判断Prompt

```java
String PROMPT = """
你是一个任务分类助手。根据用户输入的任务，选择最合适的执行模式。

可选模式：
1. autonomous - 完全自主模式
   适用场景：探索性任务、复杂问题、多步骤综合性任务
   示例："帮我分析这个复杂的情况"、"探索一下这个问题"

2. guided-browser-research - 浏览器调研模式
   适用场景：网页信息收集、电商比价、网站操作
   示例："查一下淘宝iPhone价格"、"访问这个网站并收集信息"

3. guided-general - 通用引导模式
   适用场景：标准化任务、数据分析、文档处理
   示例："分析这个CSV文件"、"生成一份报告"

用户任务：{userInput}

请只返回模式的 planTemplateId（如：guided-browser-research），不要返回其他内容。
""";
```

---

## 💻 代码实现

### TemplateSelector 服务

```java
@Service
public class TemplateSelector {

    @Autowired
    private LlmService llmService;

    private static final Map<String, String> KEYWORD_MAPPING = new HashMap<>();

    @PostConstruct
    public void init() {
        // 浏览器相关
        KEYWORD_MAPPING.put("网页", "guided-browser-research");
        KEYWORD_MAPPING.put("网站", "guided-browser-research");
        KEYWORD_MAPPING.put("淘宝", "guided-browser-research");
        KEYWORD_MAPPING.put("京东", "guided-browser-research");
        KEYWORD_MAPPING.put("浏览器", "guided-browser-research");
        KEYWORD_MAPPING.put("搜索", "guided-browser-research");
        KEYWORD_MAPPING.put("网址", "guided-browser-research");
        KEYWORD_MAPPING.put("链接", "guided-browser-research");
        KEYWORD_MAPPING.put("http", "guided-browser-research");
        KEYWORD_MAPPING.put("https", "guided-browser-research");
        KEYWORD_MAPPING.put(".com", "guided-browser-research");
        KEYWORD_MAPPING.put(".cn", "guided-browser-research");

        // 数据分析相关
        KEYWORD_MAPPING.put("数据", "guided-general");
        KEYWORD_MAPPING.put("分析", "guided-general");
        KEYWORD_MAPPING.put("统计", "guided-general");
        KEYWORD_MAPPING.put("报告", "guided-general");
        KEYWORD_MAPPING.put("导出", "guided-general");
        KEYWORD_MAPPING.put("excel", "guided-general");
        KEYWORD_MAPPING.put("csv", "guided-general");

        // 数据库相关
        KEYWORD_MAPPING.put("数据库", "guided-database");
        KEYWORD_MAPPING.put("表", "guided-database");
        KEYWORD_MAPPING.put("sql", "guided-database");
    }

    /**
     * 自动选择最合适的模板
     * @param userInput 用户输入的任务
     * @return planTemplateId
     */
    public String selectTemplate(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return "autonomous-default"; // 默认
        }

        // 1. 尝试规则匹配
        String matched = matchByRules(userInput);
        if (matched != null) {
            log.info("Template selected by rules: {} for input: {}", matched, userInput);
            return matched;
        }

        // 2. LLM智能判断（如果规则匹配失败）
        String selected = selectByLLM(userInput);
        log.info("Template selected by LLM: {} for input: {}", selected, userInput);
        return selected;
    }

    /**
     * 基于规则匹配
     */
    private String matchByRules(String userInput) {
        String lowerInput = userInput.toLowerCase();

        // 检查是否包含URL（优先级最高）
        if (lowerInput.contains("http://") || lowerInput.contains("https://") ||
            lowerInput.contains(".com") || lowerInput.contains(".cn")) {
            return "guided-browser-research";
        }

        // 检查特定平台
        if (lowerInput.contains("淘宝") || lowerInput.contains("天猫") ||
            lowerInput.contains("京东") || lowerInput.contains("拼多多")) {
            return "guided-browser-research";
        }

        // 检查浏览器相关关键词
        for (Map.Entry<String, String> entry : KEYWORD_MAPPING.entrySet()) {
            if (lowerInput.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        return null; // 未匹配
    }

    /**
     * 基于LLM判断
     */
    private String selectByLLM(String userInput) {
        String prompt = buildLLMPrompt(userInput);

        try {
            // 调用LLM
            ChatResponse response = llmService.chat(prompt, null);
            String result = response.getResult().getOutput().getText().trim();

            // 验证返回的模板ID是否有效
            if (isValidTemplate(result)) {
                return result;
            }
        } catch (Exception e) {
            log.warn("LLM template selection failed, using default", e);
        }

        // 失败时返回默认
        return "autonomous-default";
    }

    private String buildLLMPrompt(String userInput) {
        return String.format("""
            你是一个任务分类助手。根据用户输入的任务，选择最合适的执行模式。

            可选模式：
            1. autonomous-default - 完全自主模式
               适用场景：探索性任务、复杂问题、多步骤综合性任务
               示例："帮我分析这个复杂的情况"、"探索一下这个问题"

            2. guided-browser-research - 浏览器调研模式
               适用场景：网页信息收集、电商比价、网站操作
               示例："查一下淘宝iPhone价格"、"访问这个网站并收集信息"

            3. guided-general - 通用引导模式
               适用场景：标准化任务、数据分析、文档处理
               示例："分析这个CSV文件"、"生成一份报告"

            用户任务：%s

            请只返回模式的 planTemplateId（如：guided-browser-research），不要返回其他内容。
            """, userInput);
    }

    private boolean isValidTemplate(String templateId) {
        // 验证模板ID是否在有效列表中
        Set<String> validTemplates = Set.of(
            "autonomous-default",
            "guided-browser-research",
            "guided-general"
        );
        return validTemplates.contains(templateId);
    }
}
```

---

## 🔧 LynxeController 集成

### 修改前

```java
@PostMapping("/executePlan")
public ResponseEntity<?> executePlan(@RequestBody Map<String, Object> request) {
    String planTemplateId = (String) request.get("planTemplateId");  // 用户必须提供
    // ...
}
```

### 修改后

```java
@Autowired
private TemplateSelector templateSelector;

@PostMapping("/executePlan")
public ResponseEntity<?> executePlan(@RequestBody Map<String, Object> request) {
    String userInput = extractUserInput(request);
    String planTemplateId = (String) request.get("planTemplateId");

    // 如果用户没有指定模板，自动选择
    if (planTemplateId == null || planTemplateId.trim().isEmpty()) {
        planTemplateId = templateSelector.selectTemplate(userInput);
        log.info("Auto-selected template: {} for user input: {}", planTemplateId, userInput);
    }

    // 继续原有流程...
}
```

---

## 📊 判断流程图

```
用户输入："帮我查淘宝iPhone价格"
    ↓
【规则匹配阶段】
    ↓
包含 "淘宝" ？ → 是 → 返回 guided-browser-research ✅

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

用户输入："帮我分析这个复杂的问题"
    ↓
【规则匹配阶段】
    ↓
不包含任何关键词 → 匹配失败
    ↓
【LLM判断阶段】
    ↓
LLM分析：探索性任务 → 返回 autonomous-default ✅

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

用户输入："分析这个CSV文件并生成报告"
    ↓
【规则匹配阶段】
    ↓
包含 "csv" 和 "分析" → 返回 guided-general ✅
```

---

## 🎯 使用示例

### 示例1: 网页查询

**用户输入**:
```
帮我查一下淘宝上iPhone 15的价格
```

**系统判断**:
```
规则匹配: 包含"淘宝" → guided-browser-research
执行: 使用浏览器工具查询淘宝价格
```

### 示例2: 数据分析

**用户输入**:
```
分析这个销售数据并导出Excel
```

**系统判断**:
```
规则匹配: 包含"分析"和"导出" → guided-general
执行: 使用数据分析工具处理
```

### 示例3: 复杂探索

**用户输入**:
```
帮我研究一下这个问题，看看有什么解决方案
```

**系统判断**:
```
规则匹配: 无匹配关键词
LLM判断: 探索性任务 → autonomous-default
执行: AI完全自主探索
```

---

## 💡 优势

### 用户体验提升

**改进前**:
```
用户：帮我查淘宝价格
系统：请选择模板（autonomous / guided-browser / guided-general）
用户：？？？选哪个？
```

**改进后**:
```
用户：帮我查淘宝价格
系统：自动判断 → 使用 guided-browser-research → 直接执行 ✅
```

### 技术优势

1. **智能**: AI自动判断，无需用户了解模式区别
2. **快速**: 80%任务通过规则匹配，毫秒级响应
3. **准确**: 20%复杂任务通过LLM，准确率高
4. **可控**: 规则优先，LLM兜底，可监控可调整
5. **成本优化**: 大部分情况不调用LLM，节省成本

---

## 📝 配置示例

### application.yml

```yaml
template:
  selector:
    # 是否启用LLM判断（规则匹配失败时）
    enable-llm-fallback: true
    # 规则匹配关键词配置
    keywords:
      browser:
        - 网页
        - 网站
        - 淘宝
        - 京东
        - 浏览器
      general:
        - 分析
        - 数据
        - 报告
        - 导出
    # 默认模板（无法判断时使用）
    default-template: autonomous-default
```

---

## ⚠️ 注意事项

### 1. 向后兼容

**用户仍然可以手动指定模板**:
```json
{
  "task": "查淘宝价格",
  "planTemplateId": "guided-browser-research"  // 显式指定
}
```

**如果用户不指定**:
```json
{
  "task": "查淘宝价格"
  // planTemplateId 为空，系统自动选择
}
```

### 2. 日志记录

**记录自动选择的决策过程**:
```java
log.info("Auto-selected template: {} for user input: {} (matched by: {})",
         templateId, userInput, matchMethod);
```

便于调试和优化规则。

### 3. 规则优先

**规则匹配 > LLM判断 > 默认模板**

原因：
- 规则匹配：快速、可控、零成本
- LLM判断：智能、准确、有成本
- 默认模板：兜底、保底

---

## 🚀 实施步骤

1. ✅ 设计方案（本文档）
2. ⏳ 创建 TemplateSelector 服务
3. ⏳ 配置关键词映射表
4. ⏳ 实现 LLM 判断逻辑
5. ⏳ 修改 LynxeController 集成
6. ⏳ 添加日志和监控
7. ⏳ 测试验证
8. ⏳ 优化规则和Prompt

---

## 📈 持续优化

### 数据驱动优化

**收集数据**:
- 用户输入
- 选择的模板
- 执行结果
- 用户反馈

**分析优化**:
- 哪些规则最常用？
- 哪些规则需要调整？
- LLM判断的准确率？
- 用户满意度如何？

**迭代改进**:
- 根据数据调整关键词映射
- 优化LLM的Prompt
- 添加新的规则分类
- 提升自动选择准确率

---

> **文档版本**: v1.0
> **最后更新**: 2025-01-20
> **作者**: Claude + 用户共同设计
