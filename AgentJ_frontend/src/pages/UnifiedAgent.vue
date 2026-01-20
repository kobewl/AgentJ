<template>
  <div class="agent-page">
    <!-- 左侧：对话面板 -->
    <div class="chat-panel">
      <div class="panel-header">
        <div class="header-icon">
          <el-icon :size="24"><ChatDotRound /></el-icon>
        </div>
        <div class="header-content">
          <h2 class="panel-title">AI 智能助手</h2>
          <p class="panel-subtitle">自动识别任务类型并执行</p>
        </div>
      </div>

      <!-- 对话历史 -->
      <div class="messages-container">
        <div class="messages-list" ref="messagesListRef">
          <div v-if="conversationHistory.length === 0" class="empty-state">
            <div class="empty-icon">
              <el-icon :size="48"><Promotion /></el-icon>
            </div>
            <p class="empty-text">我可以帮你完成各种任务：</p>
            <div class="example-tasks">
              <div class="task-chip" @click="useExample('帮我查询北京今天的天气')">
                <el-icon><Search /></el-icon>
                查询天气
              </div>
              <div class="task-chip" @click="useExample('打开 https://github.com 并获取热门项目列表')">
                <el-icon><Monitor /></el-icon>
                网页浏览
              </div>
              <div class="task-chip" @click="useExample('查询数据库中用户数量统计')">
                <el-icon><Edit /></el-icon>
                数据库查询
              </div>
            </div>
          </div>

          <transition-group name="message" tag="div">
            <div
              v-for="(msg, index) in conversationHistory"
              :key="index"
              :class="['message-item', msg.role]"
            >
              <div class="message-avatar">
                <el-icon v-if="msg.role === 'user'"><User /></el-icon>
                <el-icon v-else><Cpu /></el-icon>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-role">{{ msg.role === 'user' ? '我' : 'AI 助手' }}</span>
                  <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
                </div>
                <div class="message-text markdown-body" v-html="renderMarkdown(msg.content)"></div>
              </div>
            </div>
          </transition-group>

          <!-- 执行中状态 -->
          <div v-if="executing" class="message-item assistant streaming">
            <div class="message-avatar">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-role">AI 助手</span>
                <span class="message-time">执行中...</span>
              </div>
              <div class="message-text">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-section">
        <el-input
          v-model="taskInput"
          type="textarea"
          :rows="3"
          placeholder="描述你想完成的任务，我会自动选择最合适的方式..."
          :disabled="executing"
          @keydown.enter.prevent="handleEnterKey"
          class="task-input"
        />
        <div class="input-actions">
          <el-button
            v-if="executing"
            type="danger"
            @click="stopExecution"
            :loading="submitting"
          >
            <el-icon><VideoPause /></el-icon>
            停止执行
          </el-button>
          <el-button
            v-else
            type="primary"
            @click="submitTask"
            :disabled="!taskInput.trim()"
            :loading="submitting"
          >
            <el-icon><VideoPlay /></el-icon>
            发送
          </el-button>
          <el-button @click="clearConversation" :disabled="conversationHistory.length === 0">
            <el-icon><Delete /></el-icon>
            清空对话
          </el-button>
        </div>
      </div>
    </div>

    <!-- 右侧：执行详情面板 -->
    <div class="output-panel" v-if="planId || showOutputPanel">
      <div class="panel-header output-header">
        <div class="header-left">
          <h3 class="output-title">执行详情</h3>
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
          <el-button size="small" @click="closeOutputPanel" text>
            <el-icon><Close /></el-icon>
            关闭
          </el-button>
        </div>
      </div>

      <div class="output-body">
        <!-- 任务信息 -->
        <div class="task-info" v-if="planId">
          <div class="info-item">
            <span class="info-label">Conversation ID</span>
            <span class="info-value mono">{{ conversationId || 'N/A' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Plan ID</span>
            <span class="info-value mono">{{ planId }}</span>
          </div>
        </div>

        <!-- 计划步骤 -->
        <div class="plan-section" v-if="planSteps.length">
          <div class="section-header">
            <span class="section-title">执行计划</span>
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
        <div class="empty-state" v-if="!planId && !showOutputPanel">
          <div class="empty-icon">
            <el-icon :size="48"><Promotion /></el-icon>
          </div>
          <p class="empty-text">开始对话后，这里会显示执行详情</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue';
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
  User,
  ChatDotRound,
  Delete,
  Close,
  Monitor,
} from '@element-plus/icons-vue';
import { executeByToolAsync, getExecutionDetails, getTaskStatus, stopTask, submitUserInput } from '@/api/executor';
import { streamSse, type SseMessage } from '@/utils/sse';
import ExecutionStepViewer from '@/components/ExecutionStepViewer.vue';
import { marked } from 'marked';
import 'highlight.js/styles/github.css';

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// ========== 配置 marked 以支持代码高亮 ==========
marked.setOptions({
  breaks: true,
  gfm: true,
});

// ========== 响应式数据 ==========
const taskInput = ref('');
const conversationId = ref('');
const conversationHistory = ref<Array<{ role: 'user' | 'assistant'; content: string; timestamp: Date }>>([]);
const submitting = ref(false);
const submittingInput = ref(false);
const executing = ref(false);
const planId = ref('');
const statusInfo = ref<any>(null);
const detailRaw = ref('');
const planStreamController = ref<AbortController | null>(null);
const waitForm = ref<Record<string, string>>({});
const showOutputPanel = ref(false);
const messagesListRef = ref<HTMLElement>();

// ========== 计算属性 ==========
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

// ========== Markdown 渲染 ==========
const renderMarkdown = (content: string) => {
  return marked(content);
};

// ========== 时间格式化 ==========
const formatTime = (timestamp: Date) => {
  const now = new Date();
  const diff = now.getTime() - timestamp.getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}小时前`;
  const days = Math.floor(hours / 24);
  return `${days}天前`;
};

// ========== 滚动到底部 ==========
const scrollToBottom = async () => {
  await nextTick();
  if (messagesListRef.value) {
    messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight;
  }
};

// ========== 示例任务 ==========
const useExample = (example: string) => {
  taskInput.value = example;
};

// ========== 清空对话 ==========
const clearConversation = () => {
  conversationHistory.value = [];
  conversationId.value = '';
  planId.value = '';
  detailRaw.value = '';
  statusInfo.value = null;
  showOutputPanel.value = false;
  ElMessage.success('对话已清空');
};

// ========== 关闭输出面板 ==========
const closeOutputPanel = () => {
  showOutputPanel.value = false;
};

// ========== 预填充等待表单 ==========
const prefillWaitForm = () => {
  if (!waitFields.value.length) return;
  waitFields.value.forEach((f) => {
    if (waitForm.value[f.name] === undefined) {
      waitForm.value[f.name] = '';
    }
  });
};

// ========== 处理任务流消息 ==========
const handleTaskStreamMessage = (data: SseMessage) => {
  if (!data) return;
  if (data.type === 'error') {
    ElMessage.error(data.message || '任务流连接异常');
    return;
  }
  if (data.planId && !planId.value) {
    planId.value = data.planId as string;
    showOutputPanel.value = true;
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
    executing.value = false;
  }
};

// ========== 开始任务流 ==========
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

// ========== 提交任务 ==========
const submitTask = async () => {
  if (!taskInput.value.trim()) {
    ElMessage.warning('请输入任务描述');
    return;
  }

  // 添加用户消息到历史
  const userMessage = {
    role: 'user' as const,
    content: taskInput.value,
    timestamp: new Date(),
  };
  conversationHistory.value.push(userMessage);

  // 如果没有 conversationId，后端会自动生成
  const currentConversationId = conversationId.value || undefined;

  submitting.value = true;
  executing.value = true;
  showOutputPanel.value = true;

  try {
    const payload = {
      toolName: 'auto_general_exec', // 使用通用智能工具，后端会自动选择模板
      requestSource: 'VUE_DIALOG' as any,
      conversationId: currentConversationId,
      replacementParams: {
        task: taskInput.value,
        input: taskInput.value,
        prompt: taskInput.value,
      },
    };

    const res = await executeByToolAsync(payload);
    const responseData = res.data as any;

    // 保存 conversationId（后端返回）
    if (responseData.conversationId) {
      conversationId.value = responseData.conversationId;
    }

    planId.value = responseData.planId || '';
    statusInfo.value = responseData.status || 'processing';
    detailRaw.value = JSON.stringify(responseData, null, 2);

    // 开始流式监听
    if (planId.value) {
      startTaskStream(planId.value);
    }

    ElMessage.success('任务已提交');
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.error || '提交失败');
    executing.value = false;
  } finally {
    submitting.value = false;
    taskInput.value = '';
    await scrollToBottom();
  }
};

// ========== 刷新 ==========
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

// ========== 停止执行 ==========
const stopExecution = async () => {
  if (planId.value) {
    try {
      await stopTask(planId.value);
      ElMessage.success('已发送停止指令');
    } catch (error) {
      ElMessage.error('停止失败');
    }
  }
  executing.value = false;
  planStreamController.value?.abort();
  planStreamController.value = null;
};

const stopNow = async () => {
  await stopExecution();
};

// ========== 提交等待输入 ==========
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

// ========== 复制详情 ==========
const copyDetail = async () => {
  if (!detailText.value) return;
  await navigator.clipboard.writeText(detailText.value);
  ElMessage.success('已复制');
};

// ========== 处理回车键 ==========
const handleEnterKey = (event: KeyboardEvent) => {
  if (event.shiftKey) {
    // Shift + Enter 换行
    return;
  }
  event.preventDefault();
  submitTask();
};

// ========== 生命周期 ==========
onBeforeUnmount(() => {
  planStreamController.value?.abort();
});

// ========== 监听对话历史变化 ==========
watch(conversationHistory, () => {
  scrollToBottom();
}, { deep: true });
</script>

<style scoped>
.agent-page {
  display: grid;
  grid-template-columns: 450px 1fr;
  gap: 24px;
  min-height: calc(100vh - 112px);
  padding: 0;
}

/* ========== 对话面板 ========== */
.chat-panel {
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
  padding: 20px 24px;
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

/* ========== 消息容器 ========== */
.messages-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-secondary);
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: var(--bg-tertiary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.empty-text {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
  text-align: center;
}

.example-tasks {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.task-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: var(--bg-primary);
  border: 2px solid var(--border-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-primary);
  font-size: 14px;
}

.task-chip:hover {
  border-color: var(--primary-color);
  background: var(--bg-hover);
  transform: translateX(4px);
}

/* ========== 消息项 ========== */
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: messageSlideIn 0.3s ease-out;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-item.user .message-avatar {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
}

.message-item.assistant .message-avatar {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.message-content {
  flex: 1;
  max-width: 85%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.message-role {
  font-weight: 600;
}

.message-item.user .message-role {
  color: var(--primary-color);
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  line-height: 1.6;
  word-wrap: break-word;
  font-size: 14px;
  color: var(--text-primary);
}

.message-item.user .message-text {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
  border: none;
}

.message-item.streaming .message-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ========== 打字指示器 ========== */
.typing-indicator {
  display: flex;
  gap: 4px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--primary-color);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

/* ========== 输入区域 ========== */
.input-section {
  padding: 20px 24px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-primary);
}

.task-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 14px 16px;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  border: 2px solid var(--border-color);
  transition: all 0.2s ease;
}

.task-input :deep(.el-textarea__inner:focus) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  justify-content: flex-end;
}

/* ========== 输出面板 ========== */
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

/* ========== 任务信息 ========== */
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

/* ========== 计划步骤 ========== */
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

/* ========== 等待输入 ========== */
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

/* ========== 执行步骤 ========== */
.execution-section,
.result-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

/* ========== 结果 ========== */
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

/* ========== 动画 ========== */
@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-enter-active,
.message-leave-active {
  transition: all 0.3s ease;
}

.message-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.message-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* ========== Markdown 样式 ========== */
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  font-size: 14px;
  line-height: 1.45;
  word-wrap: break-word;
}

.markdown-body code {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  font-family: SFMono-Regular, Consolas, "Liberation Mono", Menlo, Courier, monospace;
}

.markdown-body pre {
  word-wrap: normal;
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 12px;
  overflow: auto;
}

.markdown-body pre code {
  padding: 0;
  margin: 0;
  font-size: 14px;
  background-color: transparent;
  border: 0;
  white-space: pre;
}

.markdown-body p {
  margin: 0 0 10px;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 1.5em;
  margin: 0 0 10px;
}

/* ========== 响应式 ========== */
@media (max-width: 1200px) {
  .agent-page {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .chat-panel {
    max-height: none;
  }
}

@media (max-width: 768px) {
  .task-info {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
