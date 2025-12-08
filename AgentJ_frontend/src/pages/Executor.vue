<template>
  <el-row :gutter="16">
    <el-col :span="14">
      <el-card shadow="never">
        <template #header>
          <div class="card-toolbar">
            <span>按工具名称执行计划</span>
            <el-tag type="info">调用 /api/executor/*</el-tag>
          </div>
        </template>
        <el-form :model="form" label-width="120px">
          <el-form-item label="Tool Name" required>
            <el-input v-model="form.toolName" placeholder="coordinator 工具名称" />
          </el-form-item>
          <el-form-item label="Service Group">
            <el-input v-model="form.serviceGroup" placeholder="可选：服务组" />
          </el-form-item>
          <el-form-item label="Request Source">
            <el-select v-model="form.requestSource" placeholder="请求来源">
              <el-option label="VUE_DIALOG" value="VUE_DIALOG" />
              <el-option label="VUE_SIDEBAR" value="VUE_SIDEBAR" />
              <el-option label="HTTP_REQUEST" value="HTTP_REQUEST" />
            </el-select>
          </el-form-item>
          <el-form-item label="ConversationId">
            <el-input v-model="form.conversationId" placeholder="可留空自动生成" />
          </el-form-item>
          <el-form-item label="参数替换(JSON)">
            <el-input
              v-model="replacementJson"
              type="textarea"
              :rows="4"
              placeholder='{"project":"demo"}'
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="running" @click="runSync">同步执行</el-button>
            <el-button type="success" :loading="running" @click="runAsync">提交异步</el-button>
          </el-form-item>
        </el-form>
        <el-alert v-if="resultMessage" :title="resultMessage" type="success" show-icon closable />
      </el-card>
    </el-col>
    <el-col :span="10">
      <el-card shadow="never">
        <template #header>
          <div class="card-toolbar">
            <span>任务状态</span>
          </div>
        </template>
        <el-form label-width="110px">
          <el-form-item label="Plan ID">
            <el-input v-model="planId" placeholder="执行后返回的 planId" />
          </el-form-item>
          <el-form-item>
            <el-button :disabled="!planId" @click="loadDetails">查看详情</el-button>
            <el-button type="danger" :disabled="!planId" @click="stop">停止任务</el-button>
          </el-form-item>
        </el-form>
        <el-alert v-if="taskStatus" title="状态" type="info" :closable="false" />
        <el-input
          v-if="detail"
          v-model="detail"
          type="textarea"
          readonly
          :rows="12"
          style="margin-top: 12px"
        />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { executeByToolAsync, executeByToolSync, getExecutionDetails, getTaskStatus, stopTask } from '@/api/executor';
import type { ExecuteRequest } from '@/api/executor';

const form = ref<ExecuteRequest>({
  toolName: '',
  requestSource: 'VUE_DIALOG',
  serviceGroup: '',
  conversationId: '',
});

const replacementJson = ref('');
const running = ref(false);
const resultMessage = ref('');
const planId = ref('');
const detail = ref('');
const taskStatus = ref('');

const buildReplacementParams = () => {
  if (!replacementJson.value) return undefined;
  try {
    return JSON.parse(replacementJson.value);
  } catch (error) {
    ElMessage.error('参数替换 JSON 解析失败');
    throw error;
  }
};

const runSync = async () => {
  if (!form.value.toolName) {
    ElMessage.warning('请输入 toolName');
    return;
  }
  running.value = true;
  resultMessage.value = '';
  try {
    const payload: ExecuteRequest = { ...form.value, replacementParams: buildReplacementParams() };
    const res = await executeByToolSync(payload);
    resultMessage.value = res.data?.result || res.data?.message || '已完成';
    planId.value = (res.data as any)?.planId || planId.value;
    ElMessage.success('同步执行完成');
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.error || '执行失败');
  } finally {
    running.value = false;
  }
};

const runAsync = async () => {
  if (!form.value.toolName) {
    ElMessage.warning('请输入 toolName');
    return;
  }
  running.value = true;
  resultMessage.value = '';
  try {
    const payload: ExecuteRequest = { ...form.value, replacementParams: buildReplacementParams() };
    const res = await executeByToolAsync(payload);
    planId.value = (res.data as any)?.planId || '';
    resultMessage.value = res.data?.message || '任务已提交';
    ElMessage.success('异步任务提交成功');
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.error || '提交失败');
  } finally {
    running.value = false;
  }
};

const loadDetails = async () => {
  if (!planId.value) return;
  try {
    const [detailRes, statusRes] = await Promise.all([
      getExecutionDetails(planId.value),
      getTaskStatus(planId.value),
    ]);
    detail.value = detailRes.data as string;
    taskStatus.value = JSON.stringify(statusRes.data, null, 2);
  } catch (error) {
    ElMessage.error('查询失败');
  }
};

const stop = async () => {
  if (!planId.value) return;
  try {
    await stopTask(planId.value);
    ElMessage.success('已发送停止指令');
  } catch (error) {
    ElMessage.error('停止失败');
  }
};
</script>

