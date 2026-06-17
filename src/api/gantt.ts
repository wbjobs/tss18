import request from '@/utils/request'
import type { ApiResponse, GanttItem } from '@/types'

export function getGanttData(ticketId: number): Promise<ApiResponse<GanttItem[]>> {
  return request.get(`/gantt/ticket/${ticketId}`).then(res => res.data)
}
