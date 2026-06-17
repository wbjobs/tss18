import request from '@/utils/request'
import type { ApiResponse, SimulationRequest, SimulationResult } from '@/types'

export function runSimulation(data: SimulationRequest): Promise<ApiResponse<SimulationResult>> {
  return request.post('/simulation', data).then(res => res.data)
}
