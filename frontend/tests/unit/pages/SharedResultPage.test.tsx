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
  it('結果を選んで確認ダイアログから登録できる(2タップ)', async () => {
    let putBody: unknown = null;
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () => HttpResponse.json(apiSuccess(sharedData(true)))),
      http.put(`/api/v1/shared/${TOKEN}/matches/m1/result`, async ({ request }) => {
        putBody = await request.json();
        return HttpResponse.json(
          apiSuccess(matchOf({ id: 'm1', result: 'PLAYER1_WIN', version: 3 })),
        );
      }),
    );

    renderResultPage();

    await userEvent.click(await screen.findByRole('button', { name: '架空 太郎 の勝ち' }));
    expect(await screen.findByText('「架空 太郎 の勝ち」で登録します。')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '登録する' }));

    expect(await screen.findByText('結果を登録しました')).toBeInTheDocument();
    expect(putBody).toEqual({ result: 'PLAYER1_WIN', version: 2 });
    // 登録後は共有ページ(組み合わせ)へ戻る
    expect(await screen.findByRole('tab', { name: '組み合わせ' })).toBeInTheDocument();
  });

  it('許可されていない場合は入力ボタンを出さず締め切りメッセージを表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () => HttpResponse.json(apiSuccess(sharedData(false)))),
    );

    renderResultPage();

    expect(await screen.findByText(/この対局の結果入力は締め切られています/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '架空 太郎 の勝ち' })).not.toBeInTheDocument();
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

    await userEvent.click(await screen.findByRole('button', { name: '引き分け' }));
    await userEvent.click(screen.getByRole('button', { name: '登録する' }));

    expect(
      await screen.findByText('ほかの端末で更新されました。画面を更新して再度お試しください'),
    ).toBeInTheDocument();
  });
});
