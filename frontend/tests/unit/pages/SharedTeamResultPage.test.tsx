import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { SharedPage } from '../../../src/pages/SharedPage';
import { SharedTeamResultPage } from '../../../src/pages/SharedTeamResultPage';
import {
  boardResultOf,
  sharedSummaryOf,
  sharedTournamentOf,
  teamMatchOf,
  teamRoundOf,
  teamSummaryOf,
} from '../../fixtures';
import { apiError, apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

const TOKEN = 'D'.repeat(43);

const team1 = teamSummaryOf({ id: 't1', name: 'Aチーム' });
const team2 = teamSummaryOf({ id: 't2', name: 'Bチーム' });

const sharedData = (resultInputEnabled: boolean) =>
  sharedTournamentOf({
    tournament: sharedSummaryOf({ competitionType: 'TEAM', teamSize: 3, resultInputEnabled }),
    rounds: null,
    standings: null,
    teamRounds: [
      teamRoundOf({
        matches: [
          teamMatchOf({
            id: 'tm1',
            version: 2,
            team1,
            team2,
            boardResults: [
              boardResultOf({ boardPosition: 1 }),
              boardResultOf({ boardPosition: 2 }),
              boardResultOf({ boardPosition: 3 }),
            ],
          }),
        ],
      }),
    ],
    teamStandings: [],
  });

function renderResultPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/s/:token" element={<SharedPage />} />
      <Route path="/s/:token/team-matches/:mid" element={<SharedTeamResultPage />} />
    </Routes>,
    { route: `/s/${TOKEN}/team-matches/tm1` },
  );
}

describe('SharedTeamResultPage', () => {
  it('チームを選び全ボードの結果を選択して確認ダイアログから申告できる', async () => {
    let putBody: unknown = null;
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () => HttpResponse.json(apiSuccess(sharedData(true)))),
      http.put(`/api/v1/shared/${TOKEN}/team-matches/tm1/result`, async ({ request }) => {
        putBody = await request.json();
        return HttpResponse.json(
          apiSuccess(
            teamMatchOf({
              id: 'tm1',
              team1,
              team2,
              boardResults: [
                boardResultOf({ boardPosition: 1, team1ReportedResult: 'PLAYER1_WIN' }),
                boardResultOf({ boardPosition: 2, team1ReportedResult: 'PLAYER1_WIN' }),
                boardResultOf({ boardPosition: 3, team1ReportedResult: 'PLAYER2_WIN' }),
              ],
              version: 3,
            }),
          ),
        );
      }),
    );

    renderResultPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Aチーム' }));

    expect(screen.getByRole('button', { name: '確認して申告する' })).toBeDisabled();

    await userEvent.click(screen.getByLabelText('主将'));
    await userEvent.click(await screen.findByRole('option', { name: '勝ち' }));
    await userEvent.click(screen.getByLabelText('副将'));
    await userEvent.click(await screen.findByRole('option', { name: '勝ち' }));
    await userEvent.click(screen.getByLabelText('三将'));
    await userEvent.click(await screen.findByRole('option', { name: '負け' }));

    expect(screen.getByRole('button', { name: '確認して申告する' })).toBeEnabled();
    await userEvent.click(screen.getByRole('button', { name: '確認して申告する' }));
    await userEvent.click(screen.getByRole('button', { name: '申告する' }));

    expect(await screen.findByText('申告を送信しました')).toBeInTheDocument();
    expect(putBody).toEqual({
      reportedBy: 'PLAYER1',
      boardResults: ['PLAYER1_WIN', 'PLAYER1_WIN', 'PLAYER2_WIN'],
      version: 2,
    });
    expect(await screen.findByRole('tab', { name: '組み合わせ' })).toBeInTheDocument();
  });

  it('許可されていない場合はチーム選択ボタンを出さず締め切りメッセージを表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () => HttpResponse.json(apiSuccess(sharedData(false)))),
    );

    renderResultPage();

    expect(await screen.findByText(/この対局の結果入力は締め切られています/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Aチーム' })).not.toBeInTheDocument();
  });

  it('競合(409)はエラーメッセージを表示して画面に留まる', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () => HttpResponse.json(apiSuccess(sharedData(true)))),
      http.put(`/api/v1/shared/${TOKEN}/team-matches/tm1/result`, () =>
        HttpResponse.json(
          apiError('CONFLICT', 'ほかの端末で更新されました。画面を更新して再度お試しください'),
          { status: 409 },
        ),
      ),
    );

    renderResultPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Bチーム' }));
    for (const label of ['主将', '副将', '三将']) {
      await userEvent.click(screen.getByLabelText(label));
      await userEvent.click(await screen.findByRole('option', { name: '両者負け' }));
    }
    await userEvent.click(screen.getByRole('button', { name: '確認して申告する' }));
    await userEvent.click(screen.getByRole('button', { name: '申告する' }));

    expect(
      await screen.findByText('ほかの端末で更新されました。画面を更新して再度お試しください'),
    ).toBeInTheDocument();
  });

  it('片方のみ申告済みのボードがある場合は申告待ちの状態を表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({
                competitionType: 'TEAM',
                teamSize: 3,
                resultInputEnabled: true,
              }),
              rounds: null,
              standings: null,
              teamRounds: [
                teamRoundOf({
                  matches: [
                    teamMatchOf({
                      id: 'tm1',
                      team1,
                      team2,
                      boardResults: [
                        boardResultOf({ boardPosition: 1, team1ReportedResult: 'PLAYER1_WIN' }),
                        boardResultOf({ boardPosition: 2 }),
                        boardResultOf({ boardPosition: 3 }),
                      ],
                    }),
                  ],
                }),
              ],
              teamStandings: [],
            }),
          ),
        ),
      ),
    );

    renderResultPage();

    expect(await screen.findByText('申告待ち')).toBeInTheDocument();
  });
});
