import type { Meta, StoryObj } from '@storybook/react-vite';
import { userEvent, within } from 'storybook/test';

import { handlersFor } from '../../tests/msw/handlers';
import { TournamentListPage } from './TournamentListPage';

/**
 * S03 大会一覧。Outlet contextに依存しない(useTournamentContextを使わない)ため、
 * 単純にMSWハンドラだけ切り替えれば状態を再現できる。
 */
const meta: Meta<typeof TournamentListPage> = {
  component: TournamentListPage,
};

export default meta;

type Story = StoryObj<typeof TournamentListPage>;

/** 通常: 個人戦・団体戦が混在した一覧(TRN-AC-015: 大会カードの表示) */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.tournamentList.filled() },
  },
};

/** 空: 大会が1件もない状態(EmptyStateと「最初の大会を作成する」導線) */
export const Empty: Story = {
  parameters: {
    msw: { handlers: handlersFor.tournamentList.empty() },
  },
};

/** 検索・フィルタの結果が0件の状態(TRN-AC-023: 専用EmptyStateと「検索条件をクリア」導線) */
export const FilteredEmpty: Story = {
  parameters: {
    msw: { handlers: handlersFor.tournamentList.filled() },
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const searchField = await canvas.findByRole('textbox', { name: '大会名で検索' });
    await userEvent.type(searchField, '該当なし');
  },
};
