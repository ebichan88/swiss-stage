import { http, HttpResponse } from 'msw';
import type { Meta, StoryObj } from '@storybook/react-vite';

import {
  boardResultOf,
  sharedSummaryOf,
  sharedTournamentOf,
  teamMatchOf,
  teamRoundOf,
} from '../../tests/fixtures';
import { apiSuccess } from '../../tests/msw/apiResponse';
import { SharedTeamResultPage } from './SharedTeamResultPage';

const API = '/api/v1';
/** SharedTeamPageのfixtureと揃えるため、初期teamMatchOf()と同じidを使う */
const MATCH_ID = '01TESTTEAMMATCH000000000A';

/**
 * S11 共有・結果自己申告の団体戦版(スマホ優先)。
 * 個人戦版(SharedResultPage.stories.tsx)と同じ考え方で、送信後の状態は
 * 「両者の申告済み(=結果確定済み)の対局を開いた場合の見え方」で代替する
 */
const meta: Meta<typeof SharedTeamResultPage> = {
  component: SharedTeamResultPage,
  parameters: {
    routePath: '/s/:token/team-matches/:mid',
    route: `/s/01TESTSHARETOKEN00000001/team-matches/${MATCH_ID}`,
    globals: { viewport: { value: '375-812' } },
  },
};

export default meta;

type Story = StoryObj<typeof SharedTeamResultPage>;

/** 通常: 未申告。「あなたはどちらのチームですか?」から選択する */
export const Default: Story = {
  parameters: {
    msw: {
      handlers: [
        http.get(`${API}/shared/:token`, () =>
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
                    roundNumber: 1,
                    status: 'PLAYING',
                    matches: [teamMatchOf({ id: MATCH_ID })],
                  }),
                ],
                teamStandings: null,
              }),
            ),
          ),
        ),
      ],
    },
  },
};

/** 両チームの申告が一致し全ボードが確定済みの対局を開いた場合 */
export const AlreadyDecided: Story = {
  parameters: {
    msw: {
      handlers: [
        http.get(`${API}/shared/:token`, () =>
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
                    roundNumber: 1,
                    status: 'PLAYING',
                    matches: [
                      teamMatchOf({
                        id: MATCH_ID,
                        boardResults: [
                          boardResultOf({
                            boardPosition: 1,
                            result: 'PLAYER1_WIN',
                            team1ReportedResult: 'PLAYER1_WIN',
                            team2ReportedResult: 'PLAYER1_WIN',
                          }),
                          boardResultOf({
                            boardPosition: 2,
                            result: 'PLAYER1_WIN',
                            team1ReportedResult: 'PLAYER1_WIN',
                            team2ReportedResult: 'PLAYER1_WIN',
                          }),
                          boardResultOf({
                            boardPosition: 3,
                            result: 'PLAYER2_WIN',
                            team1ReportedResult: 'PLAYER2_WIN',
                            team2ReportedResult: 'PLAYER2_WIN',
                          }),
                        ],
                      }),
                    ],
                  }),
                ],
                teamStandings: null,
              }),
            ),
          ),
        ),
      ],
    },
  },
};
