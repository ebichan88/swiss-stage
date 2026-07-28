import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { TournamentCreatePage } from '../../../src/pages/TournamentCreatePage';
import { tournamentOf } from '../../fixtures';
import { apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

/** POST /tournaments のリクエストボディを捕捉するハンドラを登録する */
function captureCreateRequest() {
  const captured: { body?: Record<string, unknown> } = {};
  server.use(
    http.post('/api/v1/tournaments', async ({ request }) => {
      captured.body = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json(apiSuccess(tournamentOf({ id: '01TESTTOURNAMENT0000000001' })), {
        status: 201,
      });
    }),
  );
  return captured;
}

describe('TournamentCreatePage', () => {
  it('TRN-AC-016: 開催日を入力すると作成リクエストにeventDateが含まれる', async () => {
    const captured = captureCreateRequest();
    renderWithProviders(<TournamentCreatePage />, { route: '/tournaments/new' });

    await userEvent.type(screen.getByRole('textbox', { name: '大会名' }), 'テスト大会');
    fireEvent.change(screen.getByLabelText('開催日'), { target: { value: '2026-08-15' } });
    await userEvent.click(screen.getByRole('button', { name: '作成する' }));

    await waitFor(() => expect(captured.body?.eventDate).toBe('2026-08-15'));
  });

  it('TRN-AC-016: 開催日を空のままにすると作成リクエストのeventDateはnullになる', async () => {
    const captured = captureCreateRequest();
    renderWithProviders(<TournamentCreatePage />, { route: '/tournaments/new' });

    await userEvent.type(screen.getByRole('textbox', { name: '大会名' }), 'テスト大会');
    await userEvent.click(screen.getByRole('button', { name: '作成する' }));

    await waitFor(() => expect(captured.body?.eventDate).toBeNull());
  });
});
