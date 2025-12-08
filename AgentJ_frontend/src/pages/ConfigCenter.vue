<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-toolbar">
        <span>配置中心</span>
        <div class="flex-row">
          <el-input v-model="group" placeholder="输入 configGroup，如: llm" clearable style="width: 260px" />
          <el-button type="primary" :loading="loading" @click="loadConfigs">加载配置</el-button>
          <el-button type="success" :disabled="configs.length === 0" @click="save">保存修改</el-button>
          <el-popconfirm title="确定重置为默认值？" @confirm="resetAll">
            <template #reference>
              <el-button type="danger" :disabled="configs.length === 0">重置全部默认</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </template>

    <el-table :data="configs" v-loading="loading" border style="width: 100%">
      <el-table-column prop="configPath" label="路径" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="220" />
      <el-table-column prop="configValue" label="当前值" min-width="220">
        <template #default="scope">
          <el-input v-model="scope.row.configValue" type="textarea" :rows="1" />
        </template>
      </el-table-column>
      <el-table-column prop="defaultValue" label="默认值" min-width="160" />
      <el-table-column prop="inputType" label="类型" width="120" />
      <el-table-column prop="updateTime" label="更新时间" width="180" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { batchUpdateConfigs, getConfigsByGroup, resetAllDefaults } from '@/api/config';
import type { ConfigEntity } from '@/api/types';

const group = ref('');
const configs = ref<ConfigEntity[]>([]);
const loading = ref(false);

const loadConfigs = async () => {
  if (!group.value) {
    ElMessage.warning('请输入配置组名');
    return;
  }
  loading.value = true;
  try {
    const res = await getConfigsByGroup(group.value);
    configs.value = res.data || [];
  } catch (error) {
    ElMessage.error('加载配置失败');
  } finally {
    loading.value = false;
  }
};

const save = async () => {
  if (!configs.value.length) return;
  try {
    await batchUpdateConfigs(configs.value);
    ElMessage.success('已保存配置');
  } catch (error) {
    ElMessage.error('保存失败');
  }
};

const resetAll = async () => {
  try {
    await resetAllDefaults();
    ElMessage.success('已重置为默认值');
    if (group.value) {
      await loadConfigs();
    }
  } catch (error) {
    ElMessage.error('重置失败');
  }
};
</script>

