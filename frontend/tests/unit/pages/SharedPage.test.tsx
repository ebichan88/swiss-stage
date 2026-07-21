import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { SharedPage } from '../../../src/pages/SharedPage';
import {
  groupOf,
  groupStandingsOf,
  matchOf,
  roundOf,
  sharedSummaryOf,
  sharedTournamentOf,
  standingOf,
  summaryOf,
} from '../../fixtures';
import { apiError, apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

const TOKEN = 'A'.repeat(43);

function renderSharedPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/s/:token" element={<SharedPage />} />
    </Routes>,
    { route: `/s/${TOKEN}` },
  );
}

describe('SharedPage', () => {
  it('大会名・現在ラウンド・組み合わせを表示し、順位表タブに切り替えられる', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({ name: '共有テスト大会', currentRound: 1 }),
              rounds: [
                roundOf({
                  matches: [
                    matchOf({
                      player1: summaryOf({ id: 'p1', name: '架空 太郎' }),
                      player2: summaryOf({ id: 'p2', name: '仮名 花子', organization: null }),
                    }),
                  ],
                }),
              ],
            }),
          ),
        ),
      ),
    );

    renderSharedPage();

    expect(await screen.findByRole('heading', { name: '共有テスト大会' })).toBeInTheDocument();
    expect(screen.getByText('第1ラウンド / 全5ラウンド')).toBeInTheDocument();
    expect(screen.getByText(/架空 太郎/)).toBeInTheDocument();
    // 結果入力が未許可なら入力導線は出ない
    expect(screen.queryByRole('link', { name: '結果入力' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: '順位表' }));
    expect(await screen.findByRole('table')).toBeInTheDocument();
  });

  it('戦績一覧タブに切り替えるとラウンドごとの対戦相手・結果を含む一覧表が表示される', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              rounds: [
                roundOf({
                  roundNumber: 1,
                  matches: [
                    matchOf({
                      player1: summaryOf({ id: 'p1', name: '架空 太郎', entryOrder: 1 }),
                      player2: summaryOf({ id: 'p2', name: '仮名 花子', entryOrder: 2 }),
                      result: 'PLAYER1_WIN',
                    }),
                  ],
                }),
              ],
              standings: [
                groupStandingsOf({
                  standings: [
                    standingOf({
                      rank: 1,
                      participant: summaryOf({ id: 'p1', name: '架空 太郎', entryOrder: 1 }),
                    }),
                    standingOf({
                      rank: 2,
                      participant: summaryOf({ id: 'p2', name: '仮名 花子', entryOrder: 2 }),
                    }),
                  ],
                }),
              ],
            }),
          ),
        ),
      ),
    );

    renderSharedPage();

    await screen.findByRole('heading', { name: '第1回テスト囲碁大会' });
    await userEvent.click(screen.getByRole('tab', { name: '戦績一覧' }));

    expect(await screen.findByRole('columnheader', { name: '第1ラウンド' })).toBeInTheDocument();
    const rows = screen.getAllByRole('row');
    expect(rows[2]).toHaveTextContent('架空 太郎');
    expect(rows[2]).toHaveTextContent('○');
  });

  it('グループ大会の順位表はグループごとに見出し付きで表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              standings: [
                groupStandingsOf({
                  group: groupOf({ id: 'g1', name: 'A' }),
                  standings: [standingOf({ participant: summaryOf({ name: '架空 太郎' }) })],
                }),
                groupStandingsOf({
                  group: groupOf({ id: 'g2', name: 'B' }),
                  standings: [
                    standingOf({ participant: summaryOf({ id: 'p2', name: '仮名 花子' }) }),
                  ],
                }),
              ],
            }),
          ),
        ),
      ),
    );

    renderSharedPage();

    await screen.findByRole('heading', { name: '第1回テスト囲碁大会' });
    await userEvent.click(screen.getByRole('tab', { name: '順位表' }));

    expect(await screen.findByRole('heading', { name: 'A' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'B' })).toBeInTheDocument();
    expect(screen.getAllByRole('table')).toHaveLength(2);
    expect(screen.getByText(/仮名 花子/)).toBeInTheDocument();
  });

  it('結果入力が許可されていれば入力ページへのリンクを表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({ resultInputEnabled: true }),
              rounds: [roundOf({ matches: [matchOf({ id: 'm1' })] })],
            }),
          ),
        ),
      ),
    );

    renderSharedPage();

    const link = await screen.findByRole('link', { name: '結果入力' });
    expect(link).toHaveAttribute('href', `/s/${TOKEN}/matches/m1`);
  });

  it('SHR-AC-015: 確定済みの結果と自己申告が食い違う対局は「XXの勝ち」のままにせず不一致を案内する', async () => {
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
                      player1: summaryOf({ id: 'p1', name: '架空 太郎' }),
                      player2: summaryOf({ id: 'p2', name: '仮名 花子', organization: null }),
                      result: 'PLAYER1_WIN',
                      player1ReportedResult: 'PLAYER2_WIN',
                      player2ReportedResult: 'PLAYER2_WIN',
                    }),
                  ],
                }),
              ],
            }),
          ),
        ),
      ),
    );

    renderSharedPage();

    expect(
      await screen.findByText(
        '架空 太郎 の勝ち(申告が一致しません・結果入力から内容を確認できます)',
      ),
    ).toBeInTheDocument();
  });

  it('SHR-AC-015: ラウンド確定後は結果入力ボタンが出ないため、食い違いの案内文に結果入力への言及を付けない', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({ resultInputEnabled: true }),
              rounds: [
                roundOf({
                  status: 'CONFIRMED',
                  matches: [
                    matchOf({
                      id: 'm1',
                      player1: summaryOf({ id: 'p1', name: '架空 太郎' }),
                      player2: summaryOf({ id: 'p2', name: '仮名 花子', organization: null }),
                      result: 'PLAYER1_WIN',
                      player1ReportedResult: 'PLAYER2_WIN',
                      player2ReportedResult: 'PLAYER2_WIN',
                    }),
                  ],
                }),
              ],
            }),
          ),
        ),
      ),
    );

    renderSharedPage();

    expect(await screen.findByText('架空 太郎 の勝ち(申告が一致しません)')).toBeInTheDocument();
    expect(screen.queryByText(/結果入力から内容を確認できます/)).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '結果入力' })).not.toBeInTheDocument();
  });

  it('無効トークンは専用メッセージを表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiError('INVALID_SHARE_TOKEN', 'このURLは無効になっています。運営者に確認してください'),
          { status: 403 },
        ),
      ),
    );

    renderSharedPage();

    expect(await screen.findByRole('heading', { name: 'このURLは無効です' })).toBeInTheDocument();
  });
});
