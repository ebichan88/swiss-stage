import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { StandingsPage } from './StandingsPage';

/**
 * S08 順位表(個人戦)。useTournamentContext() 経由でTournamentを要求するため、
 * decorator(.storybook/decorators.tsx)にOutlet contextとして渡す。
 */
const meta: Meta<typeof StandingsPage> = {
  component: StandingsPage,
  parameters: {
    route: '/standings',
    tournament: tournamentOf({ status: 'IN_PROGRESS', currentRound: 3 }),
  },
};

export default meta;

type Story = StoryObj<typeof StandingsPage>;

/** 通常: ラウンド確定済みの順位表 */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.standings.filled() },
  },
};

/** ラウンド1が未確定の間は順位表を表示しない(全員同率rank=1になるため。RND-AC-014) */
export const BeforeRound1Confirmed: Story = {
  parameters: {
    tournament: tournamentOf({ status: 'IN_PROGRESS', currentRound: 1 }),
    msw: { handlers: handlersFor.standings.beforeRound1Confirmed() },
  },
};

/** 大量データ時の見え方確認用(300人) */
export const LargeRoster: Story = {
  parameters: {
    msw: { handlers: handlersFor.standings.largeRoster() },
  },
};
