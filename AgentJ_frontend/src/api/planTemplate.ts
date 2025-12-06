import http from './http';
import type { PlanTemplateConfigVO, PlanVersionResponse } from './types';

export const savePlan = (planJson: string) => http.post('/api/plan-template/save', { planJson });

export const listPlanTemplates = () => http.get<{ templates: any[]; count: number }>('/api/plan-template/list');

export const listPlanTemplateConfigs = () => http.get<PlanTemplateConfigVO[]>('/api/plan-template/list-config');

export const getPlanVersions = (planId: string) => http.post<PlanVersionResponse>('/api/plan-template/versions', { planId });

export const getPlanVersion = (planId: string, versionIndex: number) =>
  http.post('/api/plan-template/get-version', { planId, versionIndex });

export const getPlanTemplateConfig = (planTemplateId: string) =>
  http.get<PlanTemplateConfigVO>(`/api/plan-template/${planTemplateId}/config`);

export const deletePlanTemplate = (planId: string) => http.post('/api/plan-template/delete', { planId });

export const getParameterRequirements = (planTemplateId: string) =>
  http.get<{ parameters: string[]; hasParameters: boolean; requirements: any }>(
    `/api/plan-template/${planTemplateId}/parameters`,
  );

export const createOrUpdatePlanTemplateWithTool = (config: any) =>
  http.post('/api/plan-template/create-or-update-with-tool', config);
