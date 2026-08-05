import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Outlet, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { SettingsPage } from '../../../src/pages/SettingsPage';
import type { Tournament } from '../../../src/types/tournament';
import { tournamentOf } from '../../fixtures';
import { apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

/** SettingsPage は useTournamentContext(Outlet context)を要求するため、親Routeで供給する */
function renderSettings(tournament: Tournament) {
  return renderWithProviders(
    <Routes>
      <Route element={<Outlet context={tournament} />}>
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
    </Routes>,
    { route: '/settings' },
  );
}

/** PATCH /tournaments/:id のリクエストボディを捕捉するハンドラを登録する */
function captureUpdateRequest() {
  const captured: { body?: Record<string, unknown> } = {};
  server.use(
    http.patch('/api/v1/tournaments/:id', async ({ request }) => {
      captured.body = (await request.json()) as Record<string, unknown>;
      return HttpResponse.json(apiSuccess(tournamentOf()));
    }),
  );
  return captured;
}

describe('SettingsPage', () => {
  it('TRN-AC-016: 開催日を空にして保存するとclearEventDate:trueで送信する', async () => {
    const captured = captureUpdateRequest();
    renderSettings(tournamentOf({ id: '01TESTTOURNAMENT0000000001', eventDate: '2026-08-15' }));

    fireEvent.change(screen.getByLabelText('開催日'), { target: { value: '' } });
    await userEvent.click(screen.getByRole('button', { name: '保存する' }));

    await waitFor(() => expect(captured.body?.clearEventDate).toBe(true));
    expect(captured.body?.eventDate).toBeUndefined();
  });

  it('TRN-AC-016: 開催日を変更して保存するとeventDateで送信する', async () => {
    const captured = captureUpdateRequest();
    renderSettings(tournamentOf({ id: '01TESTTOURNAMENT0000000001', eventDate: null }));

    fireEvent.change(screen.getByLabelText('開催日'), { target: { value: '2026-09-01' } });
    await userEvent.click(screen.getByRole('button', { name: '保存する' }));

    await waitFor(() => expect(captured.body?.eventDate).toBe('2026-09-01'));
    expect(captured.body?.clearEventDate).toBeUndefined();
  });

  it('TRN-AC-019: 大会名を空にして保存するとhelperTextのエラーメッセージが表示され、入力欄にaria-describedbyで関連付けられる', async () => {
    renderSettings(tournamentOf({ id: '01TESTTOURNAMENT0000000001' }));

    const nameInput = screen.getByRole('textbox', { name: /大会名/ });
    fireEvent.change(nameInput, { target: { value: '' } });
    await userEvent.click(screen.getByRole('button', { name: '保存する' }));

    const errorMessage = await screen.findByText('大会名は必須です');
    const describedBy = nameInput.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(errorMessage.id).toBe(describedBy);
  });
});
