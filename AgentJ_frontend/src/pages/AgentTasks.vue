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
              <el-space>
                <el-button type="primary" :loading="submitting" @click="submitTask">提交任务</el-button>
                <el-button @click="loadTemplates" :loading="loadingTemplates" text>刷新模板</el-button>
              </el-space>
            </div>
          </template>

          <el-form :model="form" label-width="110px">
            <el-form-item label="模板" required>
              <el-select
                v-model="selectedTemplateId"
                filterable
                placeholder="选择模板（会带出 toolName 和参数）"
                :loading="loadingTemplates"
                style="width: 100%"
                @change="handleTemplateChange"
              >
                <el-option
                  v-for="tpl in templates"
                  :key="tpl.planTemplateId"
                  :label="tpl.title || tpl.planTemplateId"
                  :value="tpl.planTemplateId"
                >
                  <div class="option-line">
                    <span class="option-title">{{ tpl.title || tpl.planTemplateId }}</span>
                    <span class="option-meta">{{ tpl.toolConfig?.toolName || '无 toolName' }} · {{ tpl.planType || '未分类' }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="toolName" required>
              <el-select
                v-model="form.toolName"
                filterable
                allow-create
                default-first-option
                placeholder="从模板带出或手动输入"
                style="width: 100%"
              >
                <el-option
                  v-for="opt in toolOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <p class="field-help">
                toolName 来自计划模板的 <code>toolConfig.toolName</code>。下拉为空表示模板未配置工具名，可在“计划模板”页面补充。
              </p>
              <el-alert
                v-if="selectedTemplateId && !currentTemplate?.toolConfig?.toolName"
                type="warning"
                show-icon
                :closable="false"
                title="当前模板未配置 toolName，需手动填写或在模板中添加 toolConfig.toolName"
              />
            </el-form-item>

            <el-form-item label="服务组">
              <el-input v-model="form.serviceGroup" placeholder="可选：服务组(serviceGroup)" />
            </el-form-item>

            <el-form-item label="请求来源">
              <el-select v-model="form.requestSource" placeholder="请求来源">
                <el-option label="VUE_DIALOG" value="VUE_DIALOG" />
                <el-option label="VUE_SIDEBAR" value="VUE_SIDEBAR" />
                <el-option label="HTTP_REQUEST" value="HTTP_REQUEST" />
              </el-select>
            </el-form-item>

            <el-form-item label="ConversationId">
              <el-input v-model="form.conversationId" placeholder="可留空自动生成" />
            </el-form-item>

            <el-form-item label="任务描述">
              <el-input
                v-model="taskInput"
                type="textarea"
                :rows="3"
                placeholder="写明要让 Agent 完成的任务，自动填充到常见占位符 input/prompt"
              />
            </el-form-item>

            <el-form-item v-if="paramPlaceholders.length" label="参数占位符">
              <div class="param-list">
                <div v-for="req in paramPlaceholders" :key="req" class="param-item">
                  <div class="param-head">
                    <span class="param-name">{{ req }}</span>
                    <el-tag size="small" type="info">string</el-tag>
                  </div>
                  <p class="param-desc">填充 &lt;&lt;{{ req }}&gt;&gt; 占位符</p>
                  <el-input
                    v-model="paramInputs[req]"
                    type="textarea"
                    :rows="2"
                    :placeholder="`填充 <<${req}>> 占位符`"
                  />
                </div>
              </div>
            </el-form-item>

            <el-form-item v-if="paramRequirementsText" label="参数说明">
              <el-input v-model="paramRequirementsText" type="textarea" :rows="3" readonly />
            </el-form-item>

            <el-form-item label="附加参数(JSON)">
              <el-input
                v-model="extraParams"
                type="textarea"
                :rows="3"
                placeholder='可选：{"project":"demo"}，会与上方参数合并'
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
            <el-descriptions-item label="模板">
              <span>{{ currentTemplate?.title || currentTemplate?.planTemplateId || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="Tool">
              <span class="mono">{{ currentTemplate?.toolConfig?.toolName || form.toolName || '-' }}</span>
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
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Cpu } from '@element-plus/icons-vue';
import { listPlanTemplateConfigs, getParameterRequirements } from '@/api/planTemplate';
import { executeByToolAsync, getExecutionDetails, getTaskStatus, stopTask, submitUserInput } from '@/api/executor';
import type { PlanTemplateConfigVO } from '@/api/types';

const templates = ref<PlanTemplateConfigVO[]>([]);
const loadingTemplates = ref(false);
const submitting = ref(false);
const submittingInput = ref(false);

const selectedTemplateId = ref('');
const paramPlaceholders = ref<string[]>([]);
const paramRequirementsText = ref<string>('');
const paramInputs = ref<Record<string, string>>({});
const taskInput = ref('');
const extraParams = ref('');

const form = ref({
  toolName: '',
  serviceGroup: '',
  requestSource: 'VUE_DIALOG',
  conversationId: '',
});

const planId = ref('');
const statusInfo = ref<any>(null);
const detailRaw = ref('');

const currentTemplate = computed(() =>
  templates.value.find((tpl) => tpl.planTemplateId === selectedTemplateId.value),
);

const toolOptions = computed(() => {
  const seen = new Set<string>();
  const opts: { label: string; value: string }[] = [];
  templates.value.forEach((tpl) => {
    const name = tpl.toolConfig?.toolName;
    if (name && !seen.has(name)) {
      seen.add(name);
      opts.push({ label: `${name}${tpl.serviceGroup ? ` · ${tpl.serviceGroup}` : ''}`, value: name });
    }
  });
  return opts;
});

const statusText = computed(() => {
  if (!statusInfo.value) return '未开始';
  return typeof statusInfo.value === 'string' ? statusInfo.value : JSON.stringify(statusInfo.value);
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

const loadTemplates = async () => {
  loadingTemplates.value = true;
  try {
    const res = await listPlanTemplateConfigs();
    templates.value = res.data || [];
  } catch (error) {
    ElMessage.error('加载模板失败');
  } finally {
    loadingTemplates.value = false;
  }
};

const handleTemplateChange = async (id: string) => {
  paramPlaceholders.value = [];
  paramRequirementsText.value = '';
  paramInputs.value = {};
  const tpl = templates.value.find((t) => t.planTemplateId === id);
  form.value.toolName = tpl?.toolConfig?.toolName || form.value.toolName;
  form.value.serviceGroup = tpl?.serviceGroup || '';
  if (!id) return;
  try {
    const res = await getParameterRequirements(id);
    const placeholders = (res.data as any)?.parameters || [];
    paramPlaceholders.value = placeholders;
    paramRequirementsText.value = (res.data as any)?.requirements || '';
    placeholders.forEach((name: string) => {
      paramInputs.value[name] = '';
    });
  } catch (error) {
    ElMessage.error('获取参数占位符失败');
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
  Object.entries(paramInputs.value).forEach(([k, v]) => {
    if (v && v.toString().trim() !== '') params[k] = v;
  });
  // 自动把任务描述填到常见占位符
  if (taskInput.value) {
    if (params.input === undefined) params.input = taskInput.value;
    if (params.prompt === undefined && params.input !== taskInput.value) params.prompt = taskInput.value;
  }
  return { ...params, ...parseExtraParams() };
};

const submitTask = async () => {
  if (!form.value.toolName) {
    ElMessage.warning('请先选择或填写 toolName');
    return;
  }
  // 参数校验：若后端需要 parameters 列表全部提供，可在此检查
  if (paramPlaceholders.value.length) {
    const missing = paramPlaceholders.value.filter((k) => !paramInputs.value[k] || paramInputs.value[k].trim() === '');
    if (missing.length) {
      ElMessage.warning(`请填写参数: ${missing.join(', ')}`);
      return;
    }
  }
  submitting.value = true;
  try {
    const payload = {
      toolName: form.value.toolName,
      serviceGroup: form.value.serviceGroup || undefined,
      requestSource: form.value.requestSource as any,
      conversationId: form.value.conversationId || undefined,
      replacementParams: buildReplacementParams(),
    };
    const res = await executeByToolAsync(payload);
    planId.value = (res.data as any)?.planId || '';
    statusInfo.value = (res.data as any)?.status || 'processing';
    detailRaw.value = JSON.stringify(res.data, null, 2);
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
    // 当处于等待输入时，预填表单
    if (waitFields.value.length) {
      waitFields.value.forEach((f) => {
        if (!waitForm.value[f.name]) waitForm.value[f.name] = '';
      });
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

onMounted(loadTemplates);
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

.option-line {
  display: flex;
  flex-direction: column;
}

.option-title {
  font-weight: 600;
}

.option-meta {
  font-size: 12px;
  color: #94a3b8;
}

.param-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.param-item {
  border: 1px dashed #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
  background: #f8fafc;
}

.param-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.param-name {
  font-weight: 600;
  color: #1f2937;
}

.param-desc {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 13px;
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
</style>
