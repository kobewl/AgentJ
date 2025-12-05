export type SseMessage = Record<string, unknown>;

export async function streamSse(
  url: string,
  payload: Record<string, unknown>,
  onMessage: (data: SseMessage) => void,
  onError?: (error: unknown) => void,
  signal?: AbortSignal,
) {
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
      signal,
    });

    if (!response.ok || !response.body) {
      throw new Error(`SSE 请求失败: ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      for (const line of lines) {
        if (!line.startsWith('data:')) continue;
        const dataStr = line.replace(/^data:\s*/, '').trim();
        if (!dataStr) continue;
        try {
          const parsed = JSON.parse(dataStr);
          onMessage(parsed);
        } catch (err) {
          console.warn('SSE 数据解析失败', err, dataStr);
        }
      }
    }
  } catch (error) {
    console.error('SSE 连接失败', error);
    onError?.(error);
  }
}
