<template>
  <div class="codegen-editor">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="goBack" circle />
        <h1 class="app-name">{{ appInfo?.appName || '代码生成器' }}</h1>
        <el-tag v-if="appInfo?.codeGenType" type="primary" size="small" class="type-tag">
          {{ appInfo.codeGenType }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-button :icon="Download" @click="downloadCode" :loading="downloading">
          下载代码
        </el-button>
        <el-button :icon="View" @click="handleDeploy" :loading="deploying" type="primary">
          部署预览
        </el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <div
            v-for="(msg, index) in messages"
            :key="msg.id || index"
            :class="['message-item', msg.messageType === 'user' ? 'user-message' : 'ai-message']"
          >
            <!-- 用户消息 -->
            <template v-if="msg.messageType === 'user'">
              <div class="message-content user-content">{{ msg.message }}</div>
              <div class="message-avatar">
                <el-avatar :size="32" :src="userAvatar" />
              </div>
            </template>
            <!-- AI 消息 -->
            <template v-else>
              <div class="message-avatar">
                <el-avatar :size="32" :src="aiAvatar" />
              </div>
              <div class="message-content ai-content">
                <MarkdownRenderer v-if="msg.message" :content="msg.message" />
                <div v-if="msg.loading" class="loading-indicator">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <div v-if="selectedElement" class="selected-element-alert">
          <div class="element-header">
            <span class="element-tag">
              选中元素：{{ selectedElement.tagName?.toLowerCase() }}
            </span>
            <el-icon @click="clearSelectedElement" class="close-icon"><Close /></el-icon>
          </div>
          <div class="element-details">
            <div v-if="selectedElement.id" class="element-item">
              <span class="element-label">ID:</span>
              <code class="element-code">#{{ selectedElement.id }}</code>
            </div>
            <div v-if="selectedElement.className" class="element-item">
              <span class="element-label">类名:</span>
              <code class="element-code">.{{ selectedElement.className.split(' ').join('.') }}</code>
            </div>
            <div v-if="selectedElement.textContent" class="element-item">
              <span class="element-label">内容:</span>
              <span class="element-text">{{ selectedElement.textContent.substring(0, 50) }}{{ selectedElement.textContent.length > 50 ? '...' : '' }}</span>
            </div>
            <div class="element-item">
              <span class="element-label">选择器:</span>
              <code class="element-code">{{ selectedElement.selector }}</code>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-container">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="4"
            :placeholder="inputPlaceholder"
            @keydown.enter.prevent="handleSend"
            :disabled="generating"
            maxlength="1000"
            show-word-limit
          />
          <div class="input-actions">
            <span class="hint">Enter 发送</span>
            <el-button type="primary" @click="handleSend" :loading="generating" circle>
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 右侧预览区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>实时预览</h3>
          <div class="preview-actions">
            <el-button
              v-if="previewUrl"
              type="primary"
              :icon="isEditMode ? Close : Edit"
              @click="toggleEditMode"
              link
            >
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </el-button>
            <el-button v-if="previewUrl" @click="refreshPreview" link>
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-button v-if="previewUrl" @click="openInNewTab" link>
              <el-icon><TopRight /></el-icon>
              新窗口打开
            </el-button>
          </div>
        </div>
        <div class="preview-content">
          <!-- 占位状态 -->
          <div v-if="!previewUrl && !generating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>代码生成完成后将在这里展示</p>
          </div>
          <!-- 加载状态 -->
          <div v-else-if="generating" class="preview-loading">
            <el-icon class="is-loading loading-icon"><Loading /></el-icon>
            <p>正在生成代码...</p>
          </div>
          <!-- iframe -->
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            ref="previewFrame"
            @load="onIframeLoad"
            frameborder="0"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 部署成功弹窗 -->
    <el-dialog v-model="deployModalVisible" title="部署成功" width="400px">
      <p>应用已成功部署，可通过以下地址访问：</p>
      <el-input :value="deployUrl" readonly>
        <template #append>
          <el-button @click="copyDeployUrl">复制</el-button>
        </template>
      </el-input>
      <template #footer>
        <el-button @click="deployModalVisible = false">关闭</el-button>
        <el-button type="primary" @click="openDeployedSite">打开访问</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElIcon } from 'element-plus'
import {
  ArrowLeft,
  Download,
  View,
  Edit,
  Close,
  Refresh,
  TopRight,
  Loading,
  Promotion
} from '@element-plus/icons-vue'
import {
  getApp,
  getChatHistory,
  type AppVO,
  type ChatMessageVO,
  type ElementInfo,
  getPreviewUrl,
  getDownloadUrl
} from '@/api/codegen'
import { VisualEditor } from '@/utils/visualEditor'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { getToken } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

// 头像
const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')
const aiAvatar = ref('https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')

// 获取并验证 appId
const appId = computed(() => {
  const id = Number(route.params.id)
  if (isNaN(id) || id <= 0) {
    ElMessage.error('无效的应用 ID')
    router.push('/codegen')
    return 0
  }
  return id
})

const appInfo = ref<AppVO | null>(null)
const messages = ref<ChatMessageVO[]>([])
const inputMessage = ref('')
const generating = ref(false)
const deploying = ref(false)
const downloading = ref(false)

const previewUrl = ref('')
const previewFrame = ref<HTMLIFrameElement | null>(null)
const messagesContainer = ref<HTMLElement | null>(null)
const previewReady = ref(false)

const isEditMode = ref(false)
const selectedElement = ref<ElementInfo | null>(null)

const deployModalVisible = ref(false)
const deployUrl = ref('')

// 输入框占位符
const inputPlaceholder = computed(() => {
  if (selectedElement.value) {
    return `正在编辑 ${selectedElement.value.tagName} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
})

let visualEditor: VisualEditor | null = null

// 初始化可视化编辑器
function initVisualEditor() {
  if (!previewFrame.value) return

  visualEditor = new VisualEditor({
    onElementSelected: (elementInfo: ElementInfo) => {
      selectedElement.value = elementInfo
      ElMessage.success('已选中元素，现在可以输入修改要求')
    },
  })

  visualEditor.init(previewFrame.value)

  // 监听来自 iframe 的消息
  window.addEventListener('message', handleIframeMessage)
}

// 处理来自 iframe 的消息
function handleIframeMessage(event: MessageEvent) {
  if (visualEditor) {
    visualEditor.handleIframeMessage(event)
  }
}

// 切换编辑模式
function toggleEditMode() {
  if (!previewUrl.value || !previewReady.value) {
    ElMessage.warning('请等待页面加载完成')
    return
  }

  isEditMode.value = !isEditMode.value

  if (visualEditor) {
    if (isEditMode.value) {
      visualEditor.enableEditMode()
      ElMessage.info('编辑模式已开启，点击预览区域中的元素进行选中')
    } else {
      visualEditor.disableEditMode()
      clearSelectedElement()
    }
  }
}

// 清除选中元素
function clearSelectedElement() {
  selectedElement.value = null
  if (visualEditor) {
    visualEditor.clearSelection()
  }
}

// iframe 加载完成
function onIframeLoad() {
  previewReady.value = true
  if (visualEditor && previewFrame.value) {
    visualEditor.onIframeLoad()
  }
}

// 加载应用信息
async function loadAppInfo() {
  const id = appId.value
  if (id <= 0) return

  try {
    const res = await getApp(id)
    appInfo.value = res.data
    // 注意：不在这里设置预览 URL，而是在确认代码生成成功后才设置
  } catch (error: any) {
    ElMessage.error('加载应用信息失败：' + (error.response?.data?.message || error.message))
    router.push('/codegen')
  }
}

// 加载对话历史
async function loadHistory() {
  if (appId.value <= 0) return

  try {
    const res = await getChatHistory(appId.value)
    messages.value = res.data
  } catch (error: any) {
    console.error('加载历史记录失败', error)
  }
}

// 刷新预览
function refreshPreview() {
  if (previewUrl.value) {
    const url = new URL(previewUrl.value, window.location.origin)
    url.searchParams.set('t', Date.now().toString())
    previewUrl.value = url.toString()
  }
}

// 新窗口打开预览
function openInNewTab() {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 发送消息
async function handleSend() {
  const message = inputMessage.value.trim()
  if (!message) {
    ElMessage.warning('请输入消息')
    return
  }

  let finalMessage = message
  if (selectedElement.value) {
    finalMessage = buildMessageWithElement(message, selectedElement.value)
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  inputMessage.value = ''

  // 添加用户消息
  messages.value.push({
    id: Date.now(),
    message: finalMessage,
    messageType: 'user',
    appId: appId.value,
    userId: 0,
    createdAt: new Date().toISOString()
  })

  // 添加 AI 消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    id: Date.now() + 1,
    message: '',
    messageType: 'ai',
    appId: appId.value,
    userId: 0,
    createdAt: new Date().toISOString(),
    loading: true
  })

  scrollToBottom()

  // 开始流式生成
  await generateCodeStream(finalMessage, aiMessageIndex)
}

// 构建包含元素信息的消息
function buildMessageWithElement(message: string, element: ElementInfo): string {
  let result = message
  result += '\n\n选中元素信息：'
  if (element.tagName) {
    result += `\n- 标签: ${element.tagName}`
  }
  if (element.id) {
    result += `\n- ID: ${element.id}`
  }
  if (element.className) {
    result += `\n- 类名: ${element.className}`
  }
  if (element.textContent) {
    result += `\n- 文本内容: ${element.textContent.substring(0, 100)}`
  }
  if (element.selector) {
    result += `\n- CSS选择器: ${element.selector}`
  }
  return result
}

// 流式生成代码
async function generateCodeStream(userMessage: string, aiMessageIndex: number) {
  let eventSource: EventSource | null = null
  generating.value = true

  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

    // 构建 URL 参数（添加 token 用于认证）
    const token = getToken()
    const params = new URLSearchParams({
      appId: String(appId.value),
      message: userMessage
    })
    if (token) {
      params.append('token', token)
    }

    const url = `${baseURL}/api/codegen/generate/stream?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url)

    let fullContent = ''
    let streamCompleted = false

    // 处理接收到的消息
    eventSource.onmessage = (event) => {
      if (streamCompleted) return

      try {
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].message = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
      }
    }

    // 处理 done 事件
    eventSource.addEventListener('done', () => {
      if (streamCompleted) return
      streamCompleted = true
      finishGeneration(fullContent)
    })

    // 处理 error 事件
    eventSource.addEventListener('error', (event: MessageEvent) => {
      if (streamCompleted) return
      streamCompleted = true

      try {
        const errorData = JSON.parse(event.data)
        ElMessage.error(errorData.error || '生成失败')
      } catch {
        ElMessage.error('生成失败')
      }

      messages.value[aiMessageIndex].loading = false
      messages.value[aiMessageIndex].message = '生成失败，请重试'
      generating.value = false
      eventSource?.close()
    })

    // 处理连接错误
    eventSource.onerror = () => {
      if (streamCompleted) return

      if (eventSource?.readyState === EventSource.CLOSED) {
        streamCompleted = true
        finishGeneration(fullContent)
      } else {
        ElMessage.error('连接中断，正在重试...')
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    ElMessage.error('生成失败，请重试')
    messages.value[aiMessageIndex].loading = false
    generating.value = false
  }

  function finishGeneration(content: string) {
    generating.value = false
    eventSource?.close()

    setTimeout(async () => {
      await waitForDeployKeyAndRefresh()
      scrollToBottom()
    }, 1500)
  }

  // 等待后端生成 deployKey 并刷新预览
  async function waitForDeployKeyAndRefresh(maxRetries = 5) {
    for (let i = 0; i < maxRetries; i++) {
      try {
        const res = await getApp(appId.value)
        if (res.data.deployKey) {
          const newPreviewUrl = getPreviewUrl(res.data.deployKey)
          if (newPreviewUrl !== previewUrl.value) {
            previewUrl.value = newPreviewUrl + '?v=' + Date.now()
            ElMessage.success('代码生成完成，预览已更新')
          }
          appInfo.value = res.data
          return
        }
        await new Promise(resolve => setTimeout(resolve, 1000))
      } catch (error) {
        console.error('检查 deployKey 失败', error)
      }
    }
    ElMessage.warning('代码生成完成，但预览可能需要几秒钟更新')
  }
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 部署应用
async function handleDeploy() {
  try {
    deploying.value = true
    const res = await getApp(appId.value)
    if (res.data.deployKey) {
      deployUrl.value = window.location.origin + getPreviewUrl(res.data.deployKey)
      deployModalVisible.value = true
    } else {
      ElMessage.info('请先生成代码')
    }
  } catch (error: any) {
    ElMessage.error('获取应用信息失败')
  } finally {
    deploying.value = false
  }
}

// 复制部署链接
function copyDeployUrl() {
  navigator.clipboard.writeText(deployUrl.value)
  ElMessage.success('已复制到剪贴板')
}

// 打开部署的网站
function openDeployedSite() {
  window.open(deployUrl.value, '_blank')
}

// 下载代码
async function downloadCode() {
  const url = getDownloadUrl(appId.value)
  const link = document.createElement('a')
  link.href = url
  link.download = `app_${appId.value}.zip`
  link.click()
  ElMessage.success('开始下载')
}

// 返回列表
function goBack() {
  router.push('/codegen')
}

onMounted(async () => {
  await loadAppInfo()
  await loadHistory()

  // 检查历史记录中是否有 AI 响应，如果有则设置预览 URL
  checkAndSetPreviewAfterLoad()

  // 检查是否需要自动触发初始代码生成
  checkAndAutoGenerate()

  nextTick(() => {
    initVisualEditor()
  })
})

// 加载后检查并设置预览 URL
function checkAndSetPreviewAfterLoad() {
  const hasAiMessage = messages.value.some(msg => msg.messageType === 'ai')
  if (hasAiMessage && appInfo.value?.deployKey) {
    previewUrl.value = getPreviewUrl(appInfo.value.deployKey) + '?v=' + Date.now()
  }
}

// 检查并自动触发初始代码生成
function checkAndAutoGenerate() {
  const hasAiMessage = messages.value.some(msg => msg.messageType === 'ai')
  const hasUserMessage = messages.value.some(msg => msg.messageType === 'user')

  if (hasUserMessage && !hasAiMessage) {
    const lastUserMsg = [...messages.value].reverse().find(msg => msg.messageType === 'user')
    if (lastUserMsg) {
      ElMessage.info('正在生成初始代码，请稍候...')
      inputMessage.value = lastUserMsg.message
      setTimeout(() => {
        handleSend()
      }, 500)
    }
  }
}

onUnmounted(() => {
  window.removeEventListener('message', handleIframeMessage)
  if (visualEditor) {
    visualEditor.disableEditMode()
  }
})
</script>

<style scoped>
.codegen-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 16px;
  background: #f5f5f5;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.type-tag {
  font-size: 12px;
}

.header-right {
  display: flex;
  gap: 8px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px 0;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.messages-container {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.user-message {
  justify-content: flex-end;
}

.ai-message {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  word-wrap: break-word;
}

.user-content {
  background: #409eff;
  color: white;
}

.ai-content {
  background: #f5f5f5;
  color: #333;
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
}

/* 选中元素信息 */
.selected-element-alert {
  margin: 0 16px;
  padding: 12px 16px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.element-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.element-tag {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 14px;
  font-weight: 600;
  color: #007bff;
}

.close-icon {
  cursor: pointer;
  color: #999;
}

.close-icon:hover {
  color: #666;
}

.element-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.element-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.element-label {
  color: #666;
  min-width: 50px;
}

.element-code {
  font-family: 'Monaco', 'Menlo', monospace;
  background: #f6f8fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #d73a49;
  border: 1px solid #e1e4e8;
}

.element-text {
  color: #333;
  word-break: break-all;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  border-top: 1px solid #e8e8e8;
  background: white;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.hint {
  font-size: 12px;
  color: #999;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #fafafa;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.preview-placeholder p {
  margin: 0;
  font-size: 14px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.loading-icon {
  font-size: 32px;
  color: #409eff;
  margin-bottom: 16px;
}

.preview-loading p {
  margin: 0;
  font-size: 14px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }

  .message-content {
    max-width: 85%;
  }
}

/* Markdown 样式覆盖 */
.ai-content :deep(.markdown-content h1),
.ai-content :deep(.markdown-content h2),
.ai-content :deep(.markdown-content h3) {
  margin-top: 0.5em;
}

.ai-content :deep(.markdown-content p:first-child) {
  margin-top: 0;
}

.ai-content :deep(.markdown-content p:last-child) {
  margin-bottom: 0;
}
</style>
