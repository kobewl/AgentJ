# AI自动选择模式使用指南

> 用户友好的模板自动选择功能 - 让AI为你选择最合适的执行模式

---

## 🎯 功能简介

**问题**: 之前用户需要手动选择使用哪种模板（autonomous / guided-browser / guided-general）

**解决**: 现在用户只需输入 `planTemplateId: "auto"`，AI会自动分析任务并选择最合适的模板！

---

## 📖 使用方法

### 方法1: 完全自动选择（推荐）

**请求示例**:
```json
POST /api/executor/executeByToolName

{
  "toolName": "auto",
  "replacementParams": {
    "task": "帮我查一下淘宝上iPhone 15的价格"
  }
}
```

**系统行为**:
```
1. 检测到 planTemplateId = "auto"
2. 提取用户输入: "帮我查一下淘宝上iPhone 15的价格"
3. 自动判断: 包含"淘宝" → guided-browser-research
4. 使用浏览器模板执行任务 ✅
```

### 方法2: 手动指定模板（仍然支持）

**请求示例**:
```json
{
  "toolName": "guided-browser-research",  // 显式指定
  "replacementParams": {
    "task": "帮我查一下淘宝上iPhone 15的价格"
  }
}
```

---

## 🤖 AI判断逻辑

### 混合判断策略

```
用户输入
    ↓
【阶段1: 规则匹配】（80%任务，毫秒级）
    ├─ 包含"淘宝"/"京东"/"网页"？ → guided-browser-research
    ├─ 包含"数据"/"分析"/"报告"？ → guided-general
    └─ 未匹配 ↓
【阶段2: LLM判断】（20%任务，准确）
    └─ AI分析任务类型 → 返回最合适的模板
```

### 常见场景判断

| 用户输入示例 | 自动选择 | 原因 |
|-------------|---------|------|
| "查淘宝iPhone价格" | guided-browser-research | 包含"淘宝" |
| "访问这个网站 https://..." | guided-browser-research | 包含URL |
| "分析这个CSV文件" | guided-general | 包含"分析"和"csv" |
| "生成销售报告" | guided-general | 包含"报告" |
| "探索这个复杂问题" | autonomous-default | 规则未匹配，LLM判断 |

---

## 💻 API调用示例

### 示例1: 网页查询

**请求**:
```json
POST /api/executor/executeByToolNameAsync

{
  "toolName": "auto",
  "replacementParams": {
    "task": "帮我在淘宝上搜索iPhone 15并截图",
    "urls": "https://taobao.com",
    "goal": "找到最优惠的价格"
  }
}
```

**响应**:
```json
{
  "taskId": "task-123456",
  "status": "STARTED",
  "message": "🤖 Auto-selected template: guided-browser-research"
}
```

### 示例2: 数据分析

**请求**:
```json
POST /api/executor/executeByToolNameAsync

{
  "toolName": "auto",
  "replacementParams": {
    "task": "分析这个销售数据并生成Excel报告",
    "context": "需要包含月度趋势和Top10产品"
  }
}
```

**响应**:
```json
{
  "taskId": "task-789012",
  "status": "STARTED",
  "message": "🤖 Auto-selected template: guided-general"
}
```

### 示例3: 复杂探索

**请求**:
```json
POST /api/executor/executeByToolNameAsync

{
  "toolName": "auto",
  "replacementParams": {
    "task": "帮我研究一下这个问题，看看有哪些可能的解决方案"
  }
}
```

**响应**:
```json
{
  "taskId": "task-345678",
  "status": "STARTED",
  "message": "🤖 Auto-selected template: autonomous-default (by LLM)"
}
```

---

## 🔧 配置说明

### application.yml

```yaml
template:
  selector:
    # 是否启用LLM兜底判断（规则匹配失败时）
    enable-llm-fallback: true

    # 默认模板（完全无法判断时使用）
    default-template: autonomous-default
```

### 配置说明

**enable-llm-fallback**:
- `true`: 规则匹配失败时调用LLM判断（推荐，更准确）
- `false`: 规则匹配失败时直接使用默认模板（更快，但可能不够准确）

**default-template**:
- 当规则和LLM都无法判断时使用的模板
- 推荐设置为 `autonomous-default`（最灵活）

---

## 📊 性能与成本

### 成本对比

| 判断方式 | 响应时间 | LLM调用 | 成本 |
|---------|---------|---------|------|
| 规则匹配 | ~1ms | ❌ 不调用 | $0 |
| LLM判断 | ~500ms | ✅ 调用1次 | ~$0.001 |

**实际分布**（基于典型使用场景）:
- 规则匹配: 80% 的任务
- LLM判断: 20% 的任务

**总体成本**: 每次自动选择的平均成本约为 $0.0002

### 性能优化

**规则匹配优化**:
- URL检测优先级最高（一招命中）
- 常见平台关键词（淘宝、京东等）
- 通用关键词（网页、数据、分析）

**LLM判断优化**:
- 只在规则匹配失败时调用
- 使用简洁的Prompt，减少Token消耗
- 结果缓存（未来可添加）

---

## 🎨 前端集成

### Vue.js 示例

```vue
<template>
  <el-button @click="executeTask">提交任务</el-button>
</template>

<script setup>
import { ref } from 'vue';
import { executeByToolNameAsync } from '@/api/executor';

const userInput = ref('帮我查淘宝iPhone价格');

async function executeTask() {
  const response = await executeByToolNameAsync({
    toolName: 'auto',  // 🆕 使用 "auto" 触发自动选择
    replacementParams: {
      task: userInput.value
    }
  });

  console.log('选择的模板:', response.selectedTemplate);
  console.log('任务ID:', response.taskId);
}
</script>
```

### React 示例

```jsx
import { executeTask } from '@/api/executor';

function TaskForm() {
  const [input, setInput] = useState('');

  const handleSubmit = async () => {
    const result = await executeTask({
      toolName: 'auto',  // 🆕 使用 "auto" 触发自动选择
      replacementParams: {
        task: input
      }
    });

    console.log('选择的模板:', result.selectedTemplate);
  };

  return (
    <button onClick={handleSubmit}>提交任务</button>
  );
}
```

---

## ⚠️ 注意事项

### 1. 向后兼容

**旧版本调用方式仍然有效**:
```json
{
  "toolName": "guided-browser-research",  // 手动指定
  "replacementParams": {
    "task": "..."
  }
}
```

### 2. 优先级

```
手动指定 > 自动选择
```

如果用户手动指定了模板，系统不会覆盖。

### 3. 日志记录

**系统会记录自动选择的决策过程**:
```
🤖 Auto-selected template: guided-browser-research for user input: 查淘宝价格
```

便于调试和监控。

### 4. 特殊关键词

**支持的关键词（持续扩展中）**:
- **浏览器相关**: 网页、网站、淘宝、京东、浏览器、搜索、网址、链接
- **数据分析**: 数据、分析、统计、报告、导出、excel、csv、表格
- **URL检测**: http://、https://、.com、.cn

---

## 🔍 调试与监控

### 查看选择日志

**日志示例**:
```
INFO  - Template selected by rules: guided-browser-research for input: 查淘宝价格
INFO  - 🤖 Auto-selected template: guided-browser-research for user input: 查淘宝价格
```

### 获取统计信息

**API**: `GET /api/executor/template-selector/stats`

**响应**:
```json
{
  "keywordMappingSize": 20,
  "validTemplates": [
    "autonomous-default",
    "guided-browser-research",
    "guided-general"
  ],
  "enableLlmFallback": true,
  "defaultTemplate": "autonomous-default"
}
```

---

## 💡 最佳实践

### 1. 何时使用自动选择

**推荐使用**:
- ✅ 普通用户场景
- ✅ 任务类型不确定
- ✅ 快速原型开发
- ✅ 一体化应用

**手动指定更合适**:
- ⚠️ 企业级固定流程
- ⚠️ 已知的特定任务类型
- ⚠️ 需要精确控制成本

### 2. 输入描述建议

**好的输入**（易于判断）:
```
"帮我在淘宝上查iPhone 15的价格"
"分析这个销售数据并生成报告"
"访问 https://example.com 并收集信息"
```

**不够明确的输入**（可能误判）:
```
"帮我做这个"  // 太模糊
"执行任务"    // 无上下文
```

### 3. 测试建议

**测试流程**:
1. 先用规则匹配的关键词测试（快速验证）
2. 再测试复杂场景（验证LLM判断）
3. 检查日志确认选择正确
4. 根据实际使用调整关键词映射

---

## 🚀 未来优化

### 短期（1-2周）

- [ ] 添加更多关键词规则
- [ ] 优化LLM Prompt提高准确率
- [ ] 添加选择结果缓存
- [ ] 完善日志和监控

### 中期（1-2月）

- [ ] 基于用户反馈自动优化规则
- [ ] 支持自定义关键词配置
- [ ] 添加A/B测试能力
- [ ] 提供选择置信度评分

### 长期（3-6月）

- [ ] 机器学习模型替代LLM判断
- [ ] 多语言支持
- [ ] 用户个性化推荐
- [ ] 实时监控和告警

---

## 📞 支持与反馈

### 遇到问题？

1. **查看日志**: 系统会记录详细的选择过程
2. **检查输入**: 确保用户输入清晰明确
3. **手动指定**: 临时问题可以手动指定模板绕过

### 反馈渠道

- 📧 技术支持: support@example.com
- 📝 问题追踪: GitHub Issues
- 💬 用户社区: Discord/论坛

---

## 📝 更新日志

### v1.0 (2025-01-20)
- ✅ 初始版本发布
- ✅ 支持规则匹配 + LLM判断的混合方案
- ✅ 20+ 关键词规则
- ✅ 完整的日志和监控

---

> **文档版本**: v1.0
> **最后更新**: 2025-01-20
> **作者**: AgentJ Team
