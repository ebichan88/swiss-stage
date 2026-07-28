import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { TeamStandingsPage } from './TeamStandingsPage';

/** S08 順位表(団体戦)。個人名は含めない */
const meta: Meta<typeof TeamStandingsPage> = {
  component: TeamStandingsPage,
  parameters: {
    route: '/standings',
    tournament: tournamentOf({
      competitionType: 'TEAM',
      teamSize: 3,
      status: 'IN_PROGRESS',
      currentRound: 1,
    }),
  },
};

export default meta;

type Story = StoryObj<typeof TeamStandingsPage>;

/** 通常: ラウンド確定済みのチーム順位表 */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.teamStandings.filled() },
  },
};
