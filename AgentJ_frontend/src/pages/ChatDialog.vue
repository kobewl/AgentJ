<template>
  <div class="chat-page">
    <div class="chat-shell">
      <header class="chat-hero">
        <div class="hero-actions">
          <el-button text size="small" @click="clearChat" :disabled="messages.length === 0">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
          <el-button type="primary" plain size="small" @click="exportChat" :disabled="messages.length === 0">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </div>
      </header>



      <section class="chat-panel">
        <div class="messages" ref="messagesContainer">
          <div v-if="messages.length === 0 && !streamingText" class="empty-state">
            <div class="empty-badge">
              <el-icon size="20"><ChatDotRound /></el-icon>
              即刻开聊
            </div>
            <h3>你好，我是你的 AI 工作台</h3>
            <p>提问、改写、生成或审查代码，让对话自然流畅。</p>
          </div>

          <transition-group name="message" tag="div">
            <div
              v-for="(message, index) in messages"
              :key="index"
              :class="['message-row', message.role]"
            >
              <div class="avatar" :data-role="message.role">
                <el-icon v-if="message.role === 'user'" size="20"><User /></el-icon>
                <el-icon v-else size="20"><Cpu /></el-icon>
              </div>
              <div class="bubble">
                <div class="bubble-head">
                  <span class="who">{{ message.role === 'user' ? '我' : 'AI 助手' }}</span>
                  <span class="time">{{ formatTime(message.timestamp) }}</span>
                </div>
                <div class="bubble-body">
                  <div 
                    class="text"
                    v-if="message.role === 'user'"
                  >{{ message.content }}</div>
                  <div 
                    class="text markdown-body"
                    v-else
                    v-html="renderMarkdown(message.content)"
                  ></div>
                  <div v-if="message.role === 'assistant'" class="bubble-actions">
                    <el-button text size="small" @click="copyMessage(message.content)">
                      <el-icon size="12"><CopyDocument /></el-icon> 复制
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </transition-group>

          <div v-if="streamingText" class="message-row assistant streaming">
            <div class="avatar" data-role="assistant">
              <el-icon size="20"><Cpu /></el-icon>
            </div>
            <div class="bubble live">
              <div class="bubble-head">
                <span class="who">AI 助手</span>
                <span class="time">正在生成...</span>
              </div>
              <div class="bubble-body">
                <div
                  class="text markdown-body live-markdown"
                  v-html="streamingHtml"
                ></div>
                <span class="cursor">▍</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="input-panel">
        <div class="input-container">
          <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileSelect"
            accept=".txt,.pdf,.doc,.docx,.json,.csv"
          >
            <el-button 
              text 
              size="small" 
              class="attach-btn"
            >
              <el-icon><Paperclip /></el-icon>
            </el-button>
          </el-upload>
          
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            placeholder="输入你的问题、需求或粘贴代码片段..."
            :disabled="sending"
            @keydown.enter.prevent="handleEnterKey"
            class="main-input"
          >
          </el-input>
          
          <div class="send-area">
            <el-button
              v-if="sending"
              circle
              text
              class="control-btn stop-btn"
              @click="stop"
              size="large"
            >
              <el-icon size="20"><VideoPause /></el-icon>
            </el-button>
            <el-button
              v-else
              circle
              type="primary"
              class="control-btn send-btn"
              @click="send"
              :disabled="!input.trim()"
              size="large"
            >
              <el-icon size="20"><Send /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="input-meta">
          <div class="left-info">
            <span class="status-indicator" :class="connectionStatusClass">
              <span class="status-dot"></span>
              {{ connectionStatusText }}
            </span>
            <el-tag size="small" type="info" round class="stream-tag">实时流式</el-tag>
            <span class="shortcut-hint">Enter 发送 · Shift+Enter 换行</span>
          </div>
          <span v-if="selectedFile" class="file-pill">
            <el-icon size="12"><Document /></el-icon>
            {{ selectedFile.name }}
            <el-button text size="small" @click="clearFile" class="remove-file-btn">
              <el-icon size="10"><Close /></el-icon>
            </el-button>
          </span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { streamSse, type SseMessage } from '@/utils/sse';
import {
  ChatDotRound,
  ChatLineRound,
  Delete,
  Download,
  Paperclip,
  Position,
  VideoPause,
  Document,
  Close,
  User,
  Cpu,
  CopyDocument,
} from '@element-plus/icons-vue';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';

// 配置marked，支持代码高亮
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value;
      } catch (__) {}
    }
    return hljs.highlightAuto(code).value;
  },
  breaks: true,
  gfm: true
});

// 渲染markdown内容
const renderMarkdown = (content: string) => {
  return marked(content);
};

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}



const input = ref('');
const messages = ref<ChatMessage[]>([]);
const conversationId = ref('');
const streamingText = ref('');
const streamingHtml = computed(() => renderMarkdown(streamingText.value || ''));
const sending = ref(false);
const selectedFile = ref<File | null>(null);
const messagesContainer = ref<HTMLElement>();

let abortController: AbortController | null = null;



// 连接状态
const connectionStatus = ref<'connected' | 'connecting' | 'disconnected'>('connected');

const connectionStatusClass = computed(() => {
  return {
    'status-success': connectionStatus.value === 'connected',
    'status-warning': connectionStatus.value === 'connecting',
    'status-error': connectionStatus.value === 'disconnected'
  };
});

const connectionStatusIcon = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return 'CircleCheck';
    case 'connecting': return 'Loading';
    case 'disconnected': return 'CircleClose';
    default: return 'InfoFilled';
  }
});

const connectionStatusText = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return '已连接';
    case 'connecting': return '连接中...';
    case 'disconnected': return '未连接';
    default: return '未知';
  }
});

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick();
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

// 格式化时间
const formatTime = (date: Date) => {
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (minutes < 1440) return `${Math.floor(minutes / 60)}小时前`;
  
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 处理SSE消息
const handleSseMessage = (data: SseMessage) => {
  const type = data.type as string;
  
  if (type === 'start' && data.conversationId) {
    conversationId.value = data.conversationId as string;
  }
  
  if (type === 'chunk' && typeof data.content === 'string') {
    streamingText.value += data.content;
    scrollToBottom();
  }
  
  if (type === 'done') {
    if (streamingText.value) {
      messages.value.push({ 
        role: 'assistant', 
        content: streamingText.value,
        timestamp: new Date()
      });
      streamingText.value = '';
    }
    sending.value = false;
    abortController = null;
    connectionStatus.value = 'connected';
  }
  
  if (type === 'error') {
    sending.value = false;
    abortController = null;
    connectionStatus.value = 'disconnected';
    ElMessage.error((data.message as string) || '对话出错');
  }
};

// 发送消息
const send = async () => {
  if (!input.value.trim()) {
    ElMessage.warning('请输入内容');
    return;
  }
  
  // 添加用户消息
  messages.value.push({ 
    role: 'user', 
    content: input.value,
    timestamp: new Date()
  });
  
  const payload = {
    input: input.value,
    conversationId: conversationId.value || undefined,
    requestSource: 'VUE_DIALOG',
  };
  
  // 清空输入
  input.value = '';
  streamingText.value = '';
  sending.value = true;
  connectionStatus.value = 'connecting';
  abortController = new AbortController();
  
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const url = `${base}/api/executor/chat`;
  
  try {
    await streamSse(
      url, 
      payload, 
      handleSseMessage, 
      () => {
        sending.value = false;
        abortController = null;
        connectionStatus.value = 'connected';
      }, 
      abortController.signal
    );
    
    scrollToBottom();
  } catch (error) {
    sending.value = false;
    abortController = null;
    connectionStatus.value = 'disconnected';
    ElMessage.error('发送失败，请检查网络连接');
  }
};



// 停止响应
const stop = () => {
  abortController?.abort();
  sending.value = false;
  streamingText.value = '';
  connectionStatus.value = 'connected';
};

// 处理回车键
const handleEnterKey = (event: KeyboardEvent) => {
  if (event.shiftKey) {
    // Shift + Enter 换行
    return;
  }
  event.preventDefault();
  send();
};

// 复制消息
const copyMessage = (content: string) => {
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制到剪贴板');
  }).catch(() => {
    ElMessage.error('复制失败');
  });
};

// 清空对话
const clearChat = () => {
  messages.value = [];
  conversationId.value = '';
  ElMessage.success('对话已清空');
};

// 导出对话
const exportChat = () => {
  const content = messages.value.map(msg => 
    `${msg.role === 'user' ? '用户' : 'AI助手'} (${formatTime(msg.timestamp)}):\n${msg.content}\n\n`
  ).join('');
  
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `对话记录_${new Date().toLocaleDateString()}.txt`;
  link.click();
  URL.revokeObjectURL(url);
  ElMessage.success('对话已导出');
};

// 文件选择
const handleFileSelect = (file: any) => {
  selectedFile.value = file.raw;
  ElMessage.success(`已选择文件: ${file.name}`);
};

// 清除文件
const clearFile = () => {
  selectedFile.value = null;
};

// 监听消息变化
watch(messages, () => {
  scrollToBottom();
}, { deep: true });

watch(streamingText, () => {
  scrollToBottom();
});
</script>

<style scoped>
.chat-page {
  min-height: calc(100vh - 80px);
  background: #f8fafc;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  overflow: hidden;
}

.chat-shell {
  width: 100%;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  margin: 16px;
}

/* Smooth scrolling for messages */
.messages {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 #f1f5f9;
}

.messages::-webkit-scrollbar {
  width: 8px;
}

.messages::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

.messages::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.messages::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.chat-hero {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px;
  border-bottom: 1px solid #f1f5f9;
  background: white;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-panel {
  padding: 20px 24px;
  background: #f8fafc;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding-right: 12px;
  max-width: 100%;
}

.empty-state {
  text-align: center;
  padding: 120px 0;
  color: #718096;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.empty-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  font-size: 14px;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
  margin-bottom: 24px;
}

.empty-state h3 {
  font-size: 28px;
  color: #2d3748;
  margin: 0 0 16px;
}

.empty-state p {
  font-size: 16px;
  color: #718096;
  margin: 0;
  max-width: 500px;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  animation: messageSlideIn 0.3s ease-out;
  align-items: flex-start;
  max-width: 100%;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
  transition: transform 0.2s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.avatar[data-role="user"] {
  background: linear-gradient(135deg, #4299e1 0%, #3182ce 100%);
  color: white;
}

.bubble {
  max-width: 85%;
  background: white;
  border: 1px solid #e2e8f0;
  padding: 10px 14px;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  transition: all 0.2s ease;
}

.bubble:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.bubble.live {
  border-color: #667eea;
  box-shadow: 0 0 0 1px rgba(102, 126, 234, 0.2), 0 4px 12px rgba(102, 126, 234, 0.15);
  background: linear-gradient(135deg, #fafbff 0%, #f0f2ff 100%);
}

.message-row.user .bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.message-row.user .bubble:hover {
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.25);
  transform: translateY(-1px);
}

.bubble-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  margin-bottom: 6px;
}

.who {
  font-weight: 600;
  color: #475569;
}

.message-row.user .who {
  color: rgba(255, 255, 255, 0.9);
}

.time {
  font-size: 11px;
  color: #94a3b8;
}

.message-row.user .time {
  color: rgba(255, 255, 255, 0.7);
}

.bubble-body .text {
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  color: #1e293b;
}

.bubble-body .text.markdown-body {
  white-space: normal;
}

.streaming .bubble-body {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.live-markdown {
  flex: 1;
  padding: 0;
}

.message-row.user .bubble-body .text {
  color: white;
}

.bubble-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.bubble:hover .bubble-actions {
  opacity: 1;
}

.bubble-actions button {
  background: none;
  border: none;
  color: #94a3b8;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 11px;
  transition: all 0.2s ease;
}

.bubble-actions button:hover {
  background: #f1f5f9;
  color: #334155;
}

.message-row.user .bubble-actions button {
  color: rgba(255, 255, 255, 0.7);
}

.message-row.user .bubble-actions button:hover {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.streaming .cursor {
  margin-left: 3px;
  animation: blink 1s infinite;
  color: #667eea;
  font-weight: bold;
  font-size: 16px;
  vertical-align: middle;
}

@keyframes messageSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.input-panel {
  padding: 16px 24px 20px;
  border-top: 1px solid #f1f5f9;
  background: white;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 12px;
  transition: all 0.2s ease;
}

.input-container:focus-within {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.attach-btn {
  flex-shrink: 0;
  color: #64748b;
  transition: color 0.2s ease;
}

.attach-btn:hover {
  color: #667eea;
}

.main-input {
  flex: 1;
  background: transparent;
  border: none;
  resize: none;
  outline: none;
  font-size: 15px;
  line-height: 1.6;
  color: #1e293b;
  font-family: inherit;
}

.main-input::placeholder {
  color: #94a3b8;
}

.control-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.send-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #5a67d8 0%, #6b46c1 100%);
}

.send-btn:active:not(:disabled) {
  transform: translateY(0);
}

.send-btn:disabled {
  opacity: 0.5;
  transform: none;
  box-shadow: none;
}

.stop-btn {
  color: #ef4444;
  background: #f56565;
  border: none;
  border-radius: 12px;
  color: white;
  box-shadow: 0 4px 12px rgba(245, 101, 101, 0.3);
}

.stop-btn:hover {
  background: #e53e3e;
  color: #dc2626;
  box-shadow: 0 6px 16px rgba(245, 101, 101, 0.4);
  transform: translateY(-1px);
}

.stop-btn:active {
  transform: translateY(0);
}

.input-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  font-size: 12px;
  color: #a0aec0;
}

.left-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #94a3b8;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 2px rgba(74, 222, 128, 0.3);
  animation: pulse 2s infinite;
}

.status-indicator.status-warning .status-dot {
  background: #fbbf24;
  box-shadow: 0 0 0 2px rgba(251, 191, 36, 0.3);
}

.status-indicator.status-error .status-dot {
  background: #ef4444;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.3);
}

.stream-tag {
  font-size: 10px;
  padding: 2px 8px;
}

.shortcut-hint {
  color: #94a3b8;
}

.file-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f7fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 12px;
}

.file-pill .el-button {
  padding: 0;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-file-btn {
  padding: 2px;
  color: #0369a1;
  transition: color 0.2s ease;
}

.remove-file-btn:hover {
  color: #0284c7;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-page {
    padding: 10px;
    min-height: 100vh;
  }
  
  .chat-shell {
    border-radius: 16px;
    max-height: calc(100vh - 20px);
  }
  
  .chat-hero {
    padding: 20px 24px 16px;
    flex-direction: column;
    gap: 16px;
  }
  
  .chat-panel {
    padding: 16px 24px;
  }
  
  .bubble {
    max-width: 85%;
    padding: 14px 18px;
  }
  
  .input-panel {
    padding: 16px 24px 20px;
  }
}

@media (max-width: 480px) {
  .chat-page {
    padding: 0;
  }
  
  .chat-shell {
    border-radius: 0;
    max-height: 100vh;
  }
  
  .chat-panel {
    max-height: calc(100vh - 280px);
  }
  
  .bubble {
    max-width: 90%;
    padding: 12px 16px;
    border-radius: 16px;
  }
  
  .bubble-body .text {
    font-size: 14px;
  }
}

/* 增强的动画效果 */
.message-enter-active,
.message-leave-active {
  transition: all 0.3s ease;
}

.message-enter-from {
  opacity: 0;
  transform: translateY(15px) scale(0.95);
}

.message-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.message-move {
  transition: transform 0.3s ease;
}

/* 改进聊天面板高度 */
.chat-panel {
  padding: 16px 24px;
  background: #fafbfc;
  max-height: calc(100vh - 280px);
  overflow: hidden;
}

/* 优化输入工具按钮 */
.input-tools .el-button {
  color: #667eea;
  transition: all 0.2s ease;
}

.input-tools .el-button:hover {
  color: #5a67d8;
  background: #f0f2ff;
  border-color: #e0e7ff;
}

/* 优化标签样式 */
.input-top .el-tag {
  background: #ebf8ff;
  color: #2b6cb0;
  border-color: #bee3f8;
}

/* 优化空状态动画 */
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #718096;
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* GitHub风格的Markdown样式 */
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  font-size: 14px;
  line-height: 1.45;
  word-wrap: break-word;
}

.markdown-body > * {
  margin-top: 0;
  margin-bottom: 6px;
}

.markdown-body > *:last-child {
  margin-bottom: 0;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin-top: 8px;
  margin-bottom: 6px;
  font-weight: 600;
  line-height: 1.25;
  color: #2d3748;
}

.markdown-body h1 {
  padding-bottom: 0.3em;
  font-size: 1.4em;
  border-bottom: 1px solid #e2e8f0;
}

.markdown-body h2 {
  padding-bottom: 0.3em;
  font-size: 1.18em;
  border-bottom: 1px solid #e2e8f0;
}

.markdown-body h3 {
  font-size: 1.02em;
}

.markdown-body h4 {
  font-size: 1em;
}

.markdown-body h5 {
  font-size: 0.875em;
}

.markdown-body h6 {
  font-size: 0.85em;
  color: #718096;
}

.markdown-body p {
  margin: 0 0 6px;
}

.markdown-body blockquote {
  padding: 0 1em;
  color: #718096;
  border-left: 0.25em solid #e2e8f0;
  margin: 0 0 6px 0;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 1.2em;
  margin-top: 0;
  margin-bottom: 6px;
}

.markdown-body ul ul,
.markdown-body ul ol,
.markdown-body ol ul,
.markdown-body ol ol {
  margin-top: 0;
  margin-bottom: 0;
}

.markdown-body li {
  margin-bottom: 0.15em;
}

.markdown-body li > p {
  margin-top: 2px;
}

.markdown-body li + li {
  margin-top: 0.15em;
}

.markdown-body code {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  font-family: SFMono-Regular, Consolas, "Liberation Mono", Menlo, Courier, monospace;
}

.markdown-body pre {
  word-wrap: normal;
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 8px;
  overflow: auto;
  margin-top: 0;
  margin-bottom: 6px;
}

.markdown-body pre code {
  padding: 0;
  margin: 0;
  font-size: 14px;
  background-color: transparent;
  border: 0;
  word-break: normal;
  white-space: pre;
}

.markdown-body pre > code {
  display: block;
  overflow-x: auto;
}

.markdown-body table {
  border-spacing: 0;
  border-collapse: collapse;
  width: 100%;
  overflow: auto;
  margin-top: 0;
  margin-bottom: 6px;
}

.markdown-body table th {
  font-weight: 600;
  background-color: #f7fafc;
}

.markdown-body table th,
.markdown-body table td {
  padding: 6px 13px;
  border: 1px solid #e2e8f0;
}

.markdown-body table tr {
  background-color: #ffffff;
  border-top: 1px solid #c6cbd1;
}

.markdown-body table tr:nth-child(2n) {
  background-color: #f7fafc;
}

.markdown-body img {
  max-width: 100%;
  box-sizing: content-box;
  background-color: #ffffff;
  border-radius: 4px;
  margin: 6px 0;
}

.markdown-body hr {
  height: 0.25em;
  padding: 0;
  margin: 8px 0;
  background-color: #e2e8f0;
  border: 0;
}

.markdown-body strong {
  font-weight: 600;
  color: #2d3748;
}

.markdown-body em {
  font-style: italic;
}

.markdown-body a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.markdown-body a:hover {
  text-decoration: underline;
  color: #5a67d8;
}

.markdown-body del {
  text-decoration: line-through;
}

.markdown-body abbr[title] {
  border-bottom: 1px dotted #a0aec0;
  cursor: help;
}

/* 优化用户消息样式 */
.message-row.user .text {
  font-weight: 500;
}

/* 优化代码高亮样式 */
.markdown-body pre code {
  background-color: transparent;
  padding: 0;
  font-size: 14px;
  line-height: 1.5;
}

.markdown-body code::-webkit-scrollbar {
  height: 8px;
}

.markdown-body code::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.markdown-body code::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 4px;
}

.markdown-body code::-webkit-scrollbar-thumb:hover {
  background: #999;
}

/* 优化表格响应式 */
.markdown-body table {
  display: block;
  overflow-x: auto;
}

/* 优化引用样式 */
.markdown-body blockquote {
  font-style: italic;
}

/* 优化列表样式 */
.markdown-body ul li::marker {
  color: #667eea;
}

.markdown-body ol li::marker {
  font-weight: 600;
  color: #667eea;
}

/* 优化流式输出时的markdown样式 */
.streaming .text.markdown-body {
  min-height: 20px;
}

/* 增强整体视觉层次感 */
.chat-shell {
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

/* 优化滚动条样式 */
.messages {
  scrollbar-width: thin;
  scrollbar-color: #e2e8f0 #f7fafc;
}

.messages::-webkit-scrollbar {
  width: 8px;
}

.messages::-webkit-scrollbar-track {
  background: #f7fafc;
  border-radius: 4px;
}

.messages::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 4px;
}

.messages::-webkit-scrollbar-thumb:hover {
  background: #cbd5e0;
}

/* 优化头像样式 */
.avatar {
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.3);
}

/* 优化按钮样式 */
.el-button {
  border-radius: 8px;
  transition: all 0.2s ease;
}

.el-button--primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.el-button--primary:hover {
  background: linear-gradient(135deg, #5a67d8 0%, #6b46c1 100%);
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

/* 优化输入框聚焦效果 */
.input-box :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

/* 优化快捷操作按钮 */
.quick-chip {
  background: linear-gradient(135deg, #ffffff 0%, #f7fafc 100%);
  border: 1px solid #e2e8f0;
}

.quick-chip:hover {
  background: linear-gradient(135deg, #f0f2ff 0%, #e0e7ff 100%);
  border-color: #667eea;
}

.stop-btn {
  border-radius: 12px;
  padding: 10px;
  color: #fca5a5;
}

.input-meta {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #a5adcb;
}

.file-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  color: #e5e7eb;
}

@keyframes messageSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.message-enter-active,
.message-leave-active {
  transition: all 0.2s ease;
}

.message-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.message-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 900px) {
  .chat-hero {
    flex-direction: column;
  }
  .bubble {
    max-width: 90%;
  }
  .chat-panel {
    max-height: none;
  }
}

@media (max-width: 640px) {
  .chat-page {
    padding: 16px;
  }
  .chat-shell {
    border-radius: 12px;
  }
  .chat-hero {
    padding: 18px;
  }
  .quick-bar {
    padding: 12px 18px;
  }
  .input-panel {
    padding: 14px 18px 18px;
  }
}
</style>
