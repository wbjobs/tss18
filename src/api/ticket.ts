import request from '@/utils/request';
import type { ApiResponse, PageResult, Ticket } from '@/types';

export function getTicketList(params: { page: number; size: number; state?: string }): Promise<ApiResponse<PageResult<Ticket>>> {
  return request.get('/ticket', { params }).then(res => res.data);
}

export function getTicket(id: number): Promise<ApiResponse<Ticket>> {
  return request.get(`/ticket/${id}`).then(res => res.data);
}

export function createTicket(data: { stateMachineId: number; title: string; businessKey: string; payload: Record<string, any> }): Promise<ApiResponse<Ticket>> {
  return request.post('/ticket', data).then(res => res.data);
}

export function executeTransition(id: number, data: { targetStateId: string; remark?: string; triggerData?: Record<string, any> }): Promise<ApiResponse<{ success: boolean; currentStateId: string; message: string }>> {
  return request.post(`/ticket/${id}/transition`, data).then(res => res.data);
}
