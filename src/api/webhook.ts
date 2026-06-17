import request from '@/utils/request';
import type { ApiResponse, PageResult, WebhookConfig } from '@/types';

export function getWebhookList(params: { page: number; size: number }): Promise<ApiResponse<PageResult<WebhookConfig>>> {
  return request.get('/webhook', { params }).then(res => res.data);
}

export function getWebhook(id: number): Promise<ApiResponse<WebhookConfig>> {
  return request.get(`/webhook/${id}`).then(res => res.data);
}

export function createWebhook(data: { name: string; url: string; secretKey: string; events: string[]; enabled: boolean; retryCount: number }): Promise<ApiResponse<WebhookConfig>> {
  return request.post('/webhook', data).then(res => res.data);
}

export function updateWebhook(id: number, data: any): Promise<ApiResponse<WebhookConfig>> {
  return request.put(`/webhook/${id}`, data).then(res => res.data);
}

export function deleteWebhook(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/webhook/${id}`).then(res => res.data);
}

export function testWebhook(id: number): Promise<ApiResponse<void>> {
  return request.post(`/webhook/${id}/test`).then(res => res.data);
}

export function getWebhookLogs(id: number, params: { page: number; size: number }): Promise<ApiResponse<PageResult<any>>> {
  return request.get(`/webhook/${id}/logs`, { params }).then(res => res.data);
}
