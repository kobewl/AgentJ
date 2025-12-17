import http from './http';

export interface KnowledgeItem {
  id: string;
  name: string;
  type: 'KNOWLEDGE_BASE' | 'KNOWLEDGE_FILE';
  storagePath?: string;
  knowledgeBaseId?: string;
  originalFilename?: string;
  mimeType?: string;
  fileSize?: number;
  createdAt?: string;
  updatedAt?: string;
}

export async function listKnowledgeBases() {
  return http.get<{ success: boolean; data: KnowledgeItem[] }>('/api/knowledge/bases');
}

export async function createKnowledgeBase(name: string) {
  return http.post<{ success: boolean; data: KnowledgeItem }>('/api/knowledge/bases', { name });
}

export async function listKnowledgeFiles(baseId: string) {
  return http.get<{ success: boolean; data: KnowledgeItem[] }>(`/api/knowledge/bases/${baseId}/files`);
}

export async function uploadKnowledgeFile(baseId: string, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return http.post<{ success: boolean; data: KnowledgeItem }>(`/api/knowledge/bases/${baseId}/files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export async function deleteKnowledgeItem(id: string) {
  return http.delete<{ success: boolean }>(`/api/knowledge/items/${id}`);
}

export async function chatWithKnowledge(baseId: string, question: string) {
  return http.post<{ success: boolean; data: { answer: string } }>(`/api/knowledge/bases/${baseId}/chat`, {
    question,
  });
}
