import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import App from '../../src/App';
import { apiError, server } from '../msw/server';

describe('App', () => {
  it('トップページが表示される', async () => {
    server.use(
      http.get('/api/v1/auth/me', () =>
        HttpResponse.json(apiError('UNAUTHORIZED', 'ログインしてください'), { status: 401 }),
      ),
    );
    // TopPageがuseAuth()(useQuery)を呼ぶため、本番のmain.tsxと同じくQueryClientProviderが必要
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>,
    );
    // ルートはコード分割(lazy)のため初回表示は非同期
    expect(
      await screen.findByRole('heading', { name: 'Swiss Stage' }, { timeout: 10_000 }),
    ).toBeInTheDocument();
  });
});
