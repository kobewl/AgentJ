<template>
  <div class="agent-page">
    <!-- 左侧：输入面板 -->
    <div class="input-panel">
      <div class="panel-header">
        <div class="header-icon">
          <el-icon :size="24"><Cpu /></el-icon>
        </div>
        <div class="header-content">
          <h2 class="panel-title">AI Agent</h2>
          <p class="panel-subtitle">智能任务执行助手</p>
        </div>
      </div>

      <div class="panel-body">
        <!-- Agent 类型选择 -->
        <div class="form-section">
          <label class="form-label">选择 Agent 类型</label>
          <div class="agent-type-grid">
            <div
              v-for="agent in agentTypes"
              :key="agent.id"
              :class="['agent-type-card', { active: selectedAgent === agent.id }]"
              @click="selectAgent(agent.id)"
            >
              <div class="agent-icon">
                <el-icon :size="20"><component :is="agent.icon" /></el-icon>
              </div>
              <div class="agent-info">
                <span class="agent-name">{{ agent.name }}</span>
                <span class="agent-desc">{{ agent.description }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 数据源选择（仅数据库类型时显示） -->
        <div class="form-section" v-if="showDatasource">
          <label class="form-label">数据源</label>
          <el-select
            v-model="datasourceName"
            placeholder="选择数据源"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="ds in datasources"
              :key="ds.name"
              :label="ds.name"
              :value="ds.name"
            />
          </el-select>
        </div>

        <!-- 任务输入 -->
        <div class="form-section">
          <label class="form-label">任务描述</label>
          <el-input
            v-model="taskInput"
            type="textarea"
            :rows="5"
            :placeholder="getPlaceholder"
            class="task-input"
          />
        </div>

        <!-- 提交按钮 -->
        <div class="submit-section">
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="!taskInput.trim()"
            @click="submitTask"
            class="submit-btn"
          >
            <el-icon v-if="!submitting"><VideoPlay /></el-icon>
            {{ submitting ? '执行中...' : '开始执行' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 右侧：执行状态面板 -->
    <div class="output-panel">
      <div class="panel-header output-header">
        <div class="header-left">
          <h3 class="output-title">执行状态</h3>
          <el-tag v-if="planId" :type="statusTagType" size="small">{{ statusText }}</el-tag>
        </div>
        <div class="header-actions">
          <el-button size="small" :disabled="!planId" @click="refresh" text>
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button size="small" type="danger" :disabled="!planId" @click="stopNow" text>
            <el-icon><VideoPause /></el-icon>
            停止
          </el-button>
        </div>
      </div>

      <div class="output-body">
        <!-- 任务信息 -->
        <div class="task-info" v-if="planId">
          <div class="info-item">
            <span class="info-label">Plan ID</span>
            <span class="info-value mono">{{ planId }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Agent</span>
            <span class="info-value">{{ currentAgentName }}</span>
          </div>
        </div>

        <!-- 计划步骤 -->
        <div class="plan-section" v-if="planSteps.length">
          <div class="section-header">
            <span class="section-title">计划</span>
            <span class="step-count">{{ planCompleted }}/{{ planTotal }} 已完成</span>
          </div>
          <div class="plan-list">
            <div
              v-for="item in planSteps"
              :key="`plan-step-${item.index}`"
              :class="['plan-item', `plan-${item.state}`]"
            >
              <div class="plan-index">{{ item.index }}</div>
              <div class="plan-text">{{ item.text }}</div>
              <div class="plan-status">{{ item.label }}</div>
            </div>
          </div>
        </div>

        <!-- 等待用户输入 -->
        <div class="wait-input-section" v-if="waitState?.waiting">
          <div class="wait-header">
            <el-icon class="wait-icon"><Warning /></el-icon>
            <span>等待用户输入</span>
          </div>
          <p class="wait-desc">{{ waitState?.formDescription || waitState?.title }}</p>
          <el-form label-width="100px" class="wait-form">
            <el-form-item
              v-for="field in waitFields"
              :key="field.name"
              :label="field.label || field.name"
            >
              <el-input
                v-model="waitForm[field.name]"
                :placeholder="field.placeholder || ''"
                :type="field.type === 'textarea' ? 'textarea' : 'text'"
                :rows="3"
              />
            </el-form-item>
          </el-form>
          <el-button type="primary" :loading="submittingInput" @click="submitWaitInputs">
            提交
          </el-button>
        </div>

        <!-- 执行步骤 -->
        <div class="execution-section" v-if="executionSequence.length || planId">
          <div class="section-header">
            <span class="section-title">执行过程</span>
            <span class="step-count" v-if="totalSteps">{{ completedSteps }}/{{ totalSteps }} 步</span>
          </div>
          <div class="execution-container">
            <ExecutionStepViewer
              :execution-sequence="executionSequence"
              :show-tool-params="true"
            />
          </div>
        </div>

        <!-- 输出结果 -->
        <div class="result-section" v-if="detailText">
          <div class="section-header">
            <span class="section-title">执行结果</span>
            <el-button text size="small" @click="copyDetail">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
          <div class="result-content">
            <pre>{{ detailText }}</pre>
          </div>
        </div>

        <!-- 空状态 -->
        <div class="empty-state" v-if="!planId">
          <div class="empty-icon">
            <el-icon :size="48"><Promotion /></el-icon>
          </div>
          <p class="empty-text">选择 Agent 类型，输入任务描述后开始执行</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Cpu,
  VideoPlay,
  VideoPause,
  Refresh,
  Warning,
  CopyDocument,
  Promotion,
  Search,
  Edit,
  Connection,
  Monitor,
} from '@element-plus/icons-vue';
import { executeByToolAsync, getExecutionDetails, getTaskStatus, stopTask, submitUserInput } from '@/api/executor';
import { listDatasourceConfigs } from '@/api/datasource';
import { streamSse, type SseMessage } from '@/utils/sse';
import type { DatasourceConfig } from '@/api/types';
import ExecutionStepViewer from '@/components/ExecutionStepViewer.vue';

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Agent 类型定义
const agentTypes = [
  {
    id: 'general',
    name: '通用智能',
    description: '自动规划执行各类任务',
    icon: 'Cpu',
    toolName: 'auto_general_exec',
  },
  {
    id: 'browser',
    name: '浏览器操作',
    description: '网页浏览、数据采集',
    icon: 'Monitor',
    toolName: 'auto_browser_exec',
  },
  {
    id: 'database-read',
    name: '数据库查询',
    description: '查询数据、表结构分析',
    icon: 'Search',
    toolName: 'ai_database_read_agent',
  },
  {
    id: 'database-write',
    name: '数据库写入',
    description: '数据更新、插入、删除',
    icon: 'Edit',
    toolName: 'ai_database_write_agent',
  },
];

const selectedAgent = ref('general');
const taskInput = ref('');
const datasourceName = ref('');
const datasources = ref<DatasourceConfig[]>([]);

const submitting = ref(false);
const submittingInput = ref(false);
const planId = ref('');
const statusInfo = ref<any>(null);
const detailRaw = ref('');
const planStreamController = ref<AbortController | null>(null);
const waitForm = ref<Record<string, string>>({});

// 计算属性
const showDatasource = computed(() =>
  ['database-read', 'database-write'].includes(selectedAgent.value)
);

const currentAgentName = computed(() => {
  const agent = agentTypes.find(a => a.id === selectedAgent.value);
  return agent?.name || '通用智能';
});

const derivedToolName = computed(() => {
  const agent = agentTypes.find(a => a.id === selectedAgent.value);
  return agent?.toolName || 'auto_general_exec';
});

const getPlaceholder = computed(() => {
  switch (selectedAgent.value) {
    case 'browser':
      return '例如：打开 https://example.com 并提取页面标题';
    case 'database-read':
      return '例如：查询最近7天的订单数量统计';
    case 'database-write':
      return '例如：将 user 表中 age > 60 的用户状态更新为 senior';
    default:
      return '描述你想让 AI 完成的任务...';
  }
});

const statusText = computed(() => {
  if (!statusInfo.value) return '未开始';
  if (typeof statusInfo.value === 'string') return statusInfo.value;
  if (statusInfo.value.isRunning) return '执行中';
  if (statusInfo.value.desiredState === 'STOP') return '已停止';
  return '处理中';
});

const statusTagType = computed(() => {
  const text = statusText.value.toLowerCase();
  if (text.includes('执行中') || text.includes('处理中')) return 'warning';
  if (text.includes('完成') || text.includes('success')) return 'success';
  if (text.includes('停止') || text.includes('失败') || text.includes('error')) return 'danger';
  return 'info';
});

const detailJson = computed(() => {
  if (!detailRaw.value) return null;
  if (typeof detailRaw.value === 'object') return detailRaw.value as any;
  try {
    return JSON.parse(detailRaw.value);
  } catch {
    return null;
  }
});

const waitState = computed(() => detailJson.value?.userInputWaitState);

const waitFields = computed(() => {
  const inputs = waitState.value?.formInputs || [];
  return inputs
    .map((input: Record<string, string>) => {
      const name = input.name || input.key || input.field || input.id || input.label;
      if (!name) return null;
      return {
        name,
        label: input.label || input.name || input.field || name,
        placeholder: input.placeholder || '',
        type: input.type || 'text',
      };
    })
    .filter(Boolean) as { name: string; label: string; placeholder?: string; type?: string }[];
});

const executionSequence = computed(() => {
  const seq = detailJson.value?.agentExecutionSequence || [];
  // 调试输出 - 可以在浏览器控制台查看
  if (seq.length > 0) {
    console.log('[DEBUG] executionSequence:', JSON.stringify(seq, null, 2));
    seq.forEach((agent: any, idx: number) => {
      console.log(`[DEBUG] Agent ${idx}:`, agent.agentName, 'thinkActSteps count:', agent.thinkActSteps?.length || 0);
      if (agent.thinkActSteps?.length > 0) {
        agent.thinkActSteps.forEach((step: any, stepIdx: number) => {
          console.log(`[DEBUG] Step ${stepIdx}:`, {
            thinkOutput: step.thinkOutput?.substring(0, 100),
            actToolInfoList: step.actToolInfoList,
            toolName: step.toolName,
          });
        });
      }
    });
  }
  return seq;
});

const totalSteps = computed(() => {
  let count = 0;
  executionSequence.value.forEach((agent: any) => {
    count += agent.maxSteps || 0;
  });
  return count || executionSequence.value.length;
});

const completedSteps = computed(() => {
  let count = 0;
  executionSequence.value.forEach((agent: any) => {
    if (agent.status === 'FINISHED') count++;
    else if (agent.thinkActSteps?.length) {
      count += agent.thinkActSteps.filter((s: any) => s.status === 'FINISHED').length;
    }
  });
  return count;
});

const detailText = computed(() => {
  if (!detailJson.value) return '';
  if (detailJson.value.summary) return detailJson.value.summary;
  if (detailJson.value.structureResult) return detailJson.value.structureResult;
  // 如果有最后一个 agent 的结果
  const seq = detailJson.value.agentExecutionSequence || [];
  if (seq.length > 0) {
    const lastAgent = seq[seq.length - 1];
    if (lastAgent.result) return lastAgent.result;
  }
  return '';
});

const planSteps = computed(() => {
  const steps = (detailJson.value?.steps || []) as string[];
  const agents = (detailJson.value?.agentExecutionSequence || []) as any[];
  const fallback = steps.length
    ? steps
    : agents.map((agent: any) => agent.agentRequest || agent.agentName || '').filter(Boolean);

  const statusByIndex = new Map<number, string>();
  agents.forEach((agent: any) => {
    if (typeof agent.currentStep === 'number') {
      statusByIndex.set(agent.currentStep, agent.status);
    }
  });

  const currentIndex = detailJson.value?.currentStepIndex;
  const completed = detailJson.value?.completed;

  return fallback
    .map((text, idx) => {
      const normalizedText = String(text || '').replace(/^\[[^\]]+\]\s*/, '');
      const agentStatus = statusByIndex.get(idx);
      let state: 'done' | 'running' | 'pending' = 'pending';

      if (agentStatus === 'FINISHED') {
        state = 'done';
      } else if (agentStatus === 'RUNNING') {
        state = 'running';
      } else if (completed) {
        state = 'done';
      } else if (typeof currentIndex === 'number') {
        if (idx < currentIndex) state = 'done';
        else if (idx === currentIndex) state = 'running';
      }

      const label = state === 'done' ? '已完成' : state === 'running' ? '进行中' : '待执行';
      return {
        index: idx + 1,
        text: normalizedText,
        state,
        label,
      };
    })
    .filter(item => item.text);
});

const planTotal = computed(() => planSteps.value.length);
const planCompleted = computed(() => planSteps.value.filter(item => item.state === 'done').length);

// 方法
const selectAgent = (id: string) => {
  selectedAgent.value = id;
};

const loadDatasources = async () => {
  try {
    const res = await listDatasourceConfigs();
    const all = (res.data || []) as DatasourceConfig[];
    datasources.value = all.filter((d) => d.enable);
    if (!datasourceName.value && datasources.value.length) {
      datasourceName.value = datasources.value[0].name;
    }
  } catch (error) {
    console.error('加载数据源失败', error);
  }
};

const prefillWaitForm = () => {
  if (!waitFields.value.length) return;
  waitFields.value.forEach((f) => {
    if (waitForm.value[f.name] === undefined) {
      waitForm.value[f.name] = '';
    }
  });
};

const handleTaskStreamMessage = (data: SseMessage) => {
  if (!data) return;
  if (data.type === 'error') {
    ElMessage.error(data.message || '任务流连接异常');
    return;
  }
  if (data.planId && !planId.value) {
    planId.value = data.planId as string;
  }
  if (data.status !== undefined) {
    statusInfo.value = data.status;
  }
  if (data.detail !== undefined) {
    const detailValue = typeof data.detail === 'string' ? data.detail : JSON.stringify(data.detail, null, 2);
    detailRaw.value = detailValue;
    prefillWaitForm();
  }
  if (data.completed) {
    planStreamController.value = null;
  }
};

const startTaskStream = (id: string) => {
  if (!id) return;
  planStreamController.value?.abort();
  const controller = new AbortController();
  planStreamController.value = controller;
  streamSse(
    `${apiBase}/api/executor/taskStream`,
    { planId: id },
    handleTaskStreamMessage,
    () => {
      planStreamController.value = null;
    },
    controller.signal
  ).catch(() => {
    planStreamController.value = null;
  });
};

const buildReplacementParams = () => {
  const params: Record<string, unknown> = {};
  if (taskInput.value) {
    let input = taskInput.value;
    if (showDatasource.value && datasourceName.value) {
      input += `\n使用数据源: ${datasourceName.value}`;
    }
    params.task = input;
    params.input = input;
    params.prompt = input;
  }
  if (datasourceName.value) {
    params.datasourceName = datasourceName.value;
  }
  return params;
};

const submitTask = async () => {
  if (!taskInput.value.trim()) {
    ElMessage.warning('请输入任务描述');
    return;
  }

  // 重置状态
  planId.value = '';
  statusInfo.value = null;
  detailRaw.value = '';

  submitting.value = true;
  try {
    const payload = {
      toolName: derivedToolName.value,
      requestSource: 'VUE_DIALOG' as any,
      conversationId: undefined,
      replacementParams: buildReplacementParams(),
    };
    const res = await executeByToolAsync(payload);
    planId.value = (res.data as any)?.planId || '';
    statusInfo.value = (res.data as any)?.status || 'processing';
    detailRaw.value = JSON.stringify(res.data, null, 2);
    startTaskStream(planId.value);
    ElMessage.success('任务已提交');
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.error || '提交失败');
  } finally {
    submitting.value = false;
  }
};

const refresh = async () => {
  if (!planId.value) return;
  try {
    const [statusRes, detailRes] = await Promise.all([
      getTaskStatus(planId.value),
      getExecutionDetails(planId.value),
    ]);
    statusInfo.value = statusRes.data;
    detailRaw.value = detailRes.data as string;
    prefillWaitForm();
    if (!planStreamController.value) {
      startTaskStream(planId.value);
    }
    ElMessage.success('已刷新');
  } catch (error) {
    ElMessage.error('刷新失败');
  }
};

const stopNow = async () => {
  if (!planId.value) return;
  try {
    await stopTask(planId.value);
    ElMessage.success('已发送停止指令');
  } catch (error) {
    ElMessage.error('停止失败');
  }
};

const submitWaitInputs = async () => {
  if (!planId.value || !waitFields.value.length) return;
  submittingInput.value = true;
  try {
    await submitUserInput(planId.value, waitForm.value);
    ElMessage.success('已提交用户输入');
    refresh();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.error || '提交失败');
  } finally {
    submittingInput.value = false;
  }
};

const copyDetail = async () => {
  if (!detailText.value) return;
  await navigator.clipboard.writeText(detailText.value);
  ElMessage.success('已复制');
};

// 生命周期
onMounted(() => {
  loadDatasources();
});

onBeforeUnmount(() => {
  planStreamController.value?.abort();
});

// 监听 agent 类型变化时警告
watch(selectedAgent, (val) => {
  if (val === 'database-write') {
    ElMessage.warning('写入模式会修改数据库，请确认操作描述正确');
  }
});
</script>

<style scoped>
.agent-page {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 24px;
  min-height: calc(100vh - 112px);
  padding: 0;
}

/* 输入面板 */
.input-panel {
  background: var(--bg-primary);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.panel-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  opacity: 0.9;
}

.panel-body {
  padding: 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

/* Agent 类型选择网格 */
.agent-type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.agent-type-card {
  padding: 16px;
  border: 2px solid var(--border-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--bg-secondary);
}

.agent-type-card:hover {
  border-color: var(--primary-color);
  background: var(--bg-tertiary);
}

.agent-type-card.active {
  border-color: var(--primary-color);
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(139, 92, 246, 0.1));
}

.agent-icon {
  width: 36px;
  height: 36px;
  background: var(--primary-color);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 12px;
}

.agent-type-card.active .agent-icon {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
}

.agent-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.agent-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.agent-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.task-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 16px;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
}

.submit-section {
  margin-top: auto;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}

/* 输出面板 */
.output-panel {
  background: var(--bg-primary);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.output-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.output-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.output-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 任务信息 */
.task-info {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.mono {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
}

/* 等待输入 */
.wait-input-section {
  padding: 20px;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 12px;
}

.wait-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #92400e;
  margin-bottom: 8px;
}

.wait-icon {
  color: #f59e0b;
}

.wait-desc {
  color: #78350f;
  margin: 0 0 16px;
  font-size: 14px;
}

.wait-form {
  margin-bottom: 16px;
}

/* 执行步骤 */
.execution-section,
.result-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-item {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  font-size: 13px;
  color: var(--text-primary);
}

.plan-index {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.plan-text {
  line-height: 1.5;
  color: var(--text-primary);
}

.plan-status {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.plan-done {
  border-color: rgba(22, 163, 74, 0.4);
  background: rgba(22, 163, 74, 0.08);
}

.plan-done .plan-index {
  background: rgba(22, 163, 74, 0.15);
  color: #16a34a;
}

.plan-done .plan-status {
  background: rgba(22, 163, 74, 0.15);
  color: #16a34a;
}

.plan-running {
  border-color: rgba(234, 179, 8, 0.5);
  background: rgba(234, 179, 8, 0.1);
}

.plan-running .plan-index {
  background: rgba(234, 179, 8, 0.2);
  color: #ca8a04;
}

.plan-running .plan-status {
  background: rgba(234, 179, 8, 0.2);
  color: #ca8a04;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.step-count {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  padding: 4px 12px;
  border-radius: 12px;
}

.execution-container {
  max-height: 400px;
  overflow-y: auto;
  border-radius: 12px;
}

/* 结果 */
.result-content {
  background: #1e1e1e;
  border-radius: 12px;
  padding: 16px;
  max-height: 300px;
  overflow: auto;
}

.result-content pre {
  margin: 0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #d4d4d4;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-secondary);
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: var(--bg-secondary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.empty-text {
  font-size: 14px;
  margin: 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .agent-page {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .input-panel {
    max-height: none;
  }
}

@media (max-width: 768px) {
  .agent-type-grid {
    grid-template-columns: 1fr;
  }

  .task-info {
    flex-direction: column;
    gap: 12px;
  }
}
</style>



