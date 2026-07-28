import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { MatchResultsPage } from './MatchResultsPage';

/** S09 対戦結果(個人戦)。順位表とは別メニューで表示する(RND-AC-013) */
const meta: Meta<typeof MatchResultsPage> = {
  component: MatchResultsPage,
  parameters: {
    route: '/match-results',
    tournament: tournamentOf({ status: 'IN_PROGRESS', currentRound: 1 }),
  },
};

export default meta;

type Story = StoryObj<typeof MatchResultsPage>;

/** 通常: 全対局が確定済み */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.matchResults.filled() },
  },
};

/** 未入力の対局が混在するラウンド */
export const WithUndecidedMatches: Story = {
  parameters: {
    msw: { handlers: handlersFor.matchResults.withUndecided() },
  },
};
