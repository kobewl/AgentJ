<template>
  <div class="codegen-list">
    <div class="page-header">
      <h2>AI 代码生成</h2>
      <el-button type="primary" :icon="MagicStick" @click="showCreateDialog = true">
        新建应用
      </el-button>
    </div>

    <el-table :data="apps" v-loading="loading" stripe>
      <el-table-column prop="appName" label="应用名称" width="200" />
      <el-table-column prop="initPrompt" label="初始需求" show-overflow-tooltip />
      <el-table-column prop="codeGenType" label="类型" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="deployedTime" label="部署状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.deployedTime" type="success" size="small">已部署</el-tag>
          <el-tag v-else type="info" size="small">未部署</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditor(row.id)">编辑</el-button>
          <el-button link type="primary" @click="handleDownload(row)" :disabled="!row.deployKey">下载</el-button>
          <el-button link type="primary" @click="openPreview(row)" v-if="row.deployKey">预览</el-button>
          <el-popconfirm title="确定删除此应用吗？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建应用对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建应用" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="应用名称" prop="appName">
          <el-input v-model="createForm.appName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="初始需求" prop="initPrompt">
          <el-input
            v-model="createForm.initPrompt"
            type="textarea"
            :rows="4"
            placeholder="描述你想要生成的网页，例如：创建一个个人博客网站，包含首页、关于页面和文章列表"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { getUserApps, createApp, deleteApp, getPreviewUrl, getDownloadUrl } from '@/api/codegen'

const router = useRouter()

const apps = ref<any[]>([])
const loading = ref(false)
const creating = ref(false)
const showCreateDialog = ref(false)
const createFormRef = ref<FormInstance>()

const createForm = ref({
  appName: '',
  initPrompt: '',
  codeGenType: 'HTML'
})

const createRules: FormRules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  initPrompt: [
    { required: true, message: '请输入初始需求', trigger: 'blur' },
    { max: 1000, message: '长度不能超过 1000 个字符', trigger: 'blur' }
  ]
}

// 格式化时间
function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString('zh-CN')
}

// 加载应用列表
async function loadApps() {
  loading.value = true
  try {
    const res = await getUserApps()
    apps.value = res.data
  } catch (error) {
    console.error('加载应用列表失败', error)
  } finally {
    loading.value = false
  }
}

// 创建应用
async function handleCreate() {
  if (!createFormRef.value) return

  await createFormRef.value.validate(async (valid) => {
    if (!valid) return

    creating.value = true
    try {
      const res = await createApp(createForm.value)
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      createForm.value = { appName: '', initPrompt: '', codeGenType: 'HTML' }
      createFormRef.value.resetFields()
      // 跳转到编辑器
      router.push(`/codegen/editor/${res.data.id}`)
    } catch (error: any) {
      ElMessage.error('创建失败：' + (error.response?.data?.message || error.message))
    } finally {
      creating.value = false
    }
  })
}

// 打开编辑器
function openEditor(id: number) {
  router.push(`/codegen/editor/${id}`)
}

// 下载代码
function handleDownload(row: any) {
  const url = getDownloadUrl(row.id)
  const link = document.createElement('a')
  link.href = url
  link.download = `${row.appName || 'app'}_${row.id}.zip`
  link.click()
  ElMessage.success('开始下载')
}

// 打开预览
function openPreview(row: any) {
  if (row.deployKey) {
    const url = getPreviewUrl(row.deployKey)
    window.open(url, '_blank')
  }
}

// 删除应用
async function handleDelete(id: number) {
  try {
    await deleteApp(id)
    ElMessage.success('删除成功')
    await loadApps()
  } catch (error: any) {
    ElMessage.error('删除失败：' + (error.response?.data?.message || error.message))
  }
}

onMounted(() => {
  loadApps()
})
</script>

<style scoped>
.codegen-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
</style>
