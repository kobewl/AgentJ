import http from './http';
import type { InitStatus } from './types';

export interface InitConfigRequest {
  configMode?: 'dashscope' | 'custom';
  apiKey: string;
  baseUrl?: string;
  modelName?: string;
  modelDisplayName?: string;
  completionsPath?: string;
}

export const getInitStatus = () => http.get<InitStatus>('/api/init/status');

export const saveInitConfig = (data: InitConfigRequest) => http.post('/api/init/save', data);
