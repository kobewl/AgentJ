<template>
  <div class="knowledge-chat-page">
    <div class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo" v-if="!sidebarCollapsed">
          <el-icon size="20"><Collection /></el-icon>
          <span>知识库</span>
        </div>
        <el-button :icon="Fold" text @click="sidebarCollapsed = !sidebarCollapsed" class="collapse-btn" />
      </div>
      
      <div class="sidebar-section" v-if="!sidebarCollapsed">
        <div class="section-label">选择知识库</div>
        <el-select 
          v-model="selectedBaseId" 
          placeholder="请选择知识库" 
          class="base-select"
          @change="resetChat"
        >
          <el-option-group>
            <el-option 
              v-for="base in bases" 
              :key="base.id" 
              :label="base.name" 
              :value="base.id"
            >
              <span>{{ base.name }}</span>
            </el-option>
          </el-option-group>
        </el-select>
      </div>

      <div class="sidebar-section" v-if="!sidebarCollapsed">
        <div class="section-label">对话历史</div>
        <div class="chat-history">
          <div 
            v-for="chat in chatHistories" 
            :key="chat.id"
            :class="['history-item', { active: currentChatId === chat.id }]"
            @click="loadChat(chat)"
          >
            <el-icon><ChatLineRound /></el-icon>
            <span class="history-title">{{ chat.title || '新对话' }}</span>
            <el-dropdown trigger="click" @command="(cmd) => handleHistoryCommand(cmd, chat)">
              <el-icon class="history-action" @click.stop><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="rename">
                    <el-icon><Edit /></el-icon>重命名
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon>删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <el-empty v-if="chatHistories.length === 0" description="暂无对话历史" :image-size="60" />
        </div>
        <el-button 
          type="primary" 
          :icon="Plus" 
          class="new-chat-btn" 
          @click="createNewChat"
          v-if="!sidebarCollapsed"
        >
          新建对话
        </el-button>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-header">
        <div class="header-left">
          <el-button :icon="Expand" text @click="sidebarCollapsed = !sidebarCollapsed" class="mobile-menu-btn" />
          <div class="header-info">
            <h2 class="chat-title">{{ currentChatTitle }}</h2>
            <div class="chat-meta" v-if="selectedBase">
              <el-icon><Collection /></el-icon>
              <span>{{ selectedBase.name }}</span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-tooltip content="清空当前对话">
            <el-button :icon="Delete" text @click="clearChat" :disabled="messages.length === 0">
              清空
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div class="chat-container">
        <div class="chat-messages" ref="messagesRef">
          <div class="welcome-message" v-if="messages.length === 0 && !loading">
            <div class="welcome-icon">
              <el-icon size="48"><ChatDotRound /></el-icon>
            </div>
            <h3>欢迎使用知识库对话</h3>
            <p>基于 "{{ selectedBase?.name || '请选择知识库' }}" 进行智能问答</p>
            <div class="suggestions" v-if="suggestions.length > 0">
              <div 
                v-for="(item, idx) in suggestions" 
                :key="idx"
                class="suggestion-item"
                @click="sendSuggestion(item)"
              >
                <el-icon><Pointer /></el-icon>
                <span>{{ item }}</span>
              </div>
            </div>
          </div>

          <template v-for="(msg, idx) in messages" :key="idx">
            <div :class="['message-wrapper', msg.role]">
              <div class="message-avatar">
                <el-avatar v-if="msg.role === 'user'" :size="36" class="user-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div v-else class="bot-avatar">
                  <el-icon size="20"><Service /></el-icon>
                </div>
              </div>
              <div class="message-content">
                <div class="message-bubble">
                  <div class="message-text markdown-body" v-html="renderContent(msg.content)"></div>
                  <div class="message-time">{{ msg.time }}</div>
                </div>
                <div class="source-chunks" v-if="msg.role === 'assistant' && msg.sources && msg.sources.length">
                  <div class="sources-header" @click="toggleSources(idx)">
                    <el-icon :class="['sources-toggle', { expanded: expandedSources.includes(idx) }]">
                      <ArrowDown />
                    </el-icon>
                    <span>参考来源 ({{ msg.sources.length }})</span>
                  </div>
                  <div class="sources-list" v-show="expandedSources.includes(idx)">
                    <div 
                      v-for="(source, sIdx) in msg.sources" 
                      :key="sIdx"
                      class="source-item"
                    >
                      <div class="source-file">
                        <el-icon><Document /></el-icon>
                        <span>{{ source.fileName }}</span>
                      </div>
                      <div class="source-text">{{ source.content }}</div>
                      <div class="source-score">
                        <el-progress 
                          :percentage="Math.round(source.score * 100)" 
                          :stroke-width="4"
                          :color="getScoreColor(source.score)"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <div class="typing-indicator" v-if="loading">
            <div class="message-wrapper assistant">
              <div class="message-avatar">
                <div class="bot-avatar">
                  <el-icon size="20"><Service /></el-icon>
                </div>
              </div>
              <div class="message-content">
                <div class="message-bubble">
                  <div class="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <div class="input-container">
            <div class="input-wrapper">
              <div class="input-left">
                <div class="tool-buttons">
                  <el-tooltip content="上传文档" placement="top">
                    <div class="tool-btn" @click="handleUpload">
                      <el-icon size="18"><Document /></el-icon>
                    </div>
                  </el-tooltip>
                  <el-tooltip content="快捷指令" placement="top">
                    <div class="tool-btn">
                      <el-icon size="18"><Grid /></el-icon>
                    </div>
                  </el-tooltip>
                </div>
              </div>
              <div class="input-center">
                <el-input
                  v-model="question"
                  type="textarea"
                  :rows="1"
                  :autosize="{ minRows: 1, maxRows: 6 }"
                  placeholder="输入您的问题，AI将基于知识库为您解答..."
                  @keydown.enter.exact.prevent="send"
                  @keydown.shift.enter.prevent="handleShiftEnter"
                  ref="inputRef"
                  class="chat-input"
                />
              </div>
              <div class="input-right">
                <div class="char-indicator" :class="{ 'near-limit': question.length > 1800 }">
                  <span class="char-count">{{ question.length }}</span>
                  <span class="char-limit">/2000</span>
                </div>
                <el-button 
                  type="primary" 
                  :loading="loading" 
                  @click="send"
                  :disabled="!question.trim() || !selectedBaseId"
                  class="send-btn"
                >
                  <el-icon v-if="!loading"><Promotion /></el-icon>
                  <span v-if="!loading">发送</span>
                </el-button>
              </div>
            </div>
            <div class="input-footer">
              <div class="footer-left">
                <div class="footer-info">
                  <span class="info-item">
                    <el-icon size="12"><Lock /></el-icon>
                    <span>隐私安全</span>
                  </span>
                  <span class="footer-dot">·</span>
                  <span class="info-item">
                    <el-icon size="12"><Connection /></el-icon>
                    <span>{{ selectedBase?.name || '默认知识库' }}</span>
                  </span>
                </div>
              </div>
              <div class="footer-right">
                <div class="shortcut-hints">
                  <span class="hint">
                    <kbd>Enter</kbd> 发送
                  </span>
                  <span class="hint-divider">/</span>
                  <span class="hint">
                    <kbd>Shift + Enter</kbd> 换行
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showRenameDialog" title="重命名对话" width="400px">
      <el-input v-model="renameValue" placeholder="输入对话标题" maxlength="50" show-word-limit />
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { 
  Collection, ChatLineRound, MoreFilled, Edit, Delete, Plus, 
  Expand, Fold, Setting, ChatDotRound, User, Service,
  Document, ArrowDown, Pointer, Promotion, Grid, Lock, Connection
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listKnowledgeBases, chatWithKnowledge, KnowledgeItem } from '@/api/knowledge';
import {
  listConversations,
  createConversation,
  updateConversation,
  deleteConversation,
  listMessages,
  createMessage,
  generateConversationTitle,
  type ConversationSession,
  type ConversationMessage,
} from '@/api/conversation';
import { formatTime } from '@/utils/format';
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';

// 配置marked，支持代码高亮
const markedOptions = {
  highlight: function(code: string, lang?: string) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value;
      } catch (__) {}
    }
    return hljs.highlightAuto(code).value;
  },
  breaks: true,
  gfm: true,
} as unknown as any;

marked.setOptions(markedOptions);

interface SourceChunk {
  fileName: string;
  content: string;
  score: number;
  location?: string;
}

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  time: string;
  sources?: SourceChunk[];
}

interface ChatHistory {
  id: string;
  title: string;
  knowledgeBaseId: string;
  createdAt: string;
  updatedAt: string;
}

const bases = ref<KnowledgeItem[]>([]);
const selectedBaseId = ref<string>('');
const selectedBase = computed(() => bases.value.find(b => b.id === selectedBaseId.value));
const messages = ref<ChatMessage[]>([]);
const question = ref('');
const loading = ref(false);
const messagesRef = ref<HTMLDivElement | null>(null);
const inputRef = ref<any>(null);
const sidebarCollapsed = ref(false);

const chatHistories = ref<ChatHistory[]>([]);
const currentChatId = ref<string>('');
const currentChatTitle = ref('新对话');
const showRenameDialog = ref(false);
const renameValue = ref('');
const renamingChat = ref<ChatHistory | null>(null);

const expandedSources = ref<number[]>([]);

const suggestions = ref<string[]>([
  '这个知识库包含哪些内容？',
  '如何上传和管理文档？',
  '帮我总结主要知识点'
]);

const fetchBases = async () => {
  try {
    const resp = await listKnowledgeBases();
    bases.value = resp.data.data || [];
    if (!selectedBaseId.value && bases.value.length > 0) {
      selectedBaseId.value = bases.value[0].id;
      // 加载该知识库的对话历史
      await loadChatHistories();
    }
  } catch (e) {
    ElMessage.error('加载知识库列表失败');
  }
};

// 加载对话历史
const loadChatHistories = async () => {
  if (!selectedBaseId.value) return;
  try {
    const resp = await listConversations({
      conversationType: 'KNOWLEDGE',
      knowledgeBaseId: selectedBaseId.value,
      page: 1,
      size: 50,
    });
    const items = resp.data.data?.items || [];
    chatHistories.value = items.map(item => ({
      id: item.id,
      title: item.title || '新对话',
      knowledgeBaseId: item.knowledge_base_id || '',
      createdAt: item.created_at,
      updatedAt: item.updated_at,
    }));
  } catch (e) {
    console.error('加载对话历史失败', e);
  }
};

const createNewChat = () => {
  currentChatId.value = '';
  currentChatTitle.value = '新对话';
  messages.value = [];
};

// 加载对话消息
const loadChat = async (chat: ChatHistory) => {
  currentChatId.value = chat.id;
  currentChatTitle.value = chat.title || '新对话';
  selectedBaseId.value = chat.knowledgeBaseId;
  
  try {
    const resp = await listMessages(chat.id, { page: 1, size: 200, includeDeleted: false });
    const items = resp.data.data?.items || [];
    messages.value = items.map((m: ConversationMessage) => ({
      id: m.id,
      role: (m.role === 'user' ? 'user' : 'assistant') as 'user' | 'assistant',
      content: m.content,
      time: formatTime(m.created_at),
      sources: [],
    }));
    await nextTick();
    scrollToBottom();
  } catch (e) {
    console.error('加载对话消息失败', e);
    ElMessage.error('加载对话消息失败');
  }
};

const handleHistoryCommand = async (command: string, chat: ChatHistory) => {
  if (command === 'rename') {
    renamingChat.value = chat;
    renameValue.value = chat.title || '';
    showRenameDialog.value = true;
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除这个对话吗？', '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      });
      await deleteConversation(chat.id);
      chatHistories.value = chatHistories.value.filter(c => c.id !== chat.id);
      if (currentChatId.value === chat.id) {
        createNewChat();
      }
      ElMessage.success('删除成功');
    } catch {}
  }
};

const confirmRename = async () => {
  if (renamingChat.value && renameValue.value.trim()) {
    try {
      await updateConversation(renamingChat.value.id, { title: renameValue.value.trim() });
      renamingChat.value.title = renameValue.value.trim();
      if (currentChatId.value === renamingChat.value.id) {
        currentChatTitle.value = renameValue.value.trim();
      }
      showRenameDialog.value = false;
      ElMessage.success('重命名成功');
    } catch (e) {
      ElMessage.error('重命名失败');
    }
  }
};

const sendSuggestion = (suggestion: string) => {
  question.value = suggestion;
  send();
};

const send = async () => {
  if (!selectedBaseId.value) {
    ElMessage.warning('请先选择知识库');
    return;
  }
  const content = question.value.trim();
  if (!content) return;

  const isFirstMessage = messages.value.length === 0;
  let sessionId = currentChatId.value;

  // 如果没有当前会话，先创建会话
  if (!sessionId) {
    try {
      const resp = await createConversation({
        title: '新对话',
        conversation_type: 'KNOWLEDGE',
        knowledge_base_id: selectedBaseId.value,
      });
      const created = resp.data.data;
      sessionId = created.id;
      currentChatId.value = sessionId;
      currentChatTitle.value = '新对话';
      chatHistories.value.unshift({
        id: sessionId,
        title: '新对话',
        knowledgeBaseId: selectedBaseId.value,
        createdAt: created.created_at,
        updatedAt: created.updated_at,
      });
    } catch (e) {
      ElMessage.error('创建会话失败');
      return;
    }
  }

  const userMsg: ChatMessage = {
    id: `msg_${Date.now()}`,
    role: 'user',
    content,
    time: formatTime(new Date().toISOString())
  };
  messages.value.push(userMsg);
  question.value = '';
  await nextTick();
  scrollToBottom();

  // 保存用户消息到后端
  createMessage(sessionId, {
    role: 'user',
    content,
    model_name: 'LongCat-Flash-Chat',
  }).catch(() => {});

  loading.value = true;
  try {
    const resp = await chatWithKnowledge(selectedBaseId.value, content);
    const data = resp.data.data;
    const assistantContent = data?.answer || '未获取到回答';
    const assistantMsg: ChatMessage = {
      id: `msg_${Date.now() + 1}`,
      role: 'assistant',
      content: assistantContent,
      time: formatTime(new Date().toISOString()),
      sources: []
    };
    messages.value.push(assistantMsg);

    // 保存AI回复到后端
    createMessage(sessionId, {
      role: 'assistant',
      content: assistantContent,
      model_name: 'LongCat-Flash-Chat',
    }).catch(() => {});

    // 首轮对话后自动生成标题
    if (isFirstMessage && sessionId) {
      try {
        const titleResp = await generateConversationTitle({
          userContent: content,
          assistantContent: assistantContent,
        });
        const newTitle = titleResp.data.data?.title || content.slice(0, 20);
        await updateConversation(sessionId, { title: newTitle });
        currentChatTitle.value = newTitle;
        // 更新历史记录中的标题
        const history = chatHistories.value.find(h => h.id === sessionId);
        if (history) {
          history.title = newTitle;
        }
      } catch (e) {
        console.error('生成标题失败', e);
      }
    }

    // 更新历史记录的更新时间
    const historyItem = chatHistories.value.find(h => h.id === sessionId);
    if (historyItem) {
      historyItem.updatedAt = new Date().toISOString();
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '对话失败');
  } finally {
    loading.value = false;
    await nextTick();
    scrollToBottom();
  }
};

const handleShiftEnter = () => {
  const input = document.querySelector('.chat-input textarea') as HTMLTextAreaElement;
  if (input) {
    const start = input.selectionStart;
    const end = input.selectionEnd;
    question.value = question.value.substring(0, start) + '\n' + question.value.substring(end);
    nextTick(() => {
      input.selectionStart = input.selectionEnd = start + 1;
      input.focus();
    });
  }
};

const handleUpload = () => {
  ElMessage.info('文档上传功能开发中');
};

const resetChat = () => {
  messages.value = [];
  currentChatId.value = '';
  currentChatTitle.value = '新对话';
};

const clearChat = async () => {
  try {
    await ElMessageBox.confirm('确定要清空当前对话吗？', '确认清空', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    messages.value = [];
    currentChatTitle.value = '新对话';
  } catch {}
};

const toggleSources = (idx: number) => {
  const index = expandedSources.value.indexOf(idx);
  if (index > -1) {
    expandedSources.value.splice(index, 1);
  } else {
    expandedSources.value.push(idx);
  }
};

const getScoreColor = (score: number) => {
  if (score >= 0.8) return '#67c23a';
  if (score >= 0.6) return '#e6a23c';
  return '#f56c6c';
};

const renderContent = (text: string) => {
  return marked(text);
};

const scrollToBottom = () => {
  const el = messagesRef.value;
  if (el) {
    el.scrollTop = el.scrollHeight;
  }
};

onMounted(() => {
  fetchBases();
});

// 切换知识库时重新加载对话历史
watch(selectedBaseId, async (newVal, oldVal) => {
  if (newVal && newVal !== oldVal) {
    resetChat();
    await loadChatHistories();
  }
});

watch(messages, () => {
  nextTick(() => scrollToBottom());
}, { deep: true });
</script>

<style scoped>
.knowledge-chat-page {
  display: flex;
  height: calc(100vh - 100px);
  background: var(--bg-secondary);
}

.chat-sidebar {
  width: 280px;
  background: var(--bg-primary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.chat-sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: var(--primary-color);
}

.collapse-btn {
  padding: 8px;
}

.sidebar-section {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.section-label {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.base-select {
  width: 100%;
}

.chat-history {
  max-height: 300px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
  margin-bottom: 4px;
}

.history-item:hover {
  background: var(--bg-tertiary);
}

.history-item.active {
  background: var(--primary-weak);
  color: var(--primary-color);
}

.history-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.history-action {
  opacity: 0;
  transition: var(--transition);
  padding: 4px;
}

.history-item:hover .history-action {
  opacity: 1;
}

.new-chat-btn {
  width: 100%;
  margin-top: 12px;
}

.chat-main {
  flex: 1;
  display: flex;
  overflow-y: auto;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-menu-btn {
  display: none;
}

.chat-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.chat-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-message {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 24px;
}

.welcome-message h3 {
  margin: 0 0 8px;
  font-size: 24px;
  color: var(--text-primary);
}

.welcome-message p {
  margin: 0 0 32px;
  color: var(--text-secondary);
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 600px;
  width: 100%;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
  text-align: left;
  font-size: 14px;
}

.suggestion-item:hover {
  border-color: var(--primary-color);
  background: var(--primary-weak);
}

.message-wrapper {
  display: flex;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.message-wrapper.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.user-avatar {
  background: var(--primary-color);
}

.bot-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.message-content {
  flex: 1;
  max-width: calc(100% - 48px);
}

.message-wrapper.user .message-content {
  display: flex;
  justify-content: flex-end;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: var(--radius-lg);
  background: var(--bg-primary);
  box-shadow: var(--shadow-sm);
  position: relative;
}

.message-wrapper.user .message-bubble {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
  color: white;
}

.message-text {
  line-height: 1.7;
  font-size: 15px;
}

.message-text :deep(p) {
  margin: 0 0 12px;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(pre) {
  background: var(--bg-tertiary);
  padding: 12px;
  border-radius: var(--radius-md);
  overflow-x: auto;
}

.message-text :deep(code) {
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  background: var(--bg-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
}

.message-text :deep(pre code) {
  background: none;
  padding: 0;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  margin: 0 0 12px;
  padding-left: 24px;
}

.message-text :deep(li) {
  margin: 4px 0;
}

.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3),
.message-text :deep(h4) {
  margin: 16px 0 8px;
  font-weight: 600;
}

.message-text :deep(h1) {
  font-size: 1.5em;
}

.message-text :deep(h2) {
  font-size: 1.3em;
}

.message-text :deep(h3) {
  font-size: 1.1em;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-text :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 16px;
  border-left: 4px solid var(--primary-color);
  background: var(--bg-tertiary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.message-text :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 16px 0;
}

.message-text :deep(a) {
  color: var(--primary-color);
  text-decoration: none;
}

.message-text :deep(a:hover) {
  text-decoration: underline;
}

.message-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 8px;
  text-align: right;
}

.message-wrapper.user .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.source-chunks {
  margin-top: 12px;
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.sources-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--bg-tertiary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: var(--transition);
}

.sources-header:hover {
  background: var(--bg-secondary);
}

.sources-toggle {
  transition: transform 0.2s ease;
}

.sources-toggle.expanded {
  transform: rotate(180deg);
}

.sources-list {
  padding: 8px;
}

.source-item {
  padding: 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
  margin-bottom: 8px;
}

.source-item:last-child {
  margin-bottom: 0;
}

.source-item:hover {
  background: var(--bg-tertiary);
}

.source-file {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--primary-color);
  margin-bottom: 6px;
}

.source-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.source-score {
  margin-top: 8px;
}

.source-score :deep(.el-progress__text) {
  font-size: 11px !important;
  min-width: 28px;
}

.typing-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-tertiary);
  animation: typing-bounce 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) {
  animation-delay: 0s;
}

.typing-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-8px);
  }
}

.chat-input-area {
  padding: 20px 24px 24px;
  background: var(--bg-primary);
  border-top: 1px solid var(--border-color);
}

.input-container {
  max-width: 900px;
  margin: 0 auto;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border: 2px solid var(--border-color);
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.input-wrapper:hover {
  border-color: var(--primary-weak);
  background: var(--bg-primary);
}

.input-wrapper:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
  background: var(--bg-primary);
}

.input-left {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.tool-buttons {
  display: flex;
  gap: 4px;
}

.tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.tool-btn:hover {
  background: var(--bg-tertiary);
  color: var(--primary-color);
  transform: translateY(-1px);
}

.input-center {
  flex: 1;
  min-width: 0;
}

.chat-input {
  width: 100%;
}

.chat-input :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  resize: none;
  padding: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-primary);
  min-height: 24px !important;
  field-sizing: content;
  font-family: inherit;
}

.chat-input :deep(.el-textarea__inner::placeholder) {
  color: var(--text-tertiary);
  font-weight: 400;
}

.chat-input :deep(.el-textarea__inner:focus) {
  outline: none;
  box-shadow: none;
}

.chat-input :deep(.el-textarea__inner::-webkit-scrollbar) {
  width: 4px;
}

.chat-input :deep(.el-textarea__inner::-webkit-scrollbar-thumb) {
  background: var(--border-color);
  border-radius: 2px;
}

.input-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.char-indicator {
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--text-tertiary);
  transition: color 0.2s ease;
}

.char-indicator.near-limit {
  color: var(--warning-color);
}

.char-count {
  font-weight: 600;
  color: var(--text-secondary);
}

.char-indicator.near-limit .char-count {
  color: var(--warning-color);
}

.char-limit {
  opacity: 0.7;
}

.send-btn {
  height: 40px;
  padding: 0 20px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.35);
}

.send-btn:active:not(:disabled) {
  transform: translateY(0);
}

.send-btn:disabled {
  opacity: 0.5;
  background: var(--bg-tertiary);
  box-shadow: none;
}

.send-text {
  font-weight: 500;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 0 8px;
}

.footer-left {
  flex: 1;
}

.footer-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-item .el-icon {
  opacity: 0.7;
}

.footer-dot {
  color: var(--border-color);
}

.footer-right {
  flex-shrink: 0;
}

.shortcut-hints {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.hint {
  display: flex;
  align-items: center;
  gap: 4px;
}

kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  font-size: 11px;
  font-family: inherit;
  font-weight: 500;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-secondary);
}

.hint-divider {
  color: var(--border-color);
  opacity: 0.5;
}

@media (max-width: 768px) {
  .input-footer {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
  
  .footer-right {
    width: 100%;
  }
  
  .shortcut-hints {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .chat-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(-100%);
  }

  .chat-sidebar:not(.collapsed) {
    transform: translateX(0);
  }

  .mobile-menu-btn {
    display: block;
  }

  .chat-header {
    padding: 12px 16px;
  }

  .chat-messages {
    padding: 16px;
  }

  .suggestions {
    grid-template-columns: 1fr;
  }

  .message-wrapper {
    max-width: 100%;
  }

  .chat-input-area {
    padding: 12px 16px 16px;
  }
}
</style>
