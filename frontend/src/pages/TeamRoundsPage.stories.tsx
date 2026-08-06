import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { TeamRoundsPage } from './TeamRoundsPage';

/** S07 ラウンド管理(団体戦) */
const meta: Meta<typeof TeamRoundsPage> = {
  component: TeamRoundsPage,
  parameters: {
    route: '/team-rounds',
    tournament: tournamentOf({
      competitionType: 'TEAM',
      teamSize: 3,
      status: 'IN_PROGRESS',
      currentRound: 1,
    }),
  },
};

export default meta;

type Story = StoryObj<typeof TeamRoundsPage>;

/** 通常: 第1ラウンドの組み合わせ表示中 */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.teamRounds.filled() },
  },
};

/** 大会開始済みだが組み合わせ未生成(EmptyStateと生成ボタン) */
export const NotGenerated: Story = {
  parameters: {
    tournament: tournamentOf({
      competitionType: 'TEAM',
      teamSize: 3,
      status: 'IN_PROGRESS',
      currentRound: 0,
    }),
    msw: { handlers: handlersFor.teamRounds.notGenerated() },
  },
};
