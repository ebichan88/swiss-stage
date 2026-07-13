import '@testing-library/jest-dom/vitest';
import { afterAll, afterEach, beforeAll } from 'vitest';

import { server } from './msw/server';

// Node の fetch は相対URLを解釈できないため、アプリの `/api/...` 呼び出しをテスト用に絶対URL化する
const originalFetch = globalThis.fetch;
globalThis.fetch = ((input: RequestInfo | URL, init?: RequestInit) => {
  if (typeof input === 'string' && input.startsWith('/')) {
    return originalFetch(new URL(input, 'http://localhost').toString(), init);
  }
  return originalFetch(input, init);
}) as typeof fetch;

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
