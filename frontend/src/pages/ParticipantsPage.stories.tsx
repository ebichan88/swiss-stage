import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { handlersFor } from '../../tests/msw/handlers';
import { ParticipantsPage } from './ParticipantsPage';

/** S06 参加者管理(個人戦)。PREPARING=追加・編集・削除可 */
const meta: Meta<typeof ParticipantsPage> = {
  component: ParticipantsPage,
  parameters: {
    route: '/participants',
    tournament: tournamentOf({ status: 'PREPARING' }),
  },
};

export default meta;

type Story = StoryObj<typeof ParticipantsPage>;

/** 通常: 16名登録済み */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.participants.filled() },
  },
};

/** 空: 参加者0人(EmptyStateとCSVインポート・追加の導線) */
export const Empty: Story = {
  parameters: {
    msw: { handlers: handlersFor.participants.empty() },
  },
};

/** 大量データ時の見え方確認用(300人) */
export const LargeRoster: Story = {
  parameters: {
    msw: { handlers: handlersFor.participants.largeRoster() },
  },
};
