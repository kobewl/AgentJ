import { ElMessage } from 'element-plus';

export interface SseMessage {
  type: string;
  content?: string;
  conversationId?: string;
  message?: string;
  [key: string]: any;
}

/**
 * 流式SSE请求处理
 * @param url - SSE接口URL
 * @param payload - 请求数据
 * @param onMessage - 消息回调函数
 * @param onComplete - 完成回调函数
 * @param signal - 中断信号
 */
export async function streamSse(
  url: string,
  payload: any,
  onMessage: (data: SseMessage) => void,
  onComplete?: () => void,
  signal?: AbortSignal
): Promise<void> {
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify(payload),
      signal,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('Response body is not available');
    }

    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      
      if (done) {
        break;
      }

      if (signal?.aborted) {
        reader.cancel();
        break;
      }

      // 解码数据
      const chunk = decoder.decode(value, { stream: true });
      buffer += chunk;

      // 处理完整的事件
      const lines = buffer.split('\n');
      buffer = lines.pop() || ''; // 保留不完整的行

      for (const rawLine of lines) {
        const line = rawLine.trim();
        if (!line) continue;

        // 兼容 data:xxxx 和 data: xxxx 两种格式
        if (line.startsWith('data:')) {
          const dataStr = line.slice(5).trimStart();
          try {
            const data = JSON.parse(dataStr) as SseMessage;
            onMessage(data);
          } catch (error) {
            console.error('Failed to parse SSE data:', error);
            ElMessage.error('数据解析失败');
          }
        } else if (line.startsWith('event:')) {
          const eventType = line.slice(6).trimStart();
          console.log('SSE Event:', eventType);
        } else if (line.startsWith('id:')) {
          const eventId = line.slice(3).trimStart();
          console.log('SSE ID:', eventId);
        }
      }
    }

    // 处理缓冲区中剩余的数据
    const trimmed = buffer.trim();
    if (trimmed && trimmed.startsWith('data:')) {
      const dataStr = trimmed.slice(5).trimStart();
      try {
        const data = JSON.parse(dataStr) as SseMessage;
        onMessage(data);
      } catch (error) {
        console.error('Failed to parse remaining SSE data:', error);
      }
    }

    onComplete?.();

  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      console.log('SSE stream aborted');
      onComplete?.();
      return;
    }
    
    console.error('SSE stream error:', error);
    ElMessage.error('连接失败，请检查网络连接');
    throw error;
  }
}

/**
 * 创建SSE连接
 * @param url - SSE接口URL
 * @param onMessage - 消息回调函数
 * @param onError - 错误回调函数
 * @param onOpen - 连接打开回调函数
 * @param signal - 中断信号
 */
export function createSseConnection(
  url: string,
  onMessage: (data: SseMessage) => void,
  onError?: (error: Event) => void,
  onOpen?: (event: Event) => void,
  signal?: AbortSignal
): EventSource {
  const eventSource = new EventSource(url);

  eventSource.onopen = (event) => {
    console.log('SSE connection opened');
    onOpen?.(event);
  };

  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data) as SseMessage;
      onMessage(data);
    } catch (error) {
      console.error('Failed to parse SSE message:', error);
      ElMessage.error('消息解析失败');
    }
  };

  eventSource.onerror = (event) => {
    console.error('SSE connection error:', event);
    onError?.(event);
    
    if (eventSource.readyState === EventSource.CLOSED) {
      ElMessage.error('连接已关闭');
    }
  };

  // 监听中断信号
  if (signal) {
    signal.addEventListener('abort', () => {
      eventSource.close();
      console.log('SSE connection closed by abort signal');
    });
  }

  return eventSource;
}

/**
 * 解析SSE数据流
 * @param reader - ReadableStream reader
 * @param onMessage - 消息回调函数
 * @param signal - 中断信号
 */
export async function parseSseStream(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  onMessage: (data: SseMessage) => void,
  signal?: AbortSignal
): Promise<void> {
  const decoder = new TextDecoder();
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      
      if (done || signal?.aborted) {
        break;
      }

      const chunk = decoder.decode(value, { stream: true });
      buffer += chunk;

      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.trim() === '') continue;
        
        if (line.startsWith('data: ')) {
          const dataStr = line.slice(6);
          try {
            const data = JSON.parse(dataStr) as SseMessage;
            onMessage(data);
          } catch (error) {
            console.error('Failed to parse SSE data:', error);
          }
        }
      }
    }
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      console.log('SSE stream parsing aborted');
      return;
    }
    
    console.error('SSE stream parsing error:', error);
    throw error;
  } finally {
    reader.releaseLock();
  }
}

/**
 * 重连SSE连接
 * @param url - SSE接口URL
 * @param onMessage - 消息回调函数
 * @param maxRetries - 最大重试次数
 * @param retryDelay - 重试延迟（毫秒）
 * @param signal - 中断信号
 */
export async function reconnectSse(
  url: string,
  onMessage: (data: SseMessage) => void,
  maxRetries = 3,
  retryDelay = 1000,
  signal?: AbortSignal
): Promise<EventSource | null> {
  let retries = 0;
  
  while (retries < maxRetries) {
    if (signal?.aborted) {
      console.log('SSE reconnection aborted');
      return null;
    }

    try {
      const eventSource = createSseConnection(
        url,
        onMessage,
        undefined,
        undefined,
        signal
      );
      
      return eventSource;
    } catch (error) {
      retries++;
      console.error(`SSE connection attempt ${retries} failed:`, error);
      
      if (retries < maxRetries) {
        await new Promise(resolve => setTimeout(resolve, retryDelay));
      }
    }
  }
  
  ElMessage.error('连接失败，请检查网络连接');
  return null;
}
