import http from './http';
import type { CronConfig } from './types';

export const listCronTasks = () => http.get<CronConfig[]>('/api/cron-tasks');

export const getCronTask = (id: string) => http.get<CronConfig>(`/api/cron-tasks/${id}`);

export const createCronTask = (data: CronConfig) => http.post('/api/cron-tasks', data);

export const updateCronTask = (id: number | string, data: CronConfig) => http.put(`/api/cron-tasks/${id}`, data);

export const updateCronStatus = (id: string, status: number) => http.put(`/api/cron-tasks/${id}/status`, null, { params: { status } });

export const executeCronTask = (id: string) => http.post(`/api/cron-tasks/${id}/execute`);

export const deleteCronTask = (id: string) => http.delete(`/api/cron-tasks/${id}`);

