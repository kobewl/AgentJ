<template>
  <div class="page-wrapper cron-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <div class="card-title">
            <div class="title-row">
              <span class="title">定时任务</span>
              <el-tag size="small" effect="plain" type="info">自动调度</el-tag>
            </div>
            <p class="subtitle">按 Cron 表达式自动触发 Agent 计划或模板，适合日报推送、巡检、定时数据同步等场景。</p>
          </div>
          <div class="flex-row">
            <el-button :loading="loading" @click="load">刷新</el-button>
            <el-button type="primary" @click="openEdit()">新建任务</el-button>
          </div>
        </div>
        <div class="filters">
          <el-input
            v-model="keyword"
            placeholder="搜索任务名 / 描述 / 计划"
            clearable
            :prefix-icon="Search"
            style="width: 260px"
          />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="全部状态" :value="null" />
            <el-option label="已启用" :value="1" />
            <el-option label="已停用" :value="0" />
          </el-select>
        </div>
      </template>

      <el-table
        :data="filteredItems"
        v-loading="loading"
        border
        style="width: 100%"
        :empty-text="emptyText"
        highlight-current-row
      >
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="cronName" label="任务名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="cronTime" label="Cron 表达式" min-width="180" show-overflow-tooltip />
        <el-table-column prop="planDesc" label="执行计划" min-width="220" show-overflow-tooltip />
        <el-table-column prop="planTemplateId" label="计划模板 ID" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.planTemplateId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="light">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastExecutedTime" label="上次执行" width="180" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.lastExecutedTime || '尚未执行' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <div class="table-actions">
              <el-button size="small" type="primary" link @click="openEdit(scope.row)">编辑</el-button>
              <el-button size="small" type="info" link @click="execute(scope.row)">执行一次</el-button>
              <el-button
                v-if="scope.row.status === 1"
                size="small"
                type="warning"
                link
                @click="changeStatus(scope.row, 0)"
              >
                停用
              </el-button>
              <el-button
                v-else
                size="small"
                type="success"
                link
                @click="changeStatus(scope.row, 1)"
              >
                启用
              </el-button>
              <el-popconfirm title="确认删除该任务？" @confirm="remove(scope.row)">
                <template #reference>
                  <el-button size="small" type="danger" link>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawer" title="任务配置" size="40%" @close="resetForm">
      <el-form :model="form" label-width="110px">
        <el-form-item label="任务名" required>
          <el-input v-model="form.cronName" placeholder="例如：日报推送" />
        </el-form-item>
        <el-form-item label="Cron 表达式" required>
          <el-input v-model="form.cronTime" placeholder="0 0/30 * * * ?" />
        </el-form-item>
        <el-form-item label="执行计划" required>
          <el-input
            v-model="form.planDesc"
            type="textarea"
            :rows="3"
            placeholder="描述要执行的计划或关联的 Agent 任务"
          />
        </el-form-item>
        <el-form-item label="计划模板ID">
          <el-input
            v-model="form.planTemplateId"
            placeholder="可选：填入计划模板 ID 用于自动运行对应模板"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <div class="drawer-footer">
          <el-button @click="drawer = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </div>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
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
import { Search } from '@element-plus/icons-vue';

const items = ref<CronConfig[]>([]);
const loading = ref(false);
const saving = ref(false);
const drawer = ref(false);
const keyword = ref('');
const statusFilter = ref<number | null>(null);
const defaultForm: CronConfig = {
  id: undefined,
  cronName: '',
  cronTime: '',
  planDesc: '',
  planTemplateId: '',
  status: 1,
};
const form = reactive<CronConfig>({ ...defaultForm });

const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const matchKeyword =
      !kw ||
      [item.cronName, item.planDesc, item.cronTime]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(kw));
    const matchStatus = statusFilter.value == null || item.status === statusFilter.value;
    return matchKeyword && matchStatus;
  });
});

const emptyText = computed(() =>
  keyword.value ? '没有匹配的任务' : '暂无定时任务，点击右上角「新建任务」快速创建'
);

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

const resetForm = () => {
  Object.assign(form, defaultForm);
};

const openEdit = (row?: CronConfig) => {
  resetForm();
  if (row) {
    Object.assign(form, row);
  }
  drawer.value = true;
};

const save = async () => {
  if (!form.cronName || !form.cronTime || !form.planDesc) {
    ElMessage.warning('请填写任务名、Cron 表达式和执行计划');
    return;
  }
  saving.value = true;
  try {
    if (form.id) {
      await updateCronTask(form.id, form);
    } else {
      await createCronTask(form);
    }
    ElMessage.success('保存成功');
    drawer.value = false;
    await load();
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const changeStatus = async (row: CronConfig, status: number) => {
  if (!row.id || row.status === status) return;
  try {
    await updateCronStatus(String(row.id), status);
    ElMessage.success('状态已更新');
    await load();
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
    await load();
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

onMounted(load);
</script>

<style scoped>
.cron-page .card-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.cron-page .card-title {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cron-page .title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cron-page .title {
  font-size: 16px;
  font-weight: 600;
}

.cron-page .subtitle {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.cron-page .filters {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.drawer-footer {
  margin-top: 12px;
  text-align: right;
}
</style>
