import http from './http';

/**
 * 代码生成应用接口
 */

export interface CreateAppRequest {
	appName: string;
	initPrompt: string;
	codeGenType?: string;
	cover?: string;
}

export interface UpdateAppRequest {
	appName?: string;
	cover?: string;
}

export interface CodeGenRequest {
	appId: number;
	message: string;
	elementInfo?: ElementInfo;
}

export interface ElementInfo {
	tagName?: string;
	id?: string;
	className?: string;
	textContent?: string;
	selector?: string;
	pagePath?: string;
	rect?: {
		top?: number;
		left?: number;
		width?: number;
		height?: number;
	};
}

export interface AppVO {
	id: number;
	appName: string;
	cover?: string;
	initPrompt: string;
	codeGenType: string;
	deployKey?: string;
	deployedTime?: string;
	userId: number;
	createdAt: string;
	updatedAt: string;
	previewUrl?: string;
}

export interface ChatMessageVO {
	id: number;
	message: string;
	messageType: 'user' | 'ai';
	appId: number;
	userId: number;
	createdAt: string;
}

/**
 * 创建应用
 */
export function createApp(data: CreateAppRequest) {
	return http.post<AppVO>('/api/codegen/app', data);
}

/**
 * 获取应用详情
 */
export function getApp(id: number) {
	return http.get<AppVO>(`/api/codegen/app/${id}`);
}

/**
 * 获取用户的应用列表
 */
export function getUserApps() {
	return http.get<AppVO[]>('/api/codegen/app');
}

/**
 * 更新应用
 */
export function updateApp(id: number, data: UpdateAppRequest) {
	return http.put<AppVO>(`/api/codegen/app/${id}`, data);
}

/**
 * 删除应用
 */
export function deleteApp(id: number) {
	return http.delete(`/api/codegen/app/${id}`);
}

/**
 * 部署应用
 */
export function deployApp(id: number) {
	return http.post<AppVO>(`/api/codegen/app/${id}/deploy`);
}

/**
 * 获取已生成的代码
 */
export function getGeneratedCode(appId: number) {
	return http.get<string>(`/api/codegen/code/${appId}`);
}

/**
 * 获取对话历史
 */
export function getChatHistory(appId: number) {
	return http.get<ChatMessageVO[]>(`/api/codegen/chat/history/${appId}`);
}

/**
 * 获取预览 URL
 */
export function getPreviewUrl(deployKey: string): string {
	return `/static/html/${deployKey}/index.html`;
}

/**
 * 获取下载 URL
 */
export function getDownloadUrl(appId: number): string {
	return `/api/codegen/app/${appId}/download`;
}
