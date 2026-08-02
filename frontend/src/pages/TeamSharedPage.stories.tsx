import type { Meta, StoryObj } from '@storybook/react-vite';

import {
  boardResultOf,
  groupTeamStandingsOf,
  sharedSummaryOf,
  sharedTournamentOf,
  teamMatchOf,
  teamRoundOf,
  teamStandingOf,
  teamSummaryOf,
} from '../../tests/fixtures';
import { TeamSharedPage } from './TeamSharedPage';

const TOKEN = '01TESTSHARETOKEN00000001';

/**
 * S10 共有ページの団体戦版。SharedPageが競技形式で振り分けた先の内部コンポーネントだが、
 * `data` propsを直接渡せるため個別のページストーリーとして探索する
 * (`.claude/07_plans/04_design_system_rollout.md` §3-4)。
 * 個人戦版(SharedPage.stories.tsx)と同じくスマホ幅(375px)で確認する
 */
const meta: Meta<typeof TeamSharedPage> = {
  component: TeamSharedPage,
  parameters: {
    globals: { viewport: { value: '375-812' } },
  },
  args: {
    token: TOKEN,
  },
};

export default meta;

type Story = StoryObj<typeof TeamSharedPage>;

/** 通常: 組み合わせタブ・結果入力が開いている */
export const Default: Story = {
  args: {
    data: sharedTournamentOf({
      tournament: sharedSummaryOf({
        competitionType: 'TEAM',
        teamSize: 3,
        resultInputEnabled: true,
      }),
      rounds: null,
      standings: null,
      teamRounds: [
        teamRoundOf({
          status: 'PLAYING',
          matches: [
            teamMatchOf({ id: 'tm1' }),
            teamMatchOf({
              id: 'tm2',
              tableNumber: 2,
              team1: teamSummaryOf({ id: 't3', name: 'Cチーム', entryOrder: 3 }),
              team2: teamSummaryOf({ id: 't4', name: 'Dチーム', entryOrder: 4 }),
            }),
          ],
        }),
      ],
      teamStandings: [groupTeamStandingsOf()],
    }),
  },
};

/** 一部のボードが結果入力済み(申告が一致し確定) */
export const WithReportedResult: Story = {
  args: {
    data: sharedTournamentOf({
      tournament: sharedSummaryOf({
        competitionType: 'TEAM',
        teamSize: 3,
        resultInputEnabled: true,
      }),
      rounds: null,
      standings: null,
      teamRounds: [
        teamRoundOf({
          status: 'PLAYING',
          matches: [
            teamMatchOf({
              id: 'tm1',
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
                boardResultOf({ boardPosition: 3 }),
              ],
            }),
          ],
        }),
      ],
      teamStandings: [
        groupTeamStandingsOf({
          standings: [
            teamStandingOf({ rank: 1, team: teamSummaryOf({ id: 't1', name: 'Aチーム' }) }),
            teamStandingOf({
              rank: 2,
              team: teamSummaryOf({ id: 't2', name: 'Bチーム' }),
              wins: 2,
              losses: 2,
            }),
          ],
        }),
      ],
    }),
  },
};
