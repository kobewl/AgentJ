<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-toolbar">
        <span>记忆列表</span>
        <div class="flex-row">
          <el-input v-model="newMemory.conversationId" placeholder="conversationId" style="width: 200px" />
          <el-input v-model="newMemory.name" placeholder="名称" style="width: 200px" />
          <el-button type="primary" @click="create">新建</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <el-table :data="items" v-loading="loading" border style="width: 100%">
      <el-table-column prop="conversationId" label="ConversationId" min-width="220" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column label="RootPlanIds" min-width="240">
        <template #default="scope">{{ (scope.row.rootPlanIds || []).join(', ') || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="viewHistory(scope.row.conversationId)">历史</el-button>
          <el-popconfirm title="确认删除?" @confirm="remove(scope.row.conversationId)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-drawer v-model="historyVisible" title="会话历史" size="50%">
    <el-empty v-if="history.length === 0" description="暂无记录" />
    <el-collapse v-else>
      <el-collapse-item v-for="(h, idx) in history" :key="idx" :title="h.rootPlanId || '记录'">
        <pre>{{ JSON.stringify(h, null, 2) }}</pre>
      </el-collapse-item>
    </el-collapse>
  </el-drawer>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { createMemory, deleteMemory, getConversationHistory, getMemories } from '@/api/memory';
import type { MemoryItem } from '@/api/types';

const items = ref<MemoryItem[]>([]);
const loading = ref(false);
const historyVisible = ref(false);
const history = ref<any[]>([]);
const newMemory = reactive<MemoryItem>({ conversationId: '', name: '' });

const load = async () => {
  loading.value = true;
  try {
    const res = await getMemories();
    items.value = res.data.data || [];
  } catch (error) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const create = async () => {
  if (!newMemory.conversationId) {
    ElMessage.warning('请输入 conversationId');
    return;
  }
  try {
    await createMemory(newMemory);
    ElMessage.success('已创建');
    newMemory.conversationId = '';
    newMemory.name = '';
    load();
  } catch (error) {
    ElMessage.error('创建失败');
  }
};

const remove = async (conversationId: string) => {
  try {
    await deleteMemory(conversationId);
    ElMessage.success('已删除');
    load();
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

const viewHistory = async (conversationId: string) => {
  try {
    const res = await getConversationHistory(conversationId);
    history.value = res.data || [];
    historyVisible.value = true;
  } catch (error) {
    ElMessage.error('获取历史失败');
  }
};

onMounted(load);
</script>
