import http from './http';
import type { ConfigEntity, ModelOption } from './types';

export const getConfigsByGroup = (groupName: string) =>
  http.get<ConfigEntity[]>(`/api/config/group/${encodeURIComponent(groupName)}`);

export const batchUpdateConfigs = (configs: ConfigEntity[]) => http.post('/api/config/batch-update', configs);

export const resetAllDefaults = () => http.post('/api/config/reset-all-defaults', {});

export const getAvailableModels = () => http.get<{ options: ModelOption[]; total: number }>('/api/config/available-models');
