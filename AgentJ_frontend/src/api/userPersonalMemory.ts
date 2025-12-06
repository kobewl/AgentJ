import http from './http';
import type { UserPersonalMemoryItem, MemoryResponse } from './types';

/**
 * 获取用户的所有个人记忆
 */
export const getUserPersonalMemories = (userId: number) => 
  http.get<MemoryResponse<UserPersonalMemoryItem[]>>(`/api/personal-memories/${userId}`);

/**
 * 根据记忆键获取个人记忆
 */
export const getUserPersonalMemory = (userId: number, memoryKey: string) => 
  http.get<MemoryResponse<UserPersonalMemoryItem>>(`/api/personal-memories/${userId}/${memoryKey}`);

/**
 * 创建或更新个人记忆
 */
export const saveUserPersonalMemory = (userId: number, memory: UserPersonalMemoryItem) => 
  http.post<MemoryResponse<UserPersonalMemoryItem>>(`/api/personal-memories/${userId}`, memory);

/**
 * 删除个人记忆
 */
export const deleteUserPersonalMemory = (userId: number, memoryKey: string) => 
  http.delete<MemoryResponse<null>>(`/api/personal-memories/${userId}/${memoryKey}`);

/**
 * 标记记忆为已使用
 */
export const markUserPersonalMemoryUsed = (userId: number, memoryKey: string) => 
  http.post<MemoryResponse<{ updated: boolean }>>(`/api/personal-memories/${userId}/mark-used/${memoryKey}`);