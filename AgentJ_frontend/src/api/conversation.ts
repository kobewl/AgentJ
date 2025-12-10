import http from './http';

export interface ConversationSession {
  id: string;
  user_id: number;
  title?: string;
  summary?: string;
  model_name: string;
  last_message_at?: string;
  is_deleted: boolean;
  created_at: string;
  updated_at: string;
}

export interface ConversationMessage {
  id: string;
  conversation_id: string;
  user_id?: number | null;
  role: string;
  content: string;
  model_name: string;
  tokens_used: number;
  input_tokens: number;
  completion_tokens: number;
  images: string[];
  is_deleted: boolean;
  created_at: string;
  updated_at: string;
}

export interface PagedResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export const listConversations = (params: {
  page?: number;
  size?: number;
  keyword?: string;
  includeDeleted?: boolean;
}) =>
  http.get<{ data: PagedResult<ConversationSession>; success: boolean }>('/api/conversations', {
    params,
  });

export const createConversation = (data: Partial<ConversationSession>) =>
  http.post<{ data: ConversationSession; success: boolean }>('/api/conversations', data);

export const updateConversation = (id: string, data: Partial<ConversationSession>) =>
  http.put<{ data: ConversationSession; success: boolean }>(`/api/conversations/${id}`, data);

export const generateConversationTitle = (data: { userContent?: string; assistantContent?: string }) =>
  http.post<{ data: { title: string }; success: boolean }>('/api/conversations/title/generate', data);

export const deleteConversation = (id: string) =>
  http.delete<{ success: boolean }>(`/api/conversations/${id}`);

export const restoreConversation = (id: string) =>
  http.post<{ success: boolean }>(`/api/conversations/${id}/restore`);

export const listMessages = (conversationId: string, params: { page?: number; size?: number; includeDeleted?: boolean }) =>
  http.get<{ data: PagedResult<ConversationMessage>; success: boolean }>(
    `/api/conversations/${conversationId}/messages`,
    { params },
  );

export const createMessage = (conversationId: string, data: Partial<ConversationMessage>) =>
  http.post<{ data: ConversationMessage; success: boolean }>(`/api/conversations/${conversationId}/messages`, data);

export const deleteMessage = (conversationId: string, messageId: string) =>
  http.delete<{ success: boolean }>(`/api/conversations/${conversationId}/messages/${messageId}`);

export const restoreMessage = (conversationId: string, messageId: string) =>
  http.post<{ success: boolean }>(`/api/conversations/${conversationId}/messages/${messageId}/restore`);

export const searchMessages = (params: {
  keyword: string;
  conversationId?: string;
  page?: number;
  size?: number;
  includeDeleted?: boolean;
}) =>
  http.get<{ data: PagedResult<ConversationMessage>; success: boolean }>(
    '/api/conversations/messages/search',
    { params },
  );

