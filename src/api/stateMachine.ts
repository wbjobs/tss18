import request from '@/utils/request';
import type { ApiResponse, PageResult, StateMachine, StateNode, Transition } from '@/types';

export function getStateMachineList(params: { page: number; size: number }): Promise<ApiResponse<PageResult<StateMachine>>> {
  return request.get('/state-machine', { params }).then(res => res.data);
}

export function getStateMachine(id: number): Promise<ApiResponse<StateMachine>> {
  return request.get(`/state-machine/${id}`).then(res => res.data);
}

export function createStateMachine(data: { name: string; description: string }): Promise<ApiResponse<StateMachine>> {
  return request.post('/state-machine', data).then(res => res.data);
}

export function updateStateMachine(id: number, data: { nodes: StateNode[]; transitions: Transition[] }): Promise<ApiResponse<StateMachine>> {
  return request.put(`/state-machine/${id}`, data).then(res => res.data);
}

export function publishStateMachine(id: number): Promise<ApiResponse<StateMachine>> {
  return request.post(`/state-machine/${id}/publish`).then(res => res.data);
}

export function offlineStateMachine(id: number): Promise<ApiResponse<StateMachine>> {
  return request.post(`/state-machine/${id}/offline`).then(res => res.data);
}

export function deleteStateMachine(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/state-machine/${id}`).then(res => res.data);
}
