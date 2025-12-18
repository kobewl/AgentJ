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
