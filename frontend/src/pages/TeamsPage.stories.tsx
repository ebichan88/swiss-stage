import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { TeamsPage } from './TeamsPage';

/** S06 チーム管理(団体戦)。PREPARING=追加・編集・削除可 */
const meta: Meta<typeof TeamsPage> = {
  component: TeamsPage,
  parameters: {
    route: '/teams',
    tournament: tournamentOf({ competitionType: 'TEAM', teamSize: 3, status: 'PREPARING' }),
  },
};

export default meta;

type Story = StoryObj<typeof TeamsPage>;

/** 通常: 8チーム登録済み */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.teams.filled() },
  },
};

/** 空: チーム0件(EmptyStateとCSVインポート・追加の導線) */
export const Empty: Story = {
  parameters: {
    msw: { handlers: handlersFor.teams.empty() },
  },
};
