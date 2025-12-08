<template>
  <div class="page-wrapper">
    <el-card shadow="never">
      <template #header>
        <div class="card-toolbar">
          <span>个人记忆管理</span>
          <div class="flex-row">
            <div v-if="currentUser" style="margin-right: 10px; padding: 5px 10px; background: #f5f7fa; border-radius: 4px;">
              <span style="color: #606266;">当前用户: {{ currentUser.displayName || currentUser.username }}</span>
            </div>
            <el-button type="primary" @click="showAddDialog = true" :disabled="!selectedUserId">
              <el-icon><Plus /></el-icon>
              新增记忆
            </el-button>
            <el-button @click="loadMemories" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索和筛选 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="8">
          <el-input
            v-model="searchKey"
            placeholder="搜索记忆键或标题"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select v-model="sortBy" placeholder="排序方式" @change="handleSort">
            <el-option label="创建时间 ↓" value="createdAt_desc" />
            <el-option label="创建时间 ↑" value="createdAt_asc" />
            <el-option label="重要性 ↓" value="importance_desc" />
            <el-option label="最近使用 ↓" value="lastUsedAt_desc" />
          </el-select>
        </el-col>
      </el-row>

      <!-- 记忆列表 -->
      <el-table 
        :data="filteredMemories" 
        v-loading="loading" 
        border 
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="memoryKey" label="记忆键" min-width="150" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentJson" label="内容预览" min-width="300" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ getContentPreview(scope.row.contentJson) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="100">
          <template #default="scope">
            <span v-if="scope.row.confidence">
              {{ (scope.row.confidence * 100).toFixed(1) }}%
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="importance" label="重要性" width="100">
          <template #default="scope">
            <el-rate
              v-if="scope.row.importance"
              v-model="scope.row.importance"
              :max="10"
              disabled
              show-score
              text-color="#ff9900"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" width="150">
          <template #default="scope">
            <el-tag
              v-for="tag in parseTags(scope.row.tags)"
              :key="tag"
              size="small"
              style="margin-right: 5px; margin-bottom: 2px;"
            >
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastUsedAt" label="最近使用" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.lastUsedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewContent(scope.row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button size="small" type="primary" @click="editMemory(scope.row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-popconfirm 
              title="确认删除此记忆?" 
              @confirm="deleteMemory(scope.row.memoryKey)"
            >
              <template #reference>
                <el-button size="small" type="danger" plain>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 批量操作 -->
      <div style="margin-top: 20px;" v-if="selectedMemories.length > 0">
        <el-button type="danger" @click="batchDelete">
          <el-icon><Delete /></el-icon>
          批量删除 ({{ selectedMemories.length }})
        </el-button>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="totalMemories"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; text-align: right;"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="editingMemory ? '编辑记忆' : '新增记忆'"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="memoryForm" :rules="formRules" ref="memoryFormRef" label-width="100px">
        <el-form-item label="记忆键" prop="memoryKey">
          <el-input 
            v-model="memoryForm.memoryKey" 
            placeholder="如: nickname, job, custom_instruction"
            :disabled="!!editingMemory"
          />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="memoryForm.title" placeholder="记忆的简短标题" />
        </el-form-item>
        <el-form-item label="内容" prop="contentJson">
          <el-input
            v-model="memoryForm.contentJson"
            type="textarea"
            :rows="6"
            placeholder="记忆内容，可以是JSON格式"
          />
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-radio-group v-model="memoryForm.source">
            <el-radio label="AI">AI生成</el-radio>
            <el-radio label="MANUAL">手动输入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="置信度" prop="confidence">
          <el-slider
            v-model="confidencePercent"
            :min="0"
            :max="100"
            :step="1"
            show-input
            show-stops
          />
        </el-form-item>
        <el-form-item label="重要性" prop="importance">
          <el-rate v-model="memoryForm.importance" :max="10" show-text />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input
            v-model="tagInput"
            placeholder="输入标签，用逗号分隔"
            @keyup.enter="addTag"
          />
          <div style="margin-top: 5px;">
            <el-tag
              v-for="tag in parseTags(memoryForm.tags)"
              :key="tag"
              closable
              @close="removeTag(tag)"
              style="margin-right: 5px; margin-bottom: 2px;"
            >
              {{ tag }}
            </el-tag>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 内容查看对话框 -->
    <el-dialog
      v-model="showContentDialog"
      title="记忆内容"
      width="700px"
    >
      <div v-if="currentMemory">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="记忆键">{{ currentMemory.memoryKey }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentMemory.title }}</el-descriptions-item>
          <el-descriptions-item label="来源">
            <el-tag :type="currentMemory.source === 'AI' ? 'info' : 'success'">
              {{ currentMemory.source === 'AI' ? 'AI生成' : '手动输入' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="置信度">
            {{ currentMemory.confidence ? (currentMemory.confidence * 100).toFixed(1) + '%' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="重要性">
            <el-rate
              v-if="currentMemory.importance"
              v-model="currentMemory.importance"
              :max="10"
              disabled
              show-score
            />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="标签">
            <el-tag
              v-for="tag in parseTags(currentMemory.tags)"
              :key="tag"
              size="small"
              style="margin-right: 5px; margin-bottom: 2px;"
            >
              {{ tag }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近使用">{{ formatDate(currentMemory.lastUsedAt) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(currentMemory.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(currentMemory.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 20px;">
          <h4>内容详情：</h4>
          <el-input
            :value="formatContentJson(currentMemory.contentJson)"
            type="textarea"
            :rows="8"
            readonly
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { getUser } from '@/utils/auth';
import {
  getUserPersonalMemories,
  saveUserPersonalMemory,
  deleteUserPersonalMemory,
  markUserPersonalMemoryUsed,
} from '@/api/userPersonalMemory';
import type { UserPersonalMemoryItem } from '@/api/types';
import {
  Plus,
  Refresh,
  Search,
  View,
  Edit,
  Delete,
} from '@element-plus/icons-vue';

// 用户相关
const currentUser = ref(getUser());
const selectedUserId = computed(() => currentUser.value?.id || null);

// 数据相关
const memories = ref<UserPersonalMemoryItem[]>([]);
const loading = ref(false);
const selectedMemories = ref<UserPersonalMemoryItem[]>([]);

// 搜索和筛选
const searchKey = ref('');
const sortBy = ref('createdAt_desc');

// 分页
const currentPage = ref(1);
const pageSize = ref(20);
const totalMemories = computed(() => memories.value.length);

// 对话框
const showAddDialog = ref(false);
const showContentDialog = ref(false);
const editingMemory = ref<UserPersonalMemoryItem | null>(null);
const currentMemory = ref<UserPersonalMemoryItem | null>(null);
const submitLoading = ref(false);

// 表单
const memoryFormRef = ref<FormInstance>();
const memoryForm = reactive<UserPersonalMemoryItem>({
  userId: 0,
  memoryKey: '',
  title: '',
  contentJson: '',
  source: 'MANUAL',
  confidence: 0.8,
  importance: 5,
  tags: '',
});

const formRules: FormRules = {
  memoryKey: [
    { required: true, message: '请输入记忆键', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' },
  ],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 255, message: '标题长度不能超过 255 个字符', trigger: 'blur' },
  ],
  contentJson: [
    { required: true, message: '请输入内容', trigger: 'blur' },
  ],
};

const tagInput = ref('');

// 计算属性
const confidencePercent = computed({
  get: () => Math.round((memoryForm.confidence || 0) * 100),
  set: (value: number) => {
    memoryForm.confidence = value / 100;
  },
});

const filteredMemories = computed(() => {
  let result = memories.value;

  // 搜索筛选
  if (searchKey.value) {
    const key = searchKey.value.toLowerCase();
    result = result.filter(memory => 
      memory.memoryKey.toLowerCase().includes(key) ||
      (memory.title && memory.title.toLowerCase().includes(key))
    );
  }

  // 排序
  result.sort((a, b) => {
    switch (sortBy.value) {
      case 'createdAt_desc':
        return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime();
      case 'createdAt_asc':
        return new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime();
      case 'importance_desc':
        return (b.importance || 0) - (a.importance || 0);
      case 'lastUsedAt_desc':
        return new Date(b.lastUsedAt || 0).getTime() - new Date(a.lastUsedAt || 0).getTime();
      default:
        return 0;
    }
  });

  // 分页
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return result.slice(start, end);
});

// 方法
// 移除了handleUserChange函数，因为现在使用当前登录用户

const loadMemories = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请先登录');
    return;
  }

  loading.value = true;
  try {
    const response = await getUserPersonalMemories(selectedUserId.value);
    const payload = response.data;
    let list: any = null;
    if (Array.isArray(payload)) {
      list = payload;
    }
    else if (payload && Array.isArray(payload.data)) {
      list = payload.data;
    }
    else if (payload && payload.data && Array.isArray(payload.data.list)) {
      list = payload.data.list;
    }
    else if (payload && Array.isArray(payload.list)) {
      list = payload.list;
    }
    if (list && Array.isArray(list)) {
      memories.value = list as UserPersonalMemoryItem[];
    }
    else {
      memories.value = [];
      ElMessage.error('数据格式错误');
    }
  } catch (error) {
    ElMessage.error('加载个人记忆失败');
    console.error('加载个人记忆失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
};

const handleSort = () => {
  currentPage.value = 1;
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

const handleSelectionChange = (selection: UserPersonalMemoryItem[]) => {
  selectedMemories.value = selection;
};

const parseTags = (tags?: string): string[] => {
  if (!tags) return [];
  try {
    const parsed = JSON.parse(tags);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const formatDate = (date?: string | number[]): string => {
  if (!date) return '-';
  
  // 处理数组格式的日期 [2025, 12, 6, 22, 36, 9]
  if (Array.isArray(date)) {
    const [year, month, day, hour, minute, second] = date;
    return new Date(year, month - 1, day, hour, minute, second).toLocaleString('zh-CN');
  }
  
  // 处理字符串格式的日期
  return new Date(date).toLocaleString('zh-CN');
};

const formatContentJson = (contentJson?: string): string => {
  if (!contentJson) return '';
  try {
    const parsed = JSON.parse(contentJson);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return contentJson;
  }
};

const getContentPreview = (contentJson?: string): string => {
  if (!contentJson) return '-';
  try {
    const parsed = JSON.parse(contentJson);
    // 如果解析成功，提取content字段或返回简化JSON
    if (parsed.content) {
      return String(parsed.content);
    } else if (parsed.raw) {
      // 如果存在raw字段，尝试解析其中的content
      const rawData = JSON.parse(parsed.raw);
      return rawData.content || String(parsed.raw);
    } else {
      // 返回JSON字符串的前100个字符
      const jsonStr = JSON.stringify(parsed);
      return jsonStr.length > 100 ? jsonStr.substring(0, 100) + '...' : jsonStr;
    }
  } catch {
    // 如果不是有效的JSON，返回原字符串的前100个字符
    return contentJson.length > 100 ? contentJson.substring(0, 100) + '...' : contentJson;
  }
};

const viewContent = (memory: UserPersonalMemoryItem) => {
  currentMemory.value = memory;
  showContentDialog.value = true;
};

const editMemory = (memory: UserPersonalMemoryItem) => {
  editingMemory.value = memory;
  Object.assign(memoryForm, {
    ...memory,
    userId: selectedUserId.value!,
  });
  showAddDialog.value = true;
};

const deleteMemory = async (memoryKey: string) => {
  if (!selectedUserId.value) return;

  try {
    const response = await deleteUserPersonalMemory(selectedUserId.value, memoryKey);
    // 适配不同的响应格式
    if (response.data === null || response.data === undefined || 
        (response.data && typeof response.data === 'object' && 'success' in response.data && response.data.success) ||
        (response.data && typeof response.data === 'object' && response.data.success)) {
      ElMessage.success('删除成功');
      loadMemories();
    } else {
      ElMessage.error('删除失败');
    }
  } catch (error) {
    ElMessage.error('删除失败');
    console.error('删除个人记忆失败:', error);
  }
};

const batchDelete = async () => {
  if (!selectedUserId.value || selectedMemories.value.length === 0) return;

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedMemories.value.length} 条记忆吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    // 逐个删除（后端没有批量删除接口）
    for (const memory of selectedMemories.value) {
      await deleteUserPersonalMemory(selectedUserId.value, memory.memoryKey);
    }

    ElMessage.success('批量删除成功');
    selectedMemories.value = [];
    loadMemories();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败');
      console.error('批量删除失败:', error);
    }
  }
};

const submitForm = async () => {
  if (!memoryFormRef.value || !selectedUserId.value) return;

  await memoryFormRef.value.validate(async (valid) => {
    if (!valid) return;

    submitLoading.value = true;
    try {
      memoryForm.userId = selectedUserId.value;
      memoryForm.source = 'MANUAL';
      syncTagInputToTags();
      const response = await saveUserPersonalMemory(selectedUserId.value, memoryForm);
      
      // 适配不同的响应格式
      if (response.data === null || response.data === undefined || 
          (response.data && typeof response.data === 'object' && 'success' in response.data && response.data.success) ||
          (response.data && typeof response.data === 'object' && response.data.success)) {
        ElMessage.success(editingMemory.value ? '更新成功' : '创建成功');
        showAddDialog.value = false;
        resetForm();
        loadMemories();
      } else {
        ElMessage.error('操作失败');
      }
    } catch (error) {
      ElMessage.error('操作失败');
      console.error('保存个人记忆失败:', error);
    } finally {
      submitLoading.value = false;
    }
  });
};

const resetForm = () => {
  editingMemory.value = null;
  Object.assign(memoryForm, {
    userId: selectedUserId.value || 0,
    memoryKey: '',
    title: '',
    contentJson: '',
    source: 'MANUAL',
    confidence: 0.8,
    importance: 5,
    tags: '',
  });
  tagInput.value = '';
  memoryFormRef.value?.resetFields();
};

const addTag = () => {
  if (!tagInput.value.trim()) return;
  
  const tags = parseTags(memoryForm.tags);
  const newTag = tagInput.value.trim();
  
  if (!tags.includes(newTag)) {
    tags.push(newTag);
    memoryForm.tags = JSON.stringify(tags);
  }
  
  tagInput.value = '';
};

const syncTagInputToTags = () => {
  if (tagInput.value && tagInput.value.trim()) {
    addTag();
  }
};

const removeTag = (tag: string) => {
  const tags = parseTags(memoryForm.tags);
  const index = tags.indexOf(tag);
  if (index > -1) {
    tags.splice(index, 1);
    memoryForm.tags = JSON.stringify(tags);
  }
};

// 生命周期
onMounted(() => {
  // 使用当前登录用户
  if (currentUser.value) {
    loadMemories();
  } else {
    ElMessage.warning('请先登录');
  }
});
</script>

<style scoped>
.page-wrapper {
  padding: 20px;
}

.card-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.flex-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

:deep(.el-rate__text) {
  font-size: 12px;
  margin-left: 5px;
}
</style>

