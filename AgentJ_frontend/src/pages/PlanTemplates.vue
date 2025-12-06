<template>
  <div class="page-wrapper">
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <span>计划模板列表</span>
          <div class="actions">
            <el-button @click="load" :loading="loading">刷新</el-button>
            <el-button type="primary" @click="showCreate">新建模板</el-button>
          </div>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" border style="width: 100%">
        <el-table-column prop="planTemplateId" label="模板ID" min-width="200" />
        <el-table-column prop="title" label="标题/工具名" min-width="200" />
        <el-table-column prop="planType" label="类型" width="120" />
        <el-table-column prop="serviceGroup" label="服务组" width="120" />
        <el-table-column prop="directResponse" label="直接响应" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.directResponse ? 'success' : 'info'">{{ scope.row.directResponse ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <div class="table-actions">
              <el-button size="small" @click="viewDetail(scope.row.planTemplateId)">详情</el-button>
              <el-popconfirm title="确认删除该模板?" @confirm="remove(scope.row.planTemplateId)">
                <template #reference>
                  <el-button size="small" type="danger" plain>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" :title="currentConfig?.title || '模板详情'" size="50%">
      <div v-if="currentConfig">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模板ID">{{ currentConfig.planTemplateId }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ currentConfig.planType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务组">{{ currentConfig.serviceGroup || '-' }}</el-descriptions-item>
          <el-descriptions-item label="直接响应">{{ currentConfig.directResponse ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="工具">{{ currentConfig.toolConfig?.toolName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="访问级别">{{ currentConfig.accessLevel || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">步骤</h4>
        <el-timeline v-if="currentConfig.steps?.length">
          <el-timeline-item v-for="(step, index) in currentConfig.steps" :key="index" :timestamp="step.modelName || ''">
            <p><strong>需求:</strong> {{ step.stepRequirement }}</p>
            <p><strong>Agent:</strong> {{ step.agentName || '-' }}</p>
            <p><strong>工具:</strong> {{ (step.selectedToolKeys || []).join(', ') || '-' }}</p>
          </el-timeline-item>
        </el-timeline>
        <p v-else>暂无步骤数据</p>

        <h4 class="section-title" style="margin-top: 12px">参数占位符</h4>
        <el-empty v-if="!parameterReq?.parameters?.length" description="未检测到参数" />
        <el-table v-else :data="parameterReq?.requirements" border style="width: 100%">
          <el-table-column prop="name" label="参数" />
          <el-table-column prop="type" label="类型" />
          <el-table-column prop="description" label="描述" />
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="createVisible" title="新建计划模板" width="640px">
      <el-form label-width="120px" :model="createForm">
        <el-form-item label="模板ID" required>
          <el-input v-model="createForm.planTemplateId" placeholder="唯一ID，如 my-tool-001" />
        </el-form-item>
        <el-form-item label="标题/工具名" required>
          <el-select
            v-model="createForm.title"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入工具名（title 同时作为 toolName）"
            @change="handleToolSelect"
          >
            <el-option
              v-for="opt in toolOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <p class="form-hint">下拉展示后端已有的工具名（title），也可直接输入新名称。</p>
        </el-form-item>
        <el-form-item label="服务组">
          <el-input v-model="createForm.serviceGroup" placeholder="默认为 ungrouped" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="createForm.planType" placeholder="dynamic_agent" />
        </el-form-item>
        <el-form-item label="直接响应">
          <el-switch v-model="createForm.directResponse" />
        </el-form-item>
        <el-form-item label="工具描述">
          <el-input v-model="createForm.toolDescription" placeholder="可选，描述这个工具/计划" />
        </el-form-item>
        <el-form-item label="planJson (steps)" required>
          <el-input
            v-model="createForm.planJson"
            type="textarea"
            :rows="10"
            placeholder='填写包含 steps 的 JSON，至少包含 stepRequirement/agentName 等'
          />
          <p class="form-hint">仅提取其中的 steps 字段用于计划执行；参数占位符从 steps 中自动分析。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createVisible = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="submitCreate">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { createOrUpdatePlanTemplateWithTool, deletePlanTemplate, getParameterRequirements, getPlanTemplateConfig, listPlanTemplateConfigs } from '@/api/planTemplate';
import type { PlanTemplateConfigVO } from '@/api/types';

const items = ref<PlanTemplateConfigVO[]>([]);
const loading = ref(false);
const detailVisible = ref(false);
const currentConfig = ref<PlanTemplateConfigVO>();
const parameterReq = ref<any>();
const createVisible = ref(false);
const creating = ref(false);
const createForm = ref({
  planTemplateId: '',
  title: '',
  planType: 'dynamic_agent',
  serviceGroup: 'ungrouped',
  directResponse: false,
  toolDescription: '',
  planJson: `{
  "title": "<<填写工具名/标题>>",
  "planTemplateId": "<<唯一ID，例如 my-tool-001>>",
  "planType": "dynamic_agent",
  "directResponse": false,
  "steps": [
    {
      "stepRequirement": "<<input>>",
      "agentName": "ConfigurableDynaAgent",
      "modelName": "",
      "terminateColumns": ""
    }
  ]
}`
});

const load = async () => {
  loading.value = true;
  try {
    const res = await listPlanTemplateConfigs();
    items.value = res.data || [];
  } catch (error) {
    ElMessage.error('加载模板失败');
  } finally {
    loading.value = false;
  }
};

const toolOptions = computed(() => {
  const seen = new Set<string>();
  return items.value
    .filter((tpl) => tpl.title)
    .map((tpl) => {
      const val = tpl.title || '';
      if (seen.has(val)) return null;
      seen.add(val);
      return {
        label: `${tpl.title}${tpl.serviceGroup ? ` · ${tpl.serviceGroup}` : ''}`,
        value: tpl.title || '',
        planTemplateId: tpl.planTemplateId,
        serviceGroup: tpl.serviceGroup,
      };
    })
    .filter(Boolean) as { label: string; value: string; planTemplateId?: string; serviceGroup?: string }[];
});

const viewDetail = async (id: string) => {
  try {
    const [cfgRes, paramRes] = await Promise.all([
      getPlanTemplateConfig(id),
      getParameterRequirements(id),
    ]);
    currentConfig.value = cfgRes.data;
    parameterReq.value = paramRes.data;
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error('获取详情失败');
  }
};

const remove = async (id: string) => {
  try {
    await deletePlanTemplate(id);
    ElMessage.success('已删除模板');
    load();
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

const showCreate = () => {
  createVisible.value = true;
};

const handleToolSelect = (value: string) => {
  const found = toolOptions.value.find((o) => o.value === value);
  if (found) {
    createForm.value.planTemplateId = found.planTemplateId || createForm.value.planTemplateId;
    createForm.value.serviceGroup = found.serviceGroup || createForm.value.serviceGroup || 'ungrouped';
  }
};

const submitCreate = async () => {
  if (!createForm.value.planTemplateId || !createForm.value.title) {
    ElMessage.warning('请填写模板ID和标题（即工具名）');
    return;
  }
  creating.value = true;
  try {
    let steps: any[] = [];
    try {
      const parsed = JSON.parse(createForm.value.planJson);
      steps = parsed.steps || [];
    } catch (err) {
      ElMessage.error('planJson 不是合法的 JSON');
      creating.value = false;
      return;
    }

    const payload: any = {
      planTemplateId: createForm.value.planTemplateId,
      title: createForm.value.title,
      planType: createForm.value.planType || 'dynamic_agent',
      serviceGroup: createForm.value.serviceGroup || 'ungrouped',
      directResponse: !!createForm.value.directResponse,
      steps,
      toolConfig: {
        toolDescription: createForm.value.toolDescription || createForm.value.title,
        enableInternalToolcall: true,
        enableHttpService: false,
        enableInConversation: false,
        publishStatus: 'PUBLISHED',
        inputSchema: [],
      },
    };

    await createOrUpdatePlanTemplateWithTool(payload);
    ElMessage.success('已创建/更新模板');
    createVisible.value = false;
    load();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.error || '创建失败');
  } finally {
    creating.value = false;
  }
};

onMounted(load);
</script>

<style scoped>
.card-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.actions {
  display: flex;
  gap: 8px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.form-hint {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
