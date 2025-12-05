<template>
  <div class="grid">
    <el-card shadow="hover">
      <template #header>
        <div class="card-toolbar">
          <span>初始化状态</span>
          <el-tag :type="initStatus?.initialized ? 'success' : 'warning'">
            {{ initStatus?.initialized ? '已完成' : '未完成' }}
          </el-tag>
        </div>
      </template>
      <p>默认模型配置：{{ initStatus?.hasConfiguredModels ? '已存在' : '未配置' }}</p>
      <el-button type="primary" link @click="go('/init')">前往配置</el-button>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="card-toolbar">
          <span>可用模型</span>
          <el-tag type="info">{{ modelOptions.length }} 个</el-tag>
        </div>
      </template>
      <el-space wrap>
        <el-tag v-for="m in modelOptions" :key="m.value" type="success" effect="plain">
          {{ m.label }}
        </el-tag>
      </el-space>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="card-toolbar">
          <span>计划模板</span>
          <el-tag type="info">{{ planCount }} 个</el-tag>
        </div>
      </template>
      <p>通过计划模板统一编排工具、执行计划。</p>
      <el-button type="primary" link @click="go('/plan-templates')">查看模板</el-button>
    </el-card>
  </div>

  <el-card shadow="never" style="margin-top: 16px">
    <template #header>
      <div class="card-toolbar">
        <span>快捷入口</span>
      </div>
    </template>
    <el-space wrap>
      <el-button type="primary" plain @click="go('/executor')">执行工具/计划</el-button>
      <el-button type="success" plain @click="go('/chat')">LLM 对话</el-button>
      <el-button type="warning" plain @click="go('/memories')">记忆管理</el-button>
      <el-button type="info" plain @click="go('/datasource')">数据库连接</el-button>
      <el-button type="danger" plain @click="go('/cron')">定时任务</el-button>
    </el-space>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getInitStatus } from '@/api/init';
import { getAvailableModels } from '@/api/config';
import { listPlanTemplateConfigs } from '@/api/planTemplate';
import type { InitStatus, ModelOption } from '@/api/types';

const router = useRouter();
const initStatus = ref<InitStatus>();
const modelOptions = ref<ModelOption[]>([]);
const planCount = ref(0);

const go = (path: string) => router.push(path);

const loadData = async () => {
  try {
    const [initRes, modelsRes, plansRes] = await Promise.all([
      getInitStatus(),
      getAvailableModels(),
      listPlanTemplateConfigs(),
    ]);
    initStatus.value = initRes.data;
    modelOptions.value = modelsRes.data.options || [];
    planCount.value = plansRes.data?.length || 0;
  } catch (error) {
    console.error('加载首页数据失败', error);
  }
};

onMounted(loadData);
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}
</style>
