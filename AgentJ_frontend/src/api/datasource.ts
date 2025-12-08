import http from './http';
import type { DatasourceConfig } from './types';

export const listDatasourceConfigs = () => http.get<DatasourceConfig[]>('/api/datasource-configs');

export const getDatasourceConfig = (id: number) => http.get<DatasourceConfig>(`/api/datasource-configs/${id}`);

export const createDatasourceConfig = (data: DatasourceConfig) => http.post('/api/datasource-configs', data);

export const updateDatasourceConfig = (id: number, data: DatasourceConfig) =>
  http.put(`/api/datasource-configs/${id}`, data);

export const deleteDatasourceConfig = (id: number) => http.delete(`/api/datasource-configs/${id}`);

export const testDatasourceConnection = (data: DatasourceConfig) => http.post('/api/datasource-configs/test-connection', data);

