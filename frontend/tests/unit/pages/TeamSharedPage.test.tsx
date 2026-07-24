import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { SharedPage } from '../../../src/pages/SharedPage';
import {
  boardResultOf,
  groupOf,
  groupTeamStandingsOf,
  sharedSummaryOf,
  sharedTournamentOf,
  teamMatchOf,
  teamRoundOf,
  teamStandingOf,
  teamSummaryOf,
} from '../../fixtures';
import { apiSuccess, server } from '../../msw/server';
import { renderWithProviders } from '../../testUtils';

const TOKEN = 'C'.repeat(43);

function renderSharedPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/s/:token" element={<SharedPage />} />
    </Routes>,
    { route: `/s/${TOKEN}` },
  );
}

const teamTournament = sharedSummaryOf({ competitionType: 'TEAM', teamSize: 3 });

describe('TeamSharedPage', () => {
  it('大会名・現在ラウンド・チームの組み合わせを表示し、順位表タブに切り替えられる(個人名は出さない)', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({
                ...teamTournament,
                name: 'チーム共有テスト大会',
                currentRound: 1,
              }),
              rounds: null,
              standings: null,
              teamRounds: [
                teamRoundOf({
                  matches: [
                    teamMatchOf({
                      team1: teamSummaryOf({ id: 't1', name: 'Aチーム' }),
                      team2: teamSummaryOf({ id: 't2', name: 'Bチーム' }),
                    }),
                  ],
                }),
              ],
              teamStandings: [groupTeamStandingsOf()],
            }),
          ),
        ),
      ),
    );

    renderSharedPage();

    expect(
      await screen.findByRole('heading', { name: 'チーム共有テスト大会' }),
    ).toBeInTheDocument();
    expect(screen.getByText('第1ラウンド / 全5ラウンド')).toBeInTheDocument();
    expect(screen.getByText('Aチーム')).toBeInTheDocument();
    expect(screen.getByText('Bチーム')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '結果入力' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: '順位表' }));
    expect(await screen.findByRole('list', { name: '順位' })).toBeInTheDocument();
  });

  it('戦績一覧タブに切り替えるとチーム名とボード内訳を含む一覧表が表示される', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: teamTournament,
              rounds: null,
              standings: null,
              teamRounds: [
                teamRoundOf({
                  roundNumber: 1,
                  matches: [
                    teamMatchOf({
                      team1: teamSummaryOf({ id: 't1', name: 'Aチーム', entryOrder: 1 }),
                      team2: teamSummaryOf({ id: 't2', name: 'Bチーム', entryOrder: 2 }),
                      boardResults: [
                        boardResultOf({ boardPosition: 1, result: 'PLAYER1_WIN' }),
                        boardResultOf({ boardPosition: 2, result: 'PLAYER1_WIN' }),
                        boardResultOf({ boardPosition: 3, result: 'PLAYER2_WIN' }),
                      ],
                    }),
                  ],
                }),
              ],
              teamStandings: [
                groupTeamStandingsOf({
                  standings: [
                    teamStandingOf({
                      rank: 1,
                      team: teamSummaryOf({ id: 't1', name: 'Aチーム', entryOrder: 1 }),
                    }),
                    teamStandingOf({
                      rank: 2,
                      team: teamSummaryOf({ id: 't2', name: 'Bチーム', entryOrder: 2 }),
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
    expect(rows[2]).toHaveTextContent('Aチーム');
    expect(rows[2]).toHaveTextContent('2-1');
  });

  it('グループ大会の順位表はグループごとに見出し付きで表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: teamTournament,
              rounds: null,
              standings: null,
              teamRounds: [],
              teamStandings: [
                groupTeamStandingsOf({
                  group: groupOf({ id: 'g1', name: 'A' }),
                  standings: [teamStandingOf({ team: teamSummaryOf({ name: 'Aチーム' }) })],
                }),
                groupTeamStandingsOf({
                  group: groupOf({ id: 'g2', name: 'B' }),
                  standings: [
                    teamStandingOf({ team: teamSummaryOf({ id: 't2', name: 'Bチーム' }) }),
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
    expect(screen.getAllByRole('list', { name: '順位' })).toHaveLength(2);
  });

  it('結果入力が許可されていればチーム対局の結果入力ページへのリンクを表示する', async () => {
    server.use(
      http.get(`/api/v1/shared/${TOKEN}`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({ ...teamTournament, resultInputEnabled: true }),
              rounds: null,
              standings: null,
              teamRounds: [teamRoundOf({ matches: [teamMatchOf({ id: 'tm1' })] })],
              teamStandings: [],
            }),
          ),
        ),
      ),
    );

    renderSharedPage();

    const link = await screen.findByRole('link', { name: '結果入力' });
    expect(link).toHaveAttribute('href', `/s/${TOKEN}/team-matches/tm1`);
  });
});
