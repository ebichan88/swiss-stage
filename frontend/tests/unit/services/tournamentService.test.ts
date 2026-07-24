import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { ApiError } from '../../../src/services/apiClient';
import {
  createTournament,
  fetchTournaments,
  updateTournament,
} from '../../../src/services/tournamentService';
import { tournamentOf } from '../../fixtures';
import { apiError, apiSuccess, server } from '../../msw/server';

describe('tournamentService', () => {
  it('一覧取得は統一レスポンスの data を返す', async () => {
    const tournament = tournamentOf();
    server.use(http.get('/api/v1/tournaments', () => HttpResponse.json(apiSuccess([tournament]))));

    await expect(fetchTournaments()).resolves.toEqual([tournament]);
  });

  it('作成はリクエストボディをJSONで送る', async () => {
    let requestBody: unknown;
    server.use(
      http.post('/api/v1/tournaments', async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(apiSuccess(tournamentOf()), { status: 201 });
      }),
    );

    await createTournament({
      name: 'テスト大会',
      gameType: 'SHOGI',
      competitionType: 'INDIVIDUAL',
      teamSize: null,
      totalRounds: 4,
    });

    expect(requestBody).toEqual({
      name: 'テスト大会',
      gameType: 'SHOGI',
      competitionType: 'INDIVIDUAL',
      teamSize: null,
      totalRounds: 4,
    });
  });

  it('エラーレスポンスは code と message を持つ ApiError になる', async () => {
    server.use(
      http.patch('/api/v1/tournaments/x', () =>
        HttpResponse.json(apiError('CONFLICT', 'ほかの端末で更新されました'), { status: 409 }),
      ),
    );

    const promise = updateTournament('x', { name: 'x', version: 1 });
    await expect(promise).rejects.toBeInstanceOf(ApiError);
    await expect(promise).rejects.toMatchObject({
      code: 'CONFLICT',
      message: 'ほかの端末で更新されました',
    });
  });

  it('ネットワーク断は NETWORK_ERROR に正規化される', async () => {
    server.use(http.get('/api/v1/tournaments', () => HttpResponse.error()));

    await expect(fetchTournaments()).rejects.toMatchObject({ code: 'NETWORK_ERROR' });
  });
});
