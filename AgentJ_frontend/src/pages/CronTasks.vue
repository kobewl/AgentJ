<template>
  <div class="page-wrapper">
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <span>定时任务</span>
          <div class="flex-row">
            <el-button type="primary" @click="openEdit()">新建</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="items" v-loading="loading" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="任务名" min-width="160" />
        <el-table-column prop="cronExpression" label="Cron" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="260">
          <template #default="scope">
            <div class="table-actions">
              <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
              <el-button size="small" type="success" @click="changeStatus(scope.row, 1)">启用</el-button>
              <el-button size="small" type="warning" @click="changeStatus(scope.row, 0)">停用</el-button>
              <el-button size="small" type="info" @click="execute(scope.row)">执行一次</el-button>
              <el-popconfirm title="确认删除?" @confirm="remove(scope.row)">
                <template #reference>
                  <el-button size="small" type="danger" plain>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer" title="任务配置" size="40%">
      <el-form :model="form" label-width="120px">
        <el-form-item label="任务名" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Cron 表达式" required>
          <el-input v-model="form.cronExpression" placeholder="0 0/5 * * * ?" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  createCronTask,
  deleteCronTask,
  executeCronTask,
  listCronTasks,
  updateCronStatus,
  updateCronTask,
} from '@/api/cron';
import type { CronConfig } from '@/api/types';

const items = ref<CronConfig[]>([]);
const loading = ref(false);
const drawer = ref(false);
const form = reactive<CronConfig>({ name: '', cronExpression: '', description: '', status: 1 });

const load = async () => {
  loading.value = true;
  try {
    const res = await listCronTasks();
    items.value = res.data || [];
  } catch (error) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const openEdit = (row?: CronConfig) => {
  if (row) {
    Object.assign(form, row);
  } else {
    Object.assign(form, { id: undefined, name: '', cronExpression: '', description: '', status: 1 });
  }
  drawer.value = true;
};

const save = async () => {
  if (!form.name || !form.cronExpression) {
    ElMessage.warning('请填写必填项');
    return;
  }
  try {
    if (form.id) {
      await updateCronTask(form.id, form);
    } else {
      await createCronTask(form);
    }
    ElMessage.success('保存成功');
    drawer.value = false;
    load();
  } catch (error) {
    ElMessage.error('保存失败');
  }
};

const changeStatus = async (row: CronConfig, status: number) => {
  if (!row.id) return;
  try {
    await updateCronStatus(String(row.id), status);
    ElMessage.success('状态已更新');
    load();
  } catch (error) {
    ElMessage.error('更新失败');
  }
};

const execute = async (row: CronConfig) => {
  if (!row.id) return;
  try {
    await executeCronTask(String(row.id));
    ElMessage.success('已触发执行');
  } catch (error) {
    ElMessage.error('执行失败');
  }
};

const remove = async (row: CronConfig) => {
  if (!row.id) return;
  try {
    await deleteCronTask(String(row.id));
    ElMessage.success('已删除');
    load();
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

onMounted(load);
</script>

