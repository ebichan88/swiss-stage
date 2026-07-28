import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Outlet, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { PrintRosterPage } from '../../../src/pages/PrintRosterPage';
import type { Tournament } from '../../../src/types/tournament';
import { groupOf, participantOf, tournamentOf } from '../../fixtures';
import { apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

/** PrintRosterPage は useTournamentContext(Outlet context)を要求するため、親Routeで供給する */
function renderRosterPage(tournament: Tournament) {
  server.use(
    http.get('/api/v1/tournaments/:id/participants', () =>
      HttpResponse.json(apiSuccess([participantOf({ name: '架空 太郎' })])),
    ),
    http.get('/api/v1/tournaments/:id/groups', () => HttpResponse.json(apiSuccess([groupOf()]))),
  );
  return renderWithProviders(
    <Routes>
      <Route element={<Outlet context={tournament} />}>
        <Route path="/print/roster" element={<PrintRosterPage />} />
      </Route>
    </Routes>,
    { route: '/print/roster' },
  );
}

describe('PrintRosterPage', () => {
  it('PRT-AC-015: 大会が終了(FINISHED)していても名簿を印刷できる(状態を問わず利用できる)', async () => {
    renderRosterPage(tournamentOf({ status: 'FINISHED' }));
    expect(await screen.findByText('架空 太郎')).toBeInTheDocument();
  });

  it('PRT-AC-015: 準備中(PREPARING)の大会でも名簿を印刷できる', async () => {
    renderRosterPage(tournamentOf({ status: 'PREPARING' }));
    expect(await screen.findByText('架空 太郎')).toBeInTheDocument();
  });
});
