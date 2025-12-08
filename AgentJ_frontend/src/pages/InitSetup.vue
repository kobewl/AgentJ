<template>
  <div class="page-wrapper">
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <span>初始化向导</span>
          <el-tag :type="initStatus?.initialized ? 'success' : 'warning'">
            {{ initStatus?.initialized ? '已完成' : '未完成' }}
          </el-tag>
        </div>
      </template>

      <el-alert
        title="填写模型接入信息（DashScope 或 OpenAI 兼容接口）"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-form :model="form" label-width="140px" :disabled="submitting">
        <el-form-item label="配置模式">
          <el-radio-group v-model="form.configMode">
            <el-radio-button label="dashscope">DashScope 默认</el-radio-button>
            <el-radio-button label="custom">自定义(OpenAI兼容)</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="API Key" required>
          <el-input v-model="form.apiKey" type="password" placeholder="输入模型 API Key" show-password />
        </el-form-item>

        <template v-if="form.configMode === 'custom'">
          <el-form-item label="Base URL" required>
            <el-input v-model="form.baseUrl" placeholder="https://your-host/v1" />
          </el-form-item>
          <el-form-item label="模型名称" required>
            <el-input v-model="form.modelName" placeholder="gpt-4o-mini / qwen-long / ..." />
          </el-form-item>
          <el-form-item label="显示名称">
            <el-input v-model="form.modelDisplayName" placeholder="可选：前端展示名称" />
          </el-form-item>
          <el-form-item label="Completions Path">
            <el-input v-model="form.completionsPath" placeholder="/v1/chat/completions" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存并设为默认模型</el-button>
          <el-button link type="info" @click="refreshStatus">刷新状态</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-divider />
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <span>当前状态</span>
        </div>
      </template>
      <el-descriptions :column="2" border v-if="initStatus">
        <el-descriptions-item label="已初始化">{{ initStatus.initialized ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="已有模型">{{ initStatus.hasConfiguredModels ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="错误">{{ initStatus.error || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getInitStatus, saveInitConfig, type InitConfigRequest } from '@/api/init';
import type { InitStatus } from '@/api/types';

const initStatus = ref<InitStatus>();
const submitting = ref(false);

const form = reactive<InitConfigRequest>({
  configMode: 'dashscope',
  apiKey: '',
  baseUrl: '',
  modelName: '',
  modelDisplayName: '',
  completionsPath: '/v1/chat/completions',
});

const refreshStatus = async () => {
  try {
    const res = await getInitStatus();
    initStatus.value = res.data;
  } catch (error) {
    console.error('获取初始化状态失败', error);
  }
};

const onSubmit = async () => {
  if (!form.apiKey) {
    ElMessage.warning('请输入 API Key');
    return;
  }
  submitting.value = true;
  try {
    await saveInitConfig(form);
    ElMessage.success('已保存初始化配置');
    await refreshStatus();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.error || '保存失败');
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  refreshStatus();
});
</script>

