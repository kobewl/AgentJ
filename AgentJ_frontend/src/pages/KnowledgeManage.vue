<template>
  <div class="page">
    <LoadingSpinner v-if="initialLoading" fullscreen text="正在加载知识库..." />
    
    <div v-else>
      <div class="page-header">
        <div>
          <h2>知识库管理</h2>
          <p class="subtitle">创建知识库、上传文档，存储到 uploads/knowledge/知识库ID/文档ID</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="showCreateDialog = true" aria-label="新建知识库">新建知识库</el-button>
      </div>

      <el-row :gutter="16" class="base-grid" v-loading="loadingBases" element-loading-text="加载知识库列表...">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in bases" :key="item.id">
          <el-card class="base-card" shadow="hover" @click="selectBase(item.id)" :aria-label="`知识库: ${item.name}`">
            <div class="base-card__header">
              <el-tag type="success" size="small">ID: {{ item.id }}</el-tag>
              <el-icon><Collection /></el-icon>
            </div>
            <h3 class="base-name">{{ item.name }}</h3>
            <p class="muted">创建时间: {{ formatTime(item.createdAt) }}</p>
            <div class="base-card__actions">
              <el-button size="small" type="primary" plain @click.stop="selectBase(item.id)">
                管理文件
              </el-button>
              <el-popconfirm title="删除后不可恢复，确认删除？" @confirm="handleDelete(item.id)">
                <template #reference>
                  <el-button size="small" type="danger" plain>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="base-card base-card--dashed" @click="showCreateDialog = true" aria-label="新建知识库">
            <div class="create-placeholder">
              <el-icon size="28"><Plus /></el-icon>
              <span>新建知识库</span>
            </div>
          </el-card>
        </el-col>
        <el-col v-if="bases.length === 0 && !loadingBases" :span="24">
          <div class="empty-state">
            <el-icon size="48" class="empty-icon"><Collection /></el-icon>
            <p>暂无知识库</p>
            <el-button type="primary" @click="showCreateDialog = true">创建第一个知识库</el-button>
          </div>
        </el-col>
      </el-row>

      <el-card v-if="selectedBaseId" class="files-card" shadow="never">
        <template #header>
          <div class="files-header">
            <div>
              <h3>知识库文件</h3>
              <p class="muted">当前知识库 ID：{{ selectedBaseId }}</p>
            </div>
            <el-upload
              :http-request="handleUpload"
              :show-file-list="false"
              accept=".txt,.md,.pdf,.doc,.docx"
              :disabled="uploading"
            >
              <el-button type="primary" :loading="uploading" :icon="Upload">上传文档</el-button>
            </el-upload>
          </div>
        </template>

        <el-table :data="files" v-loading="loadingFiles" size="small" border empty-text="暂无文件">
          <el-table-column prop="name" label="名称" min-width="200">
            <template #default="{ row }">
              <div class="file-name">
                <el-icon><Document /></el-icon>
                <div>
                  <div>{{ row.name }}</div>
                  <div class="muted">源文件: {{ row.originalFilename || '未知' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="storagePath" label="存储路径" min-width="240">
            <template #default="{ row }">
              <code>{{ row.storagePath || '-' }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120">
            <template #default="{ row }">
              {{ formatSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-popconfirm title="确认删除该文件？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button size="small" type="danger" text>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-dialog title="新建知识库" v-model="showCreateDialog" width="360px" :close-on-click-modal="false">
        <el-form @submit.prevent :model="createForm" :rules="createRules" ref="createFormRef">
          <el-form-item label="名称" prop="name">
            <el-input 
              v-model="createForm.name" 
              placeholder="输入知识库名称" 
              maxlength="50"
              show-word-limit
              @keyup.enter="createBase"
              aria-label="知识库名称"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="createBase">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Collection, Document, Plus, Upload } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import {
  KnowledgeItem,
  listKnowledgeBases,
  createKnowledgeBase,
  listKnowledgeFiles,
  uploadKnowledgeFile,
  deleteKnowledgeItem,
} from '@/api/knowledge';
import { ElMessage, UploadRequestOptions } from 'element-plus';
import { formatTime, formatSize } from '@/utils/format';

const bases = ref<KnowledgeItem[]>([]);
const files = ref<KnowledgeItem[]>([]);
const selectedBaseId = ref<string>('');
const initialLoading = ref(true);
const loadingBases = ref(false);
const loadingFiles = ref(false);
const uploading = ref(false);
const showCreateDialog = ref(false);
const creating = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = ref({ name: '' });
const createRules: FormRules = {
  name: [
    { required: true, message: '请输入知识库名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
};

const fetchBases = async () => {
  loadingBases.value = true;
  try {
    const resp = await listKnowledgeBases();
    bases.value = resp.data.data || [];
    if (!selectedBaseId.value && bases.value.length > 0) {
      selectBase(bases.value[0].id);
    }
  } catch (error) {
    ElMessage.error('加载知识库列表失败');
  } finally {
    loadingBases.value = false;
  }
};

const selectBase = async (id: string) => {
  selectedBaseId.value = id;
  await fetchFiles();
};

const fetchFiles = async () => {
  if (!selectedBaseId.value) return;
  loadingFiles.value = true;
  try {
    const resp = await listKnowledgeFiles(selectedBaseId.value);
    files.value = resp.data.data || [];
  } catch (error) {
    ElMessage.error('加载文件列表失败');
  } finally {
    loadingFiles.value = false;
  }
};

const createBase = async () => {
  if (!createFormRef.value) return;
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return;
    
    creating.value = true;
    try {
      await createKnowledgeBase(createForm.value.name.trim());
      ElMessage.success('创建成功');
      showCreateDialog.value = false;
      createForm.value.name = '';
      createFormRef.value?.resetFields();
      await fetchBases();
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '创建失败');
    } finally {
      creating.value = false;
    }
  });
};

const handleUpload = async (options: UploadRequestOptions) => {
  if (!selectedBaseId.value) {
    ElMessage.warning('请先选择知识库');
    return;
  }
  const file = options.file as File;
  uploading.value = true;
  try {
    await uploadKnowledgeFile(selectedBaseId.value, file);
    ElMessage.success('上传成功');
    await fetchFiles();
    options.onSuccess?.({});
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '上传失败');
    options.onError?.(e as any);
  } finally {
    uploading.value = false;
  }
};

const handleDelete = async (id: string) => {
  try {
    await deleteKnowledgeItem(id);
    ElMessage.success('删除成功');
    if (id === selectedBaseId.value) {
      selectedBaseId.value = '';
      files.value = [];
    }
    await fetchBases();
    if (selectedBaseId.value) {
      await fetchFiles();
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败');
  }
};

onMounted(async () => {
  try {
    await fetchBases();
  } finally {
    initialLoading.value = false;
  }
});
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.subtitle {
  color: var(--text-secondary);
  margin: 4px 0 0;
}
.base-grid {
  margin-top: 4px;
}
.base-card {
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
}
.base-card:hover {
  border-color: var(--el-color-primary);
  transform: translateY(-2px);
}
.base-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.base-name {
  margin: 0 0 4px;
}
.muted {
  color: var(--text-secondary);
  font-size: 12px;
}
.base-card__actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.base-card--dashed {
  border: 1px dashed var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
}
.create-placeholder {
  text-align: center;
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
}
.files-card {
  margin-top: 8px;
}
.files-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.file-name {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: var(--text-secondary);
}

.empty-icon {
  color: var(--text-tertiary);
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0 0 16px;
  font-size: 16px;
}

code {
  background: var(--bg-tertiary, #f6f8fa);
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
