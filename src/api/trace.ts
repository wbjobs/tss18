import request from '@/utils/request';
import type { ApiResponse, PageResult, TraceRecord } from '@/types';

export function getTicketTraces(ticketId: number): Promise<ApiResponse<TraceRecord[]>> {
  return request.get(`/trace/ticket/${ticketId}`).then(res => res.data);
}

export function getTraceList(params: { page: number; size: number }): Promise<ApiResponse<PageResult<TraceRecord>>> {
  return request.get('/trace', { params }).then(res => res.data);
}
