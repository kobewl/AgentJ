<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-toolbar">
        <span>LLM 对话 (SSE)</span>
        <el-tag v-if="conversationId" type="success">会话ID：{{ conversationId }}</el-tag>
      </div>
    </template>
    <div class="chat-panel">
      <div class="messages">
        <div v-for="(m, idx) in messages" :key="idx" :class="['msg', m.role]">
          <strong>{{ m.role === 'user' ? '用户' : '助手' }}：</strong>
          <span>{{ m.content }}</span>
        </div>
        <div v-if="streamingText" class="msg assistant">
          <strong>助手：</strong><span>{{ streamingText }}</span>
        </div>
      </div>
      <div class="composer">
        <el-input v-model="input" type="textarea" :rows="3" placeholder="输入你的问题" />
        <div class="flex-row" style="margin-top: 8px">
          <el-input v-model="conversationId" placeholder="会话ID (可空自动生成)" />
          <el-button type="primary" :loading="sending" @click="send">发送</el-button>
          <el-button :disabled="!sending" @click="stop">停止</el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { streamSse, type SseMessage } from '@/utils/sse';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

const input = ref('');
const messages = ref<ChatMessage[]>([]);
const conversationId = ref('');
const streamingText = ref('');
const sending = ref(false);
let abortController: AbortController | null = null;

const handleSseMessage = (data: SseMessage) => {
  const type = data.type as string;
  if (type === 'start' && data.conversationId) {
    conversationId.value = data.conversationId as string;
  }
  if (type === 'chunk' && typeof data.content === 'string') {
    streamingText.value += data.content;
  }
  if (type === 'done') {
    if (streamingText.value) {
      messages.value.push({ role: 'assistant', content: streamingText.value });
      streamingText.value = '';
    }
    sending.value = false;
    abortController = null;
  }
  if (type === 'error') {
    sending.value = false;
    abortController = null;
    ElMessage.error((data.message as string) || '对话出错');
  }
};

const send = async () => {
  if (!input.value) {
    ElMessage.warning('请输入内容');
    return;
  }
  messages.value.push({ role: 'user', content: input.value });
  const payload = {
    input: input.value,
    conversationId: conversationId.value || undefined,
    requestSource: 'VUE_DIALOG',
  };
  input.value = '';
  streamingText.value = '';
  sending.value = true;
  abortController = new AbortController();

  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const url = `${base}/api/executor/chat`;

  streamSse(url, payload, handleSseMessage, () => {
    sending.value = false;
    abortController = null;
  }, abortController.signal);
};

const stop = () => {
  abortController?.abort();
  sending.value = false;
  streamingText.value = '';
};
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.messages {
  min-height: 260px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  max-height: 480px;
  overflow-y: auto;
}

.msg {
  margin-bottom: 8px;
  padding: 6px 8px;
  border-radius: 6px;
}

.msg.user {
  background: #ecf5ff;
}

.msg.assistant {
  background: #f5f3ff;
}

.composer {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
}
</style>
