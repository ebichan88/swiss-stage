import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { TournamentOverviewPage } from './TournamentOverviewPage';

/** S05 大会管理(概要)。個人戦は参加者数、団体戦はチーム数で開始条件を判定する */
const meta: Meta<typeof TournamentOverviewPage> = {
  component: TournamentOverviewPage,
  parameters: {
    route: '/overview',
    tournament: tournamentOf({ status: 'PREPARING' }),
  },
};

export default meta;

type Story = StoryObj<typeof TournamentOverviewPage>;

/** 個人戦・開催中(帳票印刷・終了ボタン) */
export const InProgress: Story = {
  parameters: {
    tournament: tournamentOf({ status: 'IN_PROGRESS', currentRound: 2 }),
    msw: { handlers: handlersFor.participants.filled() },
  },
};

/** 個人戦・準備中で参加者2名未満(開始不可の警告表示) */
export const PreparingBelowMinimum: Story = {
  parameters: {
    tournament: tournamentOf({ status: 'PREPARING' }),
    msw: { handlers: handlersFor.participants.empty() },
  },
};

/** 団体戦・開催中 */
export const TeamInProgress: Story = {
  parameters: {
    tournament: tournamentOf({
      competitionType: 'TEAM',
      teamSize: 3,
      status: 'IN_PROGRESS',
      currentRound: 1,
    }),
    msw: { handlers: handlersFor.teams.filled() },
  },
};
