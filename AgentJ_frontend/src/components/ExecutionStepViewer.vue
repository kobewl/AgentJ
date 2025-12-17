<template>
  <div class="execution-viewer">
    <!-- 执行阶段折叠面板 -->
    <div
      v-for="(agent, agentIdx) in executionSequence"
      :key="agent.stepId || agentIdx"
      class="agent-section"
    >
      <div class="agent-header" @click="toggleAgent(agentIdx)">
        <div class="header-left">
          <span class="collapse-icon">{{ expandedAgents.includes(agentIdx) ? '▼' : '▶' }}</span>
          <span class="agent-phase">{{ getAgentPhase(agent) }}</span>
          <span class="step-progress" v-if="agent.currentStep || agent.maxSteps">
            步骤 {{ agent.currentStep || 0 }}/{{ agent.maxSteps || '?' }}
          </span>
        </div>
        <div class="header-right">
          <span :class="['status-badge', getStatusClass(agent.status)]">
            {{ formatStatus(agent.status) }}
          </span>
        </div>
      </div>

      <div v-show="expandedAgents.includes(agentIdx)" class="agent-content">
        <!-- Agent 基本信息 -->
        <div class="agent-info">
          <div class="info-row" v-if="agent.agentName">
            <span class="info-label">Agent:</span>
            <span class="info-value">{{ agent.agentName }}</span>
          </div>
          <div class="info-row" v-if="agent.modelName">
            <span class="info-label">Model:</span>
            <span class="info-value mono">{{ agent.modelName }}</span>
          </div>
          <div class="info-row" v-if="agent.agentDescription">
            <span class="info-label">描述:</span>
            <span class="info-value">{{ agent.agentDescription }}</span>
          </div>
          <div class="info-row" v-if="agent.agentRequest">
            <span class="info-label">请求:</span>
            <span class="info-value request-text">{{ truncateText(agent.agentRequest, 200) }}</span>
          </div>
        </div>

        <!-- Think/Act 步骤列表 -->
        <div class="steps-container" v-if="agent.thinkActSteps?.length">
          <div
            v-for="(step, stepIdx) in agent.thinkActSteps"
            :key="`${agent.stepId}-${stepIdx}`"
            class="step-item"
            :class="{ 'step-running': step.status === 'RUNNING' }"
          >
            <!-- 步骤头部 -->
            <div class="step-header">
              <div class="step-number">{{ stepIdx + 1 }}</div>
              <span :class="['step-type', getStepTypeClass(step)]">
                {{ getStepTypeLabel(step) }}
              </span>
              <span class="step-tool" v-if="getToolName(step)">
                {{ getToolName(step) }}
              </span>
              <span :class="['step-status', getStatusClass(step.status)]" v-if="step.status">
                {{ formatStatus(step.status) }}
              </span>
            </div>

            <!-- 思考内容 -->
            <div v-if="step.thinkOutput" class="step-block think-block">
              <div class="block-label">💭 思考</div>
              <div class="block-content">{{ truncateText(step.thinkOutput, 500) }}</div>
            </div>

            <!-- 思考输入（如果没有输出显示输入） -->
            <div v-else-if="step.thinkInput" class="step-block think-block">
              <div class="block-label">💭 输入</div>
              <div class="block-content">{{ truncateText(step.thinkInput, 500) }}</div>
            </div>

            <!-- 行动描述 -->
            <div v-if="step.actionDescription" class="step-block action-block">
              <div class="block-label">🎯 行动</div>
              <div class="block-content">{{ step.actionDescription }}</div>
            </div>

            <!-- 工具信息（从 actToolInfoList 获取） -->
            <div v-if="step.actToolInfoList?.length" class="tools-container">
              <div
                v-for="(tool, toolIdx) in step.actToolInfoList"
                :key="`tool-${stepIdx}-${toolIdx}`"
                class="tool-item"
              >
                <div class="tool-header">
                  <span class="tool-icon">🔧</span>
                  <span class="tool-name">{{ tool.name || '工具调用' }}</span>
                </div>
                
                <!-- 工具参数 -->
                <div v-if="tool.parameters" class="tool-params">
                  <div class="block-label">参数:</div>
                  <pre class="block-code">{{ formatToolParams(tool.parameters) }}</pre>
                </div>
                
                <!-- 工具结果 -->
                <div v-if="tool.result" class="tool-result">
                  <div class="block-label">结果:</div>
                  <div class="block-content result-content">{{ truncateText(tool.result, 400) }}</div>
                </div>
              </div>
            </div>

            <!-- 旧版工具参数（兼容） -->
            <div v-else-if="step.toolParameters && showToolParams" class="step-block params-block">
              <div class="block-label">📋 参数</div>
              <pre class="block-code">{{ formatToolParams(step.toolParameters) }}</pre>
            </div>

            <!-- 旧版执行结果（兼容） -->
            <div v-if="step.actionResult && !step.actToolInfoList?.length" class="step-block result-block">
              <div class="block-label">✅ 结果</div>
              <div class="block-content result-content">{{ truncateText(step.actionResult, 300) }}</div>
            </div>

            <!-- 错误信息 -->
            <div v-if="step.errorMessage" class="step-block error-block">
              <div class="block-label">❌ 错误</div>
              <div class="block-content error-text">{{ step.errorMessage }}</div>
            </div>

            <!-- 如果步骤没有任何内容，显示原始数据概览 -->
            <div v-if="!hasStepContent(step)" class="step-block empty-step">
              <div class="block-label">📝 步骤数据</div>
              <div class="block-content empty-content">
                <span v-if="step.actionNeeded">需要执行行动</span>
                <span v-if="step.id"> · ID: {{ step.id }}</span>
                <span v-if="!step.thinkOutput && !step.thinkInput && !step.actToolInfoList?.length">
                  · 正在处理中...
                </span>
              </div>
            </div>

            <!-- 步骤统计 -->
            <div class="step-stats" v-if="step.inputCharCount || step.outputCharCount">
              <span v-if="step.inputCharCount">输入: {{ step.inputCharCount }} 字符</span>
              <span v-if="step.outputCharCount">输出: {{ step.outputCharCount }} 字符</span>
            </div>
          </div>
        </div>

        <!-- 无步骤但有状态信息 -->
        <div v-else class="no-steps-info">
          <div class="loading-indicator" v-if="agent.status === 'RUNNING'">
            <span class="loading-dot"></span>
            <span class="loading-dot"></span>
            <span class="loading-dot"></span>
            <span class="loading-text">正在执行中...</span>
          </div>
          <div v-else-if="agent.status === 'IDLE'" class="idle-info">
            <span class="idle-icon">⏳</span>
            <span>等待开始执行</span>
          </div>
          <div v-else-if="agent.status === 'FINISHED'" class="finished-info">
            <span class="finished-icon">✓</span>
            <span>执行完成</span>
          </div>
          <div v-else class="pending-info">
            <span class="pending-icon">○</span>
            <span>等待执行...</span>
          </div>
        </div>

        <!-- Agent 执行结果 -->
        <div v-if="agent.result" class="agent-result">
          <div class="result-label">执行结果</div>
          <div class="result-text">{{ truncateText(agent.result, 500) }}</div>
        </div>

        <!-- 错误信息 -->
        <div v-if="agent.errorMessage" class="agent-error">
          <div class="error-label">错误信息</div>
          <div class="error-text">{{ agent.errorMessage }}</div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!executionSequence?.length" class="no-data">
      <div class="no-data-icon">◉</div>
      <div class="no-data-text">暂无执行记录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

interface ActToolInfo {
  name?: string;
  parameters?: string;
  result?: string;
  id?: string;
}

interface ThinkActStep {
  id?: number;
  thinkInput?: string;
  thinkOutput?: string;
  actionNeeded?: boolean;
  actionDescription?: string;
  toolName?: string;
  toolParameters?: string;
  actionResult?: string;
  status?: string;
  errorMessage?: string;
  inputCharCount?: number;
  outputCharCount?: number;
  actToolInfoList?: ActToolInfo[];
}

interface AgentExecution {
  stepId?: string;
  agentName?: string;
  agentDescription?: string;
  agentRequest?: string;
  modelName?: string;
  status?: string;
  result?: string;
  errorMessage?: string;
  thinkActSteps?: ThinkActStep[];
  currentStep?: number;
  maxSteps?: number;
}

const props = withDefaults(defineProps<{
  executionSequence: AgentExecution[];
  showToolParams?: boolean;
}>(), {
  executionSequence: () => [],
  showToolParams: true
});

const expandedAgents = ref<number[]>([]);

// 监听序列变化，自动展开所有 agent
watch(() => props.executionSequence, (newVal) => {
  if (newVal?.length) {
    expandedAgents.value = newVal.map((_, idx) => idx);
  }
}, { immediate: true });

const toggleAgent = (idx: number) => {
  const index = expandedAgents.value.indexOf(idx);
  if (index > -1) {
    expandedAgents.value.splice(index, 1);
  } else {
    expandedAgents.value.push(idx);
  }
};

const getAgentPhase = (agent: AgentExecution): string => {
  if (agent.agentName) {
    const name = agent.agentName;
    if (name.toLowerCase().includes('browser')) return '🌐 浏览器操作';
    if (name.toLowerCase().includes('search')) return '🔍 搜索';
    if (name.toLowerCase().includes('database') || name.toLowerCase().includes('sql')) return '🗄️ 数据库';
    if (name.toLowerCase().includes('read')) return '📖 读取';
    if (name.toLowerCase().includes('write')) return '✏️ 写入';
    if (name.toLowerCase().includes('plan')) return '📋 规划';
    if (name.toLowerCase().includes('execute')) return '⚡ 执行';
    if (name.toLowerCase().includes('configurable')) return '🤖 智能代理';
    return name;
  }
  return '⚙️ 处理中';
};

const getStatusClass = (status?: string): string => {
  const s = (status || '').toLowerCase();
  if (s.includes('running') || s === 'running') return 'status-running';
  if (s.includes('finished') || s.includes('success') || s === 'finished') return 'status-success';
  if (s.includes('fail') || s.includes('error')) return 'status-error';
  if (s.includes('idle')) return 'status-idle';
  return 'status-default';
};

const formatStatus = (status?: string): string => {
  const s = (status || 'IDLE').toUpperCase();
  if (s === 'RUNNING') return '执行中';
  if (s === 'FINISHED') return '完成';
  if (s.includes('ERROR') || s.includes('FAIL')) return '失败';
  if (s === 'IDLE') return '等待';
  return s;
};

// 获取工具名称（优先从 actToolInfoList）
const getToolName = (step: ThinkActStep): string => {
  if (step.actToolInfoList?.length) {
    return step.actToolInfoList.map(t => t.name).filter(Boolean).join(', ');
  }
  return step.toolName || '';
};

const getStepTypeClass = (step: ThinkActStep): string => {
  if (step.errorMessage) return 'type-error';
  const toolName = getToolName(step);
  if (toolName) {
    const tool = toolName.toLowerCase();
    if (tool.includes('search') || tool.includes('query')) return 'type-search';
    if (tool.includes('browser') || tool.includes('navigate')) return 'type-browse';
    if (tool.includes('write') || tool.includes('update') || tool.includes('insert')) return 'type-write';
    if (tool.includes('read') || tool.includes('select') || tool.includes('database')) return 'type-read';
    return 'type-tool';
  }
  if (step.thinkOutput && !step.actionNeeded) return 'type-think';
  return 'type-default';
};

const getStepTypeLabel = (step: ThinkActStep): string => {
  if (step.errorMessage) return '错误';
  const toolName = getToolName(step);
  if (toolName) {
    const tool = toolName.toLowerCase();
    if (tool.includes('search') || tool.includes('query')) return '搜索';
    if (tool.includes('browser') || tool.includes('navigate') || tool.includes('click')) return '浏览';
    if (tool.includes('write') || tool.includes('update') || tool.includes('insert')) return '写入';
    if (tool.includes('read') || tool.includes('select') || tool.includes('get') || tool.includes('database')) return '读取';
    if (tool.includes('execute')) return '执行';
    return '工具调用';
  }
  if (step.thinkOutput && !step.actionNeeded) return '思考';
  if (step.actionDescription) return '行动';
  if (step.thinkOutput || step.thinkInput) return '思考';
  return '步骤';
};

const truncateText = (text: string, maxLen: number): string => {
  if (!text) return '';
  if (text.length <= maxLen) return text;
  return text.substring(0, maxLen) + '...';
};

// 检查步骤是否有内容显示
const hasStepContent = (step: ThinkActStep): boolean => {
  return !!(
    step.thinkOutput ||
    step.thinkInput ||
    step.actionDescription ||
    step.actToolInfoList?.length ||
    step.toolParameters ||
    step.actionResult ||
    step.errorMessage
  );
};

const formatToolParams = (params: string): string => {
  if (!params) return '';
  try {
    const obj = JSON.parse(params);
    return JSON.stringify(obj, null, 2);
  } catch {
    return params;
  }
};
</script>

<style scoped>
.execution-viewer {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #1a1a2e;
  border-radius: 12px;
  overflow: hidden;
}

.agent-section {
  border-bottom: 1px solid #2d2d44;
}

.agent-section:last-child {
  border-bottom: none;
}

.agent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: #16213e;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.agent-header:hover {
  background: #1f2b4a;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-icon {
  font-size: 10px;
  color: #7f8c8d;
  width: 14px;
  transition: transform 0.2s;
}

.agent-phase {
  font-size: 14px;
  font-weight: 600;
  color: #ecf0f1;
}

.step-progress {
  font-size: 12px;
  color: #7f8c8d;
  background: #2d2d44;
  padding: 2px 8px;
  border-radius: 10px;
}

.header-right {
  display: flex;
  align-items: center;
}

.status-badge {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.status-running {
  background: #27ae60;
  color: white;
  animation: pulse 1.5s infinite;
}

.status-success {
  background: #16a085;
  color: white;
}

.status-error {
  background: #e74c3c;
  color: white;
}

.status-idle {
  background: #34495e;
  color: #bdc3c7;
}

.status-default {
  background: #2c3e50;
  color: #95a5a6;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.agent-content {
  padding: 16px 18px;
  background: #1a1a2e;
}

.agent-info {
  background: #16213e;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  gap: 12px;
  font-size: 13px;
  margin-bottom: 8px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #7f8c8d;
  min-width: 50px;
  flex-shrink: 0;
}

.info-value {
  color: #ecf0f1;
}

.request-text {
  color: #95a5a6;
  font-style: italic;
}

.mono {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: #3498db;
}

/* 步骤列表 */
.steps-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-item {
  background: #16213e;
  border-radius: 10px;
  padding: 14px 16px;
  border-left: 4px solid #3498db;
  transition: all 0.2s;
}

.step-item.step-running {
  border-left-color: #27ae60;
  box-shadow: 0 0 15px rgba(39, 174, 96, 0.2);
}

.step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.step-number {
  width: 24px;
  height: 24px;
  background: #3498db;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-type {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 6px;
  background: #2d2d44;
  color: #ecf0f1;
}

.type-search { background: #2980b9; color: white; }
.type-browse { background: #e67e22; color: white; }
.type-write { background: #9b59b6; color: white; }
.type-read { background: #27ae60; color: white; }
.type-tool { background: #8e44ad; color: white; }
.type-think { background: #1abc9c; color: white; }
.type-error { background: #e74c3c; color: white; }
.type-default { background: #34495e; color: #bdc3c7; }

.step-tool {
  font-size: 11px;
  color: #3498db;
  font-family: 'Consolas', 'Monaco', monospace;
  background: #0d1117;
  padding: 2px 8px;
  border-radius: 4px;
}

.step-status {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: auto;
}

.step-block {
  margin-top: 10px;
}

.block-label {
  font-size: 11px;
  color: #7f8c8d;
  margin-bottom: 6px;
  font-weight: 500;
}

.block-content {
  font-size: 13px;
  color: #bdc3c7;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.think-block .block-content {
  color: #1abc9c;
  padding-left: 12px;
  border-left: 2px solid #1abc9c;
}

.action-block .block-content {
  color: #f39c12;
}

/* 工具信息容器 */
.tools-container {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tool-item {
  background: #0d1117;
  border-radius: 8px;
  padding: 12px;
  border-left: 3px solid #8e44ad;
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.tool-icon {
  font-size: 14px;
}

.tool-name {
  font-size: 13px;
  font-weight: 600;
  color: #bd93f9;
  font-family: 'Consolas', 'Monaco', monospace;
}

.tool-params {
  margin-bottom: 10px;
}

.tool-result {
  margin-top: 8px;
}

.params-block {
  margin-top: 10px;
}

.block-code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  color: #3498db;
  background: #161b22;
  padding: 10px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
}

.result-block .result-content,
.tool-result .result-content {
  color: #27ae60;
  background: rgba(39, 174, 96, 0.1);
  padding: 10px;
  border-radius: 6px;
  max-height: 150px;
  overflow-y: auto;
}

.error-block .error-text {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.1);
  padding: 10px;
  border-radius: 6px;
}

.empty-step .empty-content {
  color: #7f8c8d;
  font-style: italic;
  font-size: 12px;
}

/* 步骤统计 */
.step-stats {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #2d2d44;
  font-size: 11px;
  color: #7f8c8d;
  display: flex;
  gap: 16px;
}

/* 无步骤状态 */
.no-steps-info {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #7f8c8d;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  background: #27ae60;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.loading-dot:nth-child(1) { animation-delay: -0.32s; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.loading-text {
  color: #27ae60;
  font-size: 13px;
  margin-left: 8px;
}

.idle-info,
.finished-info,
.pending-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.idle-icon { font-size: 16px; }
.finished-icon { color: #27ae60; font-size: 18px; }
.pending-icon { font-size: 14px; }

/* Agent 结果 */
.agent-result {
  margin-top: 16px;
  padding: 14px;
  background: rgba(39, 174, 96, 0.1);
  border-radius: 8px;
  border-left: 3px solid #27ae60;
}

.result-label {
  font-size: 12px;
  color: #27ae60;
  font-weight: 600;
  margin-bottom: 8px;
}

.result-text {
  font-size: 13px;
  color: #ecf0f1;
  line-height: 1.6;
  white-space: pre-wrap;
}

.agent-error {
  margin-top: 16px;
  padding: 14px;
  background: rgba(231, 76, 60, 0.1);
  border-radius: 8px;
  border-left: 3px solid #e74c3c;
}

.error-label {
  font-size: 12px;
  color: #e74c3c;
  font-weight: 600;
  margin-bottom: 8px;
}

/* 空状态 */
.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #7f8c8d;
}

.no-data-icon {
  font-size: 32px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.no-data-text {
  font-size: 14px;
}
</style>


