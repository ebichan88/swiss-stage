import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { SharedPage } from '../../../src/pages/SharedPage';
import { SharedResultPage } from '../../../src/pages/SharedResultPage';
import { matchOf, roundOf, sharedSummaryOf, sharedTournamentOf, summaryOf } from '../../fixtures';
import { apiError, apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

const TOKEN = 'B'.repeat(43);

const sharedData = (resultInputEnabled: boolean) =>
  sharedTournamentOf({
    tournament: sharedSummaryOf({ resultInputEnabled }),
    rounds: [
      roundOf({
        matches: [
          matchOf({
            id: 'm1',
            version: 2,
            player1: summaryOf({ id: 'p1', name: '架空 太郎' }),
            player2: summaryOf({ id: 'p2', name: '仮名 花子', organization: null }),
          }),
        ],
      }),
    ],
  });

function renderResultPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/s/:token" element={<SharedPage />} />
      <Route path="/s/:token/matches/:mid" element={<SharedResultPage />} />
    </Routes>,
    { route: `/s/${TOKEN}/matches/m1` },
  );
}

describe('SharedResultPage', () => {
  it('自分がどちらかを選んでから結果を選び、確認ダイアログから申告できる', async () => {
    let putBody: unknown = null;
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () => HttpResponse.json(apiSuccess(sharedData(true)))),
      http.put(`/api/v1/shared/${TOKEN}/matches/m1/result`, async ({ request }) => {
        putBody = await request.json();
        return HttpResponse.json(
          apiSuccess(
            matchOf({ id: 'm1', result: 'NONE', player1ReportedResult: 'PLAYER1_WIN', version: 3 }),
          ),
        );
      }),
    );

    renderResultPage();

    await userEvent.click(await screen.findByRole('button', { name: '架空 太郎' }));
    await userEvent.click(await screen.findByRole('button', { name: '勝ち' }));
    expect(
      await screen.findByText('「勝ち」で申告します。相手の申告と一致すると結果が確定します。'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '申告する' }));

    expect(await screen.findByText('申告を送信しました')).toBeInTheDocument();
    expect(putBody).toEqual({ reportedBy: 'PLAYER1', result: 'PLAYER1_WIN', version: 2 });
    // 送信後は同じ画面(申告状態選択)に留まる
    expect(await screen.findByText('あなたはどちらですか?')).toBeInTheDocument();
  });

  it('許可されていない場合は選択ボタンを出さず締め切りメッセージを表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () => HttpResponse.json(apiSuccess(sharedData(false)))),
    );

    renderResultPage();

    expect(await screen.findByText(/この対局の結果入力は締め切られています/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '架空 太郎' })).not.toBeInTheDocument();
  });

  it('競合(409)はエラーメッセージを表示して画面に留まる', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () => HttpResponse.json(apiSuccess(sharedData(true)))),
      http.put(`/api/v1/shared/${TOKEN}/matches/m1/result`, () =>
        HttpResponse.json(
          apiError('CONFLICT', 'ほかの端末で更新されました。画面を更新して再度お試しください'),
          { status: 409 },
        ),
      ),
    );

    renderResultPage();

    await userEvent.click(await screen.findByRole('button', { name: '仮名 花子' }));
    await userEvent.click(await screen.findByRole('button', { name: '引き分け' }));
    await userEvent.click(screen.getByRole('button', { name: '申告する' }));

    expect(
      await screen.findByText('ほかの端末で更新されました。画面を更新して再度お試しください'),
    ).toBeInTheDocument();
  });

  it('片方のみ申告済みは相手の申告待ちである旨を表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({ resultInputEnabled: true }),
              rounds: [
                roundOf({
                  matches: [
                    matchOf({
                      id: 'm1',
                      version: 3,
                      player1: summaryOf({ id: 'p1', name: '架空 太郎' }),
                      player2: summaryOf({ id: 'p2', name: '仮名 花子', organization: null }),
                      player1ReportedResult: 'PLAYER1_WIN',
                    }),
                  ],
                }),
              ],
            }),
          ),
        ),
      ),
    );

    renderResultPage();

    expect(await screen.findByText(/架空 太郎が申告しました/)).toBeInTheDocument();
  });
});
