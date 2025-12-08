export interface ConfigEntity {
  id?: number;
  configGroup: string;
  configSubGroup: string;
  configKey: string;
  configPath: string;
  configValue?: string;
  defaultValue?: string;
  description?: string;
  inputType?: string;
  optionsJson?: string;
  updateTime?: string;
  createTime?: string;
}

export interface ModelOption {
  value: string;
  label: string;
}

export interface InitStatus {
  initialized: boolean;
  hasConfiguredModels: boolean;
  success: boolean;
  error?: string;
}

export interface CronConfig {
  id?: number | string;
  name?: string;
  cronExpression?: string;
  description?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface DatasourceConfig {
  id?: number;
  name: string;
  url: string;
  username?: string;
  password?: string;
  driverClassName?: string;
  enabled?: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface StepConfig {
  stepRequirement?: string;
  agentName?: string;
  modelName?: string;
  terminateColumns?: string[];
  selectedToolKeys?: string[];
}

export interface ToolConfig {
  toolName?: string;
  httpEnabled?: boolean;
  toolDescription?: string;
  serviceGroup?: string;
}

export interface PlanTemplateConfigVO {
  planTemplateId: string;
  title?: string;
  planType?: string;
  serviceGroup?: string;
  directResponse?: boolean;
  accessLevel?: string;
  steps?: StepConfig[];
  toolConfig?: ToolConfig;
  createTime?: string;
  updateTime?: string;
}

export interface PlanVersionResponse {
  planId: string;
  versionCount: number;
  versions: string[];
}

export interface ExecutorResponse {
  status?: string;
  planId?: string;
  result?: string;
  error?: string;
  message?: string;
  conversationId?: string;
}

export interface MemoryItem {
  conversationId: string;
  name?: string;
  rootPlanIds?: string[];
  createTime?: string;
}

export interface UserPersonalMemoryItem {
  id?: number;
  userId: number;
  memoryKey: string;
  title?: string;
  contentJson: string;
  source?: 'AI' | 'MANUAL';
  confidence?: number;
  importance?: number;
  tags?: string;
  lastUsedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MemoryResponse<T = unknown> {
  success: boolean;
  message?: string;
  data?: T;
}

