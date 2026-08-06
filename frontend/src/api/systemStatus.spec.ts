import { describe, expect, it } from 'vitest';

import { parseSystemStatus } from './systemStatus';

describe('parseSystemStatus', () => {
  it('accepts the backend status contract', () => {
    expect(parseSystemStatus({ status: 'UP' })).toEqual({ status: 'UP' });
  });

  it('rejects an unknown status', () => {
    expect(() => parseSystemStatus({ status: 'DOWN' })).toThrow();
  });
});
