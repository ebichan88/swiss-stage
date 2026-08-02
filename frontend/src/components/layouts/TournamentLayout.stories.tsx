import type { Meta, StoryObj } from '@storybook/react-vite';

import { handlersFor } from '../../../tests/msw/handlers';
import { TournamentLayout } from './TournamentLayout';

const TOURNAMENT_ID = '01TESTTOURNAMENT0000000000';

/**
 * 大会管理系画面の共通レイアウト(見出し帯・サイドバー/下部タブ)。
 * ページ本体(Outlet配下)は個別画面の担当のため、このストーリーでは空のまま表示する
 * (`.claude/07_plans/04_design_system_rollout.md` §3-4)。
 * PC(サイドバー)/スマホ(下部タブ)の両方をビューポート違いで確認する
 */
const meta: Meta<typeof TournamentLayout> = {
  component: TournamentLayout,
  parameters: {
    routePath: '/tournaments/:id/rounds',
    route: `/tournaments/${TOURNAMENT_ID}/rounds`,
    msw: { handlers: handlersFor.tournamentDetail.filled({ id: TOURNAMENT_ID, currentRound: 1 }) },
  },
};

export default meta;

type Story = StoryObj<typeof TournamentLayout>;

/** PC幅: 左サイドバーで現在地(ラウンド)がアクセント表示される */
export const Desktop: Story = {};

/** スマホ幅: 下部固定タブで現在地(ラウンド)がアクセント表示される */
export const Mobile: Story = {
  parameters: {
    globals: { viewport: { value: '375-812' } },
  },
};
