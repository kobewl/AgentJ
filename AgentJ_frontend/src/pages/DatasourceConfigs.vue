<template>
  <div class="page-wrapper">
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <span>数据源配置</span>
          <div class="flex-row">
            <el-button type="primary" @click="openEdit()">新建</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="items" v-loading="loading" border style="width: 100%">
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="url" label="URL" min-width="220" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="enabled" label="启用" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="remove(scope.row.id)">
              <template #reference>
                <el-button size="small" type="danger" plain>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer" title="数据源配置" size="40%">
      <el-form :model="form" label-width="120px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="URL" required>
          <el-input v-model="form.url" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="Driver">
          <el-input v-model="form.driverClassName" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
          <el-button @click="test">测试连接</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  createDatasourceConfig,
  deleteDatasourceConfig,
  listDatasourceConfigs,
  testDatasourceConnection,
  updateDatasourceConfig,
} from '@/api/datasource';
import type { DatasourceConfig } from '@/api/types';

const items = ref<DatasourceConfig[]>([]);
const loading = ref(false);
const drawer = ref(false);
const form = reactive<DatasourceConfig>({ name: '', url: '', username: '', password: '', enabled: true });

const load = async () => {
  loading.value = true;
  try {
    const res = await listDatasourceConfigs();
    items.value = res.data || [];
  } catch (error) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const openEdit = (row?: DatasourceConfig) => {
  if (row) {
    Object.assign(form, row);
  } else {
    Object.assign(form, { id: undefined, name: '', url: '', username: '', password: '', driverClassName: '', enabled: true });
  }
  drawer.value = true;
};

const save = async () => {
  if (!form.name || !form.url) {
    ElMessage.warning('请填写名称和 URL');
    return;
  }
  try {
    if (form.id) {
      await updateDatasourceConfig(form.id, form);
    } else {
      await createDatasourceConfig(form);
    }
    ElMessage.success('保存成功');
    drawer.value = false;
    load();
  } catch (error) {
    ElMessage.error('保存失败');
  }
};

const test = async () => {
  try {
    const res = await testDatasourceConnection(form);
    ElMessage.success(res.data.message || '测试完成');
  } catch (error) {
    ElMessage.error('测试失败');
  }
};

const remove = async (id?: number) => {
  if (!id) return;
  try {
    await deleteDatasourceConfig(id);
    ElMessage.success('已删除');
    load();
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

onMounted(load);
</script>

