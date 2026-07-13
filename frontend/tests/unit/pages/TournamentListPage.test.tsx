import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { TournamentListPage } from '../../../src/pages/TournamentListPage';
import { tournamentOf } from '../../fixtures';
import { apiError, apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

describe('TournamentListPage', () => {
  it('大会一覧を表示する', async () => {
    server.use(
      http.get('/api/v1/tournaments', () =>
        HttpResponse.json(
          apiSuccess([
            tournamentOf({ id: 't1', name: '夏の囲碁大会' }),
            tournamentOf({
              id: 't2',
              name: '秋の将棋大会',
              gameType: 'SHOGI',
              status: 'IN_PROGRESS',
              currentRound: 2,
            }),
          ]),
        ),
      ),
    );

    renderWithProviders(<TournamentListPage />);

    expect(await screen.findByText('夏の囲碁大会')).toBeInTheDocument();
    expect(screen.getByText('秋の将棋大会')).toBeInTheDocument();
    expect(screen.getByText('開催中')).toBeInTheDocument();
  });

  it('0件のときは空状態とアクションを表示する', async () => {
    server.use(http.get('/api/v1/tournaments', () => HttpResponse.json(apiSuccess([]))));

    renderWithProviders(<TournamentListPage />);

    expect(await screen.findByText('大会がまだありません')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '最初の大会を作成する' })).toBeInTheDocument();
  });

  it('取得失敗時は再試行ボタン付きのエラーを表示する', async () => {
    server.use(
      http.get('/api/v1/tournaments', () =>
        HttpResponse.json(apiError('INTERNAL_ERROR', '予期しないエラーが発生しました'), {
          status: 500,
        }),
      ),
    );

    renderWithProviders(<TournamentListPage />);

    expect(await screen.findByText('大会一覧の取得に失敗しました')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '再試行' })).toBeInTheDocument();
  });
});
