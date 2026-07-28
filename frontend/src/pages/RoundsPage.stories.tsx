import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { RoundsPage } from './RoundsPage';

/** S07 ラウンド管理(個人戦) */
const meta: Meta<typeof RoundsPage> = {
  component: RoundsPage,
  parameters: {
    route: '/rounds',
    tournament: tournamentOf({ status: 'IN_PROGRESS', currentRound: 1 }),
  },
};

export default meta;

type Story = StoryObj<typeof RoundsPage>;

/** 通常: 第1ラウンドの組み合わせ表示中 */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.rounds.filled() },
  },
};

/** 大会開始済みだが組み合わせ未生成(EmptyStateと生成ボタン) */
export const NotGenerated: Story = {
  parameters: {
    tournament: tournamentOf({ status: 'IN_PROGRESS', currentRound: 0 }),
    msw: { handlers: handlersFor.rounds.notGenerated() },
  },
};
