<template>
  <div class="chat-page">
    <div class="chat-header">
      <div>
        <h2>知识库对话</h2>
        <p class="subtitle">选择知识库，基于知识进行问答</p>
      </div>
      <el-space>
        <el-select v-model="selectedBaseId" placeholder="选择知识库" style="width: 240px" @change="resetChat">
          <el-option v-for="item in bases" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" @click="fetchBases" text>刷新</el-button>
      </el-space>
    </div>

    <el-card class="chat-card" shadow="never">
      <div class="chat-body" ref="scrollRef">
        <div v-for="(msg, idx) in messages" :key="idx" :class="['bubble', msg.role]">
          <div class="bubble-meta">
            <el-tag size="small" :type="msg.role === 'user' ? 'primary' : 'success'">
              {{ msg.role === 'user' ? '你' : 'AgentJ' }}
            </el-tag>
            <span class="muted">{{ msg.time }}</span>
          </div>
          <div class="bubble-content" v-html="renderContent(msg.content)"></div>
        </div>
        <div v-if="loadingAnswer" class="bubble assistant">
          <div class="bubble-meta">
            <el-tag size="small" type="success">AgentJ</el-tag>
            <span class="muted">思考中...</span>
          </div>
          <div class="bubble-content typing">···</div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          :autosize="{ minRows: 2, maxRows: 6 }"
          placeholder="输入问题，回车发送"
          @keyup.enter.exact.prevent="send"
        />
        <div class="input-actions">
          <el-button type="primary" :icon="Promotion" :loading="loadingAnswer" @click="send">发送</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue';
import { Promotion, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { listKnowledgeBases, chatWithKnowledge, KnowledgeItem } from '@/api/knowledge';
import { formatTime } from '@/utils/format';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  time: string;
}

const bases = ref<KnowledgeItem[]>([]);
const selectedBaseId = ref<string>('');
const messages = ref<ChatMessage[]>([]);
const question = ref('');
const loadingAnswer = ref(false);
const scrollRef = ref<HTMLDivElement | null>(null);

const fetchBases = async () => {
  const resp = await listKnowledgeBases();
  bases.value = resp.data.data || [];
  if (!selectedBaseId.value && bases.value.length > 0) {
    selectedBaseId.value = bases.value[0].id;
  }
};

const send = async () => {
  if (!selectedBaseId.value) {
    ElMessage.warning('请先选择知识库');
    return;
  }
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题');
    return;
  }
  const content = question.value.trim();
  messages.value.push({ role: 'user', content, time: formatTime(new Date().toISOString()) });
  question.value = '';
  await nextTick();
  scrollToBottom();

  loadingAnswer.value = true;
  try {
    const resp = await chatWithKnowledge(selectedBaseId.value, content);
    const answer = resp.data.data?.answer || '未获取到回答';
    messages.value.push({ role: 'assistant', content: answer, time: formatTime(new Date().toISOString()) });
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '对话失败');
  } finally {
    loadingAnswer.value = false;
    await nextTick();
    scrollToBottom();
  }
};

const resetChat = () => {
  messages.value = [];
};

const renderContent = (text: string) => {
  return text.replaceAll('\n', '<br/>');
};

const scrollToBottom = () => {
  const el = scrollRef.value;
  if (el) {
    el.scrollTop = el.scrollHeight;
  }
};

onMounted(() => {
  fetchBases();
});
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.subtitle {
  color: var(--text-secondary);
  margin: 4px 0 0;
}
.chat-card {
  min-height: 520px;
  display: flex;
  flex-direction: column;
}
.chat-body {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
.bubble {
  max-width: 80%;
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: 12px;
  background: white;
  box-shadow: var(--shadow-sm, 0 2px 6px rgba(0,0,0,0.06));
}
.bubble.user {
  margin-left: auto;
  background: #e8f0ff;
}
.bubble.assistant {
  margin-right: auto;
  background: #f6f7f9;
}
.bubble-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.muted {
  color: var(--text-secondary);
  font-size: 12px;
}
.bubble-content {
  white-space: pre-wrap;
  color: var(--text-primary);
}
.typing {
  letter-spacing: 3px;
}
.chat-input {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  background: var(--bg-secondary);
}
.input-actions {
  margin-top: 10px;
  text-align: right;
}
</style>
