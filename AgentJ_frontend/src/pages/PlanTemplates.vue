<template>
  <div class="page-wrapper">
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <span>计划模板列表</span>
          <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" border style="width: 100%">
        <el-table-column prop="planTemplateId" label="模板ID" min-width="200" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="planType" label="类型" width="120" />
        <el-table-column prop="serviceGroup" label="服务组" width="120" />
        <el-table-column prop="directResponse" label="直接响应" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.directResponse ? 'success' : 'info'">{{ scope.row.directResponse ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="toolConfig.toolName" label="工具名" min-width="160">
          <template #default="scope">{{ scope.row.toolConfig?.toolName || '-' }}</template>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { deletePlanTemplate, getParameterRequirements, getPlanTemplateConfig, listPlanTemplateConfigs } from '@/api/planTemplate';
import type { PlanTemplateConfigVO } from '@/api/types';

const items = ref<PlanTemplateConfigVO[]>([]);
const loading = ref(false);
const detailVisible = ref(false);
const currentConfig = ref<PlanTemplateConfigVO>();
const parameterReq = ref<any>();

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

onMounted(load);
</script>
