import { http, HttpResponse } from 'msw';

import {
  groupOf,
  groupStandingsOf,
  groupTeamStandingsOf,
  matchOf,
  participantOf,
  roundOf,
  sharedSummaryOf,
  sharedTournamentOf,
  standingOf,
  summaryOf,
  teamMatchOf,
  teamOf,
  teamRoundOf,
  teamStandingOf,
  tournamentOf,
} from '../fixtures';
import { apiError, apiSuccess } from './apiResponse';

/**
 * Storybookのページストーリー用MSWハンドラ(msw-storybook-addon)。
 * パスは実際のリクエストと同じ相対パス(src/services/apiClient.ts の BASE_PATH)。
 * `:id` 等はワイルドカードマッチのため、Outlet contextで渡すtournamentのidを気にせず使い回せる。
 *
 * 既存のVitestテスト(tests/unit/**)はこのファイルに依存しない(server.use()での個別定義のまま)。
 */
const API = '/api/v1';

/** 大量データ表示(300人)確認用。個人名は架空のみ(09_test_strategy.md §5) */
function manyParticipants(count: number) {
  return Array.from({ length: count }, (_, i) =>
    participantOf({
      id: `01TESTPARTICIPANT${String(i + 1).padStart(9, '0')}`,
      name: `参加者 ${i + 1}`,
      entryOrder: i + 1,
    }),
  );
}

function manyTeams(count: number) {
  return Array.from({ length: count }, (_, i) =>
    teamOf({
      id: `01TESTTEAM${String(i + 1).padStart(16, '0')}`,
      name: `チーム ${i + 1}`,
      entryOrder: i + 1,
    }),
  );
}

function manyStandings(count: number) {
  return Array.from({ length: count }, (_, i) =>
    standingOf({
      rank: i + 1,
      participant: summaryOf({ id: `p${i + 1}`, name: `参加者 ${i + 1}`, entryOrder: i + 1 }),
      wins: Math.max(0, 5 - Math.floor(i / 20)),
      losses: Math.min(5, Math.floor(i / 20)),
      sos: 30 - i * 0.1,
    }),
  );
}

export const handlersFor = {
  /** TournamentLayout(共通レイアウト)自体はuseTournament(id)で個別に大会情報を取得する */
  tournamentDetail: {
    filled: (overrides: Parameters<typeof tournamentOf>[0] = {}) => [
      http.get(`${API}/tournaments/:id`, () =>
        HttpResponse.json(apiSuccess(tournamentOf({ status: 'IN_PROGRESS', ...overrides }))),
      ),
    ],
  },

  tournamentList: {
    filled: () => [
      http.get(`${API}/tournaments`, () =>
        HttpResponse.json(
          apiSuccess([
            tournamentOf({
              id: '01TESTTOURNAMENT0000000001',
              name: '第3回テスト将棋大会',
              gameType: 'SHOGI',
              status: 'IN_PROGRESS',
              currentRound: 2,
            }),
            tournamentOf({
              id: '01TESTTOURNAMENT0000000002',
              name: '第2回テスト囲碁団体戦',
              competitionType: 'TEAM',
              teamSize: 3,
              status: 'PREPARING',
            }),
            tournamentOf({
              id: '01TESTTOURNAMENT0000000003',
              name: '第1回テスト囲碁大会',
              status: 'FINISHED',
              currentRound: 5,
            }),
          ]),
        ),
      ),
    ],
    empty: () => [http.get(`${API}/tournaments`, () => HttpResponse.json(apiSuccess([])))],
  },

  standings: {
    /** ラウンド1確定済み・通常表示 */
    filled: () => [
      http.get(`${API}/tournaments/:id/standings`, () =>
        HttpResponse.json(apiSuccess([groupStandingsOf({ standings: manyStandings(8) })])),
      ),
      http.get(`${API}/tournaments/:id/rounds`, () =>
        HttpResponse.json(apiSuccess([roundOf({ roundNumber: 1, status: 'CONFIRMED' })])),
      ),
    ],
    /** ラウンド1が未確定の間は順位表を出さない(05_swiss_pairing_algorithm.md) */
    beforeRound1Confirmed: () => [
      http.get(`${API}/tournaments/:id/standings`, () =>
        HttpResponse.json(apiSuccess([groupStandingsOf({ standings: [] })])),
      ),
      http.get(`${API}/tournaments/:id/rounds`, () =>
        HttpResponse.json(apiSuccess([roundOf({ roundNumber: 1, status: 'PLAYING' })])),
      ),
    ],
    /** 大量データ時の見え方確認用(300人) */
    largeRoster: () => [
      http.get(`${API}/tournaments/:id/standings`, () =>
        HttpResponse.json(apiSuccess([groupStandingsOf({ standings: manyStandings(300) })])),
      ),
      http.get(`${API}/tournaments/:id/rounds`, () =>
        HttpResponse.json(apiSuccess([roundOf({ roundNumber: 1, status: 'CONFIRMED' })])),
      ),
    ],
  },

  matchResults: {
    filled: () => [
      http.get(`${API}/tournaments/:id/standings`, () =>
        HttpResponse.json(apiSuccess([groupStandingsOf({ standings: manyStandings(4) })])),
      ),
      http.get(`${API}/tournaments/:id/rounds`, () =>
        HttpResponse.json(
          apiSuccess([
            roundOf({
              roundNumber: 1,
              status: 'CONFIRMED',
              matches: [
                matchOf({ id: 'm1', result: 'PLAYER1_WIN' }),
                matchOf({
                  id: 'm2',
                  tableNumber: 2,
                  player1: summaryOf({ id: 'p3', name: '仮名 三郎' }),
                  player2: summaryOf({ id: 'p4', name: '仮名 四郎' }),
                  result: 'PLAYER2_WIN',
                }),
              ],
            }),
          ]),
        ),
      ),
    ],
    /** 未入力の対局が混在するラウンド */
    withUndecided: () => [
      http.get(`${API}/tournaments/:id/standings`, () =>
        HttpResponse.json(apiSuccess([groupStandingsOf({ standings: manyStandings(4) })])),
      ),
      http.get(`${API}/tournaments/:id/rounds`, () =>
        HttpResponse.json(
          apiSuccess([
            roundOf({
              roundNumber: 1,
              status: 'PLAYING',
              matches: [
                matchOf({ id: 'm1', result: 'PLAYER1_WIN' }),
                matchOf({
                  id: 'm2',
                  tableNumber: 2,
                  player1: summaryOf({ id: 'p3', name: '仮名 三郎' }),
                  player2: summaryOf({ id: 'p4', name: '仮名 四郎' }),
                  result: 'NONE',
                }),
              ],
            }),
          ]),
        ),
      ),
    ],
  },

  participants: {
    filled: () => [
      http.get(`${API}/tournaments/:id/participants`, () =>
        HttpResponse.json(apiSuccess(manyParticipants(16))),
      ),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
    empty: () => [
      http.get(`${API}/tournaments/:id/participants`, () => HttpResponse.json(apiSuccess([]))),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
    /** 大量データ時の見え方確認用(300人) */
    largeRoster: () => [
      http.get(`${API}/tournaments/:id/participants`, () =>
        HttpResponse.json(apiSuccess(manyParticipants(300))),
      ),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
  },

  rounds: {
    filled: () => [
      http.get(`${API}/tournaments/:id/rounds`, () =>
        HttpResponse.json(apiSuccess([roundOf({ roundNumber: 1, status: 'PLAYING' })])),
      ),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
    /** 大会開始済みだが組み合わせ未生成 */
    notGenerated: () => [
      http.get(`${API}/tournaments/:id/rounds`, () => HttpResponse.json(apiSuccess([]))),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
  },

  teams: {
    filled: () => [
      http.get(`${API}/tournaments/:id/teams`, () => HttpResponse.json(apiSuccess(manyTeams(8)))),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
    empty: () => [
      http.get(`${API}/tournaments/:id/teams`, () => HttpResponse.json(apiSuccess([]))),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
  },

  teamRounds: {
    filled: () => [
      http.get(`${API}/tournaments/:id/team-rounds`, () =>
        HttpResponse.json(
          apiSuccess([
            teamRoundOf({
              roundNumber: 1,
              status: 'PLAYING',
              matches: [
                teamMatchOf({
                  id: 'm1',
                  team1: teamOf({ id: 't1', name: 'Aチーム' }),
                  team2: teamOf({ id: 't2', name: 'Bチーム' }),
                }),
              ],
            }),
          ]),
        ),
      ),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
    /** 大会開始済みだが組み合わせ未生成 */
    notGenerated: () => [
      http.get(`${API}/tournaments/:id/team-rounds`, () => HttpResponse.json(apiSuccess([]))),
      http.get(`${API}/tournaments/:id/groups`, () => HttpResponse.json(apiSuccess([groupOf()]))),
    ],
  },

  teamStandings: {
    filled: () => [
      http.get(`${API}/tournaments/:id/team-standings`, () =>
        HttpResponse.json(
          apiSuccess([
            groupTeamStandingsOf({
              standings: [
                teamStandingOf({ rank: 1, team: teamOf({ id: 't1', name: 'Aチーム' }) }),
                teamStandingOf({
                  rank: 2,
                  team: teamOf({ id: 't2', name: 'Bチーム' }),
                  wins: 2,
                  losses: 2,
                }),
              ],
            }),
          ]),
        ),
      ),
      http.get(`${API}/tournaments/:id/team-rounds`, () =>
        HttpResponse.json(
          apiSuccess([
            teamRoundOf({
              roundNumber: 1,
              status: 'CONFIRMED',
              matches: [teamMatchOf()],
            }),
          ]),
        ),
      ),
    ],
  },

  shared: {
    /** 通常表示(組み合わせタブ)。結果入力が開いており、まだ入力されていない */
    normal: () => [
      http.get(`${API}/shared/:token`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({ resultInputEnabled: true }),
            }),
          ),
        ),
      ),
    ],
    /** 一部の対局が結果入力済み(両者の申告が一致し確定済み) */
    withReportedResult: () => [
      http.get(`${API}/shared/:token`, () =>
        HttpResponse.json(
          apiSuccess(
            sharedTournamentOf({
              tournament: sharedSummaryOf({ resultInputEnabled: true }),
              rounds: [
                roundOf({
                  roundNumber: 1,
                  status: 'PLAYING',
                  matches: [
                    matchOf({
                      player1ReportedResult: 'PLAYER1_WIN',
                      player2ReportedResult: 'PLAYER1_WIN',
                      result: 'PLAYER1_WIN',
                    }),
                  ],
                }),
              ],
            }),
          ),
        ),
      ),
    ],
    /** 再発行等で無効化された共有トークン(06_error_handling_design.md: 403 INVALID_SHARE_TOKEN) */
    invalidToken: () => [
      http.get(`${API}/shared/:token`, () =>
        HttpResponse.json(
          apiError('INVALID_SHARE_TOKEN', 'このURLは無効になっています。運営者に確認してください'),
          { status: 403 },
        ),
      ),
    ],
  },
};
