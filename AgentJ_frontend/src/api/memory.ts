import http from './http';
import type { MemoryItem, MemoryResponse } from './types';

export const getMemories = () => http.get<MemoryResponse<MemoryItem[]>>('/api/memories');

export const getMemory = (conversationId: string) => http.get<MemoryResponse<MemoryItem>>('/api/memories/single', { params: { conversationId } });

export const createMemory = (memory: MemoryItem) => http.post<MemoryResponse<MemoryItem>>('/api/memories', memory);

export const updateMemory = (memory: MemoryItem) => http.put<MemoryResponse<MemoryItem>>('/api/memories', memory);

export const deleteMemory = (conversationId: string) => http.delete<MemoryResponse<unknown>>(`/api/memories/${conversationId}`);

export const getConversationHistory = (conversationId: string) =>
  http.get(`/api/memories/${conversationId}/history`);
