import http from './http';

export interface WorkflowNode {
  id: string;
  type: string;
  position: { x: number; y: number };
  data: Record<string, any>;
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  sourceHandle?: string;
  targetHandle?: string;
  label?: string;
  type?: string;
}

export interface WorkflowViewport {
  x: number;
  y: number;
  zoom: number;
}

export interface Workflow {
  id?: number;
  name: string;
  description?: string;
  status?: string;
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  viewport?: WorkflowViewport;
}

export interface WorkflowExecuteRequest {
  inputs: Record<string, any>;
  stream?: boolean;
  threadId?: string;
  storeId?: string;
  checkpointId?: string;
}

export interface NodeType {
  type: string;
  label: string;
  description: string;
  icon: string;
  color: string;
  configSchema?: Record<string, any>;
}

// ============ CRUD Operations ============

export async function getWorkflows(): Promise<Workflow[]> {
  const response = await http.get('/api/workflow');
  return response.data;
}

export async function getWorkflow(id: number): Promise<Workflow> {
  const response = await http.get(`/api/workflow/${id}`);
  return response.data;
}

export async function createWorkflow(workflow: Workflow): Promise<Workflow> {
  const response = await http.post('/api/workflow', workflow);
  return response.data;
}

export async function updateWorkflow(id: number, workflow: Workflow): Promise<Workflow> {
  const response = await http.put(`/api/workflow/${id}`, workflow);
  return response.data;
}

export async function deleteWorkflow(id: number): Promise<void> {
  await http.delete(`/api/workflow/${id}`);
}

export async function publishWorkflow(id: number): Promise<Workflow> {
  const response = await http.post(`/api/workflow/${id}/publish`);
  return response.data;
}

// ============ Execution Operations ============

export async function executeWorkflow(id: number, request: WorkflowExecuteRequest): Promise<Record<string, any>> {
  const response = await http.post(`/api/workflow/${id}/execute`, request);
  return response.data;
}

export async function getExecutionHistory(id: number): Promise<any[]> {
  const response = await http.get(`/api/workflow/${id}/executions`);
  return response.data;
}

// ============ Node Types ============

export async function getNodeTypes(): Promise<NodeType[]> {
  const response = await http.get('/api/workflow/node-types');
  return response.data;
}

// ============ Model Operations ============

export interface ModelInfo {
  id: number;
  modelName: string;
  description: string;
  isDefault: boolean;
  defaultTemperature?: number;
  defaultTopP?: number;
}

export async function getAvailableModels(): Promise<ModelInfo[]> {
  const response = await http.get('/api/workflow/models');
  return response.data;
}

// ============ Streaming Execution ============

export async function executeWorkflowStream(id: number, request: WorkflowExecuteRequest) {
  // Note: EventSource only supports GET requests.
  // For POST requests with body, use fetch with ReadableStream instead.
  const baseUrl = http.defaults.baseURL || '';
  const params = new URLSearchParams();
  if (request.threadId) params.append('threadId', request.threadId);
  if (request.storeId) params.append('storeId', request.storeId);
  if (request.checkpointId) params.append('checkpointId', request.checkpointId);

  // Pass inputs as query params for SSE (EventSource limitation)
  if (request.inputs) {
    Object.entries(request.inputs).forEach(([key, value]) => {
      params.append(key, String(value));
    });
  }

  const queryString = params.toString();
  const url = `${baseUrl}/api/workflow/${id}/execute/stream${queryString ? '?' + queryString : ''}`;
  return new EventSource(url);
}

// ============ State History & Time Travel ============

export interface StateSnapshot {
  threadId: string;
  checkpointId?: string;
  state: Record<string, any>;
  createdAt: string;
}

export async function getState(threadId: string): Promise<StateSnapshot | null> {
  const response = await http.get(`/api/workflow/state/${threadId}`);
  return response.data;
}

export async function getStateHistory(threadId: string): Promise<StateSnapshot[]> {
  const response = await http.get(`/api/workflow/state/${threadId}/history`);
  return response.data;
}

export async function replayWorkflow(
  id: number,
  threadId: string,
  checkpointId?: string,
  inputs?: Record<string, any>
): Promise<Record<string, any>> {
  const params = new URLSearchParams({ threadId });
  if (checkpointId) params.append('checkpointId', checkpointId);
  const response = await http.post(`/api/workflow/${id}/replay?${params}`, inputs || {});
  return response.data;
}

export async function updateState(
  workflowId: number,
  threadId: string,
  updates: Record<string, any>,
  asNode?: string
): Promise<{ message: string; threadId: string }> {
  const params = new URLSearchParams();
  if (asNode) params.append('asNode', asNode);
  const queryString = params.toString();
  const response = await http.post(
    `/api/workflow/${workflowId}/state/${threadId}/update${queryString ? '?' + queryString : ''}`,
    updates
  );
  return response.data;
}

// ============ Human-in-the-Loop ============

export async function pauseExecution(
  threadId: string,
  checkpointId?: string
): Promise<{ message: string; threadId: string }> {
  const params = checkpointId ? `?checkpointId=${checkpointId}` : '';
  const response = await http.post(`/api/workflow/execution/${threadId}/pause${params}`);
  return response.data;
}

export async function resumeExecution(
  threadId: string,
  userInputs: Record<string, any>
): Promise<{ message: string; threadId: string }> {
  const response = await http.post(`/api/workflow/execution/${threadId}/resume`, userInputs);
  return response.data;
}

// ============ Long-term Memory (Store) ============

export async function getStoreData(namespace: string, key: string): Promise<Record<string, any> | null> {
  const response = await http.get(`/api/workflow/store/${namespace}/${key}`);
  return response.data;
}

export async function putStoreData(
  namespace: string,
  key: string,
  data: any
): Promise<{ message: string; namespace: string; key: string }> {
  const response = await http.post(`/api/workflow/store/${namespace}/${key}`, { value: data });
  return response.data;
}

export async function deleteStoreData(
  namespace: string,
  key: string
): Promise<{ message: string }> {
  const response = await http.delete(`/api/workflow/store/${namespace}/${key}`);
  return response.data;
}

// ============ User Preferences ============

export async function getUserPreferences(userId: string): Promise<Record<string, any>> {
  const response = await http.get(`/api/workflow/user/${userId}/preferences`);
  return response.data;
}

export async function saveUserPreferences(
  userId: string,
  preferences: Record<string, any>
): Promise<{ message: string; userId: string }> {
  const response = await http.post(`/api/workflow/user/${userId}/preferences`, preferences);
  return response.data;
}

export async function getUserProfile(userId: string): Promise<Record<string, any>> {
  const response = await http.get(`/api/workflow/user/${userId}/profile`);
  return response.data;
}

export async function saveUserProfile(
  userId: string,
  profile: Record<string, any>
): Promise<{ message: string; userId: string }> {
  const response = await http.post(`/api/workflow/user/${userId}/profile`, profile);
  return response.data;
}

// ============ Cache Management ============

export interface CacheStats {
  size: number;
  maxSize: number;
  hitRate: number;
  evictionCount: number;
}

export async function getCacheStats(): Promise<CacheStats> {
  const response = await http.get('/api/workflow/cache/stats');
  return response.data;
}

export async function clearCache(): Promise<{ message: string }> {
  const response = await http.post('/api/workflow/cache/clear');
  return response.data;
}

export async function invalidateCache(id: number): Promise<{ message: string }> {
  const response = await http.post(`/api/workflow/cache/${id}/invalidate`);
  return response.data;
}

// ============ Execution History ============

export async function getExecutionsByThreadId(threadId: string): Promise<any[]> {
  const response = await http.get(`/api/workflow/execution/thread/${threadId}`);
  return response.data;
}
