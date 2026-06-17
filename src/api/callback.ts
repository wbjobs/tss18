import request from '@/utils/request';
import type { ApiResponse } from '@/types';

export function sendCallback(
  tenantId: number,
  callbackSource: string,
  data: {
    businessKey: string;
    eventType: string;
    data: Record<string, any>;
    timestamp: number;
  }
): Promise<ApiResponse<{ success: boolean; transitionTriggered: boolean; currentState?: string; message?: string }>> {
  return request.post(`/callback/${tenantId}/${callbackSource}`, data).then(res => res.data);
}
