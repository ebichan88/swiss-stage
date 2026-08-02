import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { TeamMatchResultsPage } from './TeamMatchResultsPage';

/** S09 対戦結果(団体戦)。順位表とは別メニューで表示する。個人名は含めない */
const meta: Meta<typeof TeamMatchResultsPage> = {
  component: TeamMatchResultsPage,
  parameters: {
    route: '/match-results',
    tournament: tournamentOf({
      competitionType: 'TEAM',
      teamSize: 3,
      status: 'IN_PROGRESS',
      currentRound: 1,
    }),
  },
};

export default meta;

type Story = StoryObj<typeof TeamMatchResultsPage>;

/** 通常: ラウンド確定済みのチーム対戦結果表 */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.teamStandings.filled() },
  },
};
