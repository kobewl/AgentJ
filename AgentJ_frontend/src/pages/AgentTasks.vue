<template>
  <div class="page-wrapper">
    <el-row :gutter="16">
      <el-col :span="13">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="card-toolbar">
              <div class="title">
                <el-icon><Cpu /></el-icon>
                <span>Agent 调用</span>
              </div>
              <el-button type="primary" :loading="submitting" @click="submitTask">提交任务</el-button>
            </div>
          </template>

          <el-form :model="form" label-width="110px">
            <el-form-item label="快捷类别">
              <el-radio-group v-model="selectedCategory" @change="onCategoryChange">
                <el-radio-button
                  v-for="cat in quickCategories"
                  :key="cat.planTemplateId"
                  :label="cat.planTemplateId"
                >
                  {{ cat.label }}
                </el-radio-button>
              </el-radio-group>
              <p class="field-help">无需模板，选类别即可；默认使用通用执行</p>
            </el-form-item>

            <el-form-item label="任务描述">
              <el-input
                v-model="taskInput"
                type="textarea"
                :rows="3"
                placeholder="写明要让 Agent 完成的任务"
              />
            </el-form-item>

            <el-form-item label="附加参数(JSON)">
              <el-input
                v-model="extraParams"
                type="textarea"
                :rows="3"
                placeholder='可选：{"urls":"https://xxx","goal":"输出要点+来源"}'
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="11">
        <el-card shadow="never" class="panel status-panel">
          <template #header>
            <div class="card-toolbar">
              <span>任务状态</span>
              <el-space>
                <el-button size="small" :disabled="!planId" @click="refresh">刷新</el-button>
                <el-button size="small" type="danger" plain :disabled="!planId" @click="stopNow">停止</el-button>
              </el-space>
            </div>
          </template>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="Plan ID">
              <span class="mono">{{ planId || '尚未提交' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="Status">
              <el-tag :type="statusTagType">{{ statusText }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="类别">
              <span>{{ categoryLabel }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="Tool">
              <span class="mono">{{ derivedToolName }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-divider />

          <div class="section" v-if="waitState?.waiting">
            <div class="section-head">
              <span>等待用户输入</span>
              <el-tag type="warning" size="small">暂停中</el-tag>
            </div>
            <p class="muted">{{ waitState?.formDescription || waitState?.title }}</p>
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
            <el-button type="primary" size="small" :loading="submittingInput" @click="submitWaitInputs">
              提交输入
            </el-button>
          </div>

          <div class="section" v-if="agentTimeline.length">
            <div class="section-head">
              <span>执行步骤</span>
              <el-tag size="small" type="info">AgentExecutionSequence</el-tag>
            </div>
            <div class="timeline-scroll">
              <el-timeline>
                <el-timeline-item
                  v-for="(item, idx) in agentTimeline"
                  :key="item.stepId || idx"
                  :type="item.tagType"
                  :timestamp="item.status"
                >
                  <div class="timeline-item">
                    <p class="timeline-title">{{ item.agentName || '未命名步骤' }}</p>
                    <p class="muted">stepId: {{ item.stepId || '-' }}</p>
                    <p class="muted">model: {{ item.modelName || '-' }}</p>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>

          <div class="section" v-if="thinkActTimeline.length">
            <div class="section-head">
              <span>执行过程</span>
              <el-tag size="small" type="info">Think / Act</el-tag>
            </div>
            <div class="timeline-scroll">
              <el-timeline>
                <el-timeline-item
                  v-for="(item, idx) in thinkActTimeline"
                  :key="`${item.stepId || 'step'}-${idx}`"
                  :timestamp="item.status || ''"
                >
                  <div class="timeline-item">
                    <p class="timeline-title">{{ item.agentName }}</p>
                    <p class="muted">stepId: {{ item.stepId || '-' }} · 环节 #{{ item.idx + 1 }}</p>
                    <p v-if="item.think" class="muted">思考: {{ item.think }}</p>
                    <p v-if="item.action" class="muted">行动: {{ item.action }}<span v-if="item.tool"> ({{ item.tool }})</span></p>
                    <p v-if="item.result" class="muted">结果: {{ item.result }}</p>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </div>

          <div class="section">
            <div class="section-head">
              <span>最新输出</span>
              <el-button text size="small" :disabled="!detailRaw" @click="copyDetail">复制</el-button>
            </div>
            <el-input v-model="detailText" type="textarea" :rows="10" readonly />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Cpu } from '@element-plus/icons-vue';
import { executeByToolAsync, getExecutionDetails, getTaskStatus, stopTask, submitUserInput } from '@/api/executor';
import { streamSse, type SseMessage } from '@/utils/sse';

const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const quickCategories = [
  { label: '浏览器调研/操作', planTemplateId: 'auto-browser-plan', toolName: 'auto_browser_exec' },
  { label: '通用智能执行', planTemplateId: 'auto-general-plan', toolName: 'auto_general_exec' },
];

const submitting = ref(false);
const submittingInput = ref(false);

const selectedCategory = ref<string | null>(null);
const taskInput = ref('');
const extraParams = ref('');

const form = ref({
  requestSource: 'VUE_DIALOG',
  conversationId: undefined,
});

const planId = ref('');
const statusInfo = ref<any>(null);
const detailRaw = ref('');
const planStreamController = ref<AbortController | null>(null);

const statusText = computed(() => {
  if (!statusInfo.value) return '未开始';
  return typeof statusInfo.value === 'string' ? statusInfo.value : JSON.stringify(statusInfo.value);
});

const categoryLabel = computed(() => {
  const found = quickCategories.find((c) => c.planTemplateId === selectedCategory.value);
  return found?.label || selectedCategory.value || '-';
});

const derivedToolName = computed(() => {
  const found = quickCategories.find((c) => c.planTemplateId === selectedCategory.value);
  if (found?.toolName) return found.toolName;
  return 'auto_general_exec';
});

const statusTagType = computed(() => {
  const text = statusText.value.toLowerCase();
  if (text.includes('processing') || text.includes('running')) return 'warning';
  if (text.includes('success') || text.includes('finish') || text.includes('completed')) return 'success';
  if (text.includes('fail') || text.includes('error')) return 'danger';
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
const waitForm = ref<Record<string, string>>({});

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

const agentTimeline = computed(() => {
  const seq = detailJson.value?.agentExecutionSequence || [];
  return seq.map((item: any) => {
    const status = (item.status || '').toString().toLowerCase();
    let tagType: 'primary' | 'success' | 'warning' | 'danger' | 'info' = 'info';
    if (status.includes('running')) tagType = 'primary';
    else if (status.includes('finished') || status.includes('success')) tagType = 'success';
    else if (status.includes('fail') || status.includes('error')) tagType = 'danger';
    return { ...item, tagType };
  });
});

const detailText = computed(() => {
  if (!detailJson.value) return detailRaw.value || '';
  if (detailJson.value.summary) return detailJson.value.summary;
  if (detailJson.value.structureResult) return detailJson.value.structureResult;
  return detailRaw.value;
});

const thinkActTimeline = computed(() => {
  const seq = detailJson.value?.agentExecutionSequence || [];
  const rows: Array<{
    agentName: string;
    stepId: string;
    idx: number;
    think?: string;
    action?: string;
    result?: string;
    tool?: string;
    status?: string;
  }> = [];
  seq.forEach((agent: any) => {
    const steps = agent?.thinkActSteps || [];
    steps.forEach((step: any, idx: number) => {
      rows.push({
        agentName: agent?.agentName || agent?.stepId || `步骤${idx + 1}`,
        stepId: agent?.stepId,
        idx,
        think: step?.thinkOutput || step?.thinkInput,
        action: step?.actionDescription || step?.toolName,
        result: step?.actionResult || step?.thinkOutput,
        tool: step?.toolName,
        status: step?.status || agent?.status,
      });
    });
  });
  return rows;
});

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

const onCategoryChange = (val: string) => {
  const found = quickCategories.find((c) => c.planTemplateId === val);
  if (!found) {
    ElMessage.info('未选择类别，将使用通用执行');
  }
};

const parseExtraParams = () => {
  if (!extraParams.value.trim()) return {};
  try {
    return JSON.parse(extraParams.value);
  } catch (error) {
    ElMessage.error('附加参数 JSON 解析失败');
    throw error;
  }
};

const buildReplacementParams = () => {
  const params: Record<string, unknown> = {};
  if (taskInput.value) {
    params.input = taskInput.value;
    params.prompt = taskInput.value;
  }
  return { ...params, ...parseExtraParams() };
};

const submitTask = async () => {
  // toolName 由类别推导，无类别则使用通用
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
    ElMessage.success('已提交 Agent 任务');
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
  if (!detailRaw.value) return;
  await navigator.clipboard.writeText(detailRaw.value);
  ElMessage.success('已复制');
};

onBeforeUnmount(() => {
  planStreamController.value?.abort();
});
</script>

<style scoped>
.page-wrapper {
  padding: 8px;
}

.panel {
  height: 100%;
}

.card-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.status-panel .section {
  margin-bottom: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  font-weight: 600;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.timeline-title {
  margin: 0;
  font-weight: 600;
}

.muted {
  color: #94a3b8;
  margin: 0 0 4px;
}

.mono {
  font-family: SFMono-Regular, Consolas, Menlo, monospace;
}

.wait-form {
  margin-top: 8px;
}

.timeline-scroll {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 6px;
}
</style>
