import {
  getSystemStatus as requestSystemStatus,
} from '@api/index';
import type { SystemStatus } from '@api/index';
import { zGetSystemStatusResponse } from '@api/zod.gen';

export type { SystemStatus };

export function parseSystemStatus(value: unknown): SystemStatus {
  return zGetSystemStatusResponse.parse(value);
}

export async function getSystemStatus(): Promise<SystemStatus> {
  const result = await requestSystemStatus();

  if (result.error) {
    throw new Error('Backend status request failed', {
      cause: result.error,
    });
  }

  return parseSystemStatus(result.data);
}
