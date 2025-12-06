import http from './http';
import type { ExecutorResponse } from './types';

export interface ExecuteRequest {
  toolName: string;
  uploadedFiles?: string[];
  replacementParams?: Record<string, unknown>;
  serviceGroup?: string;
  conversationId?: string;
  uploadKey?: string;
  requestSource?: 'HTTP_REQUEST' | 'VUE_DIALOG' | 'VUE_SIDEBAR';
}

export const executeByToolSync = (data: ExecuteRequest) => http.post<ExecutorResponse>('/api/executor/executeByToolNameSync', data);

export const executeByToolAsync = (data: ExecuteRequest) => http.post<ExecutorResponse>('/api/executor/executeByToolNameAsync', data);

export const getExecutionDetails = (planId: string) => http.get<string>(`/api/executor/details/${planId}`);

export const stopTask = (planId: string) => http.post(`/api/executor/stopTask/${planId}`);

export const getTaskStatus = (planId: string) => http.get(`/api/executor/taskStatus/${planId}`);

export const submitUserInput = (planId: string, formData: Record<string, string>) =>
  http.post(`/api/executor/submit-input/${planId}`, formData);
