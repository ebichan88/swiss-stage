import type { Meta, StoryObj } from '@storybook/react-vite';

import { handlersFor } from '../../tests/msw/handlers';
import { SharedPage } from './SharedPage';

/**
 * S10 共有ページ(参加者向け・スマホ優先)。useParams() でtokenを読むため
 * Outlet contextではなく decorator の routePath('/s/:token')で供給する。
 * 最重要画面の1つ(00_basic_design.md §4)のためスマホ幅(375px)で確認する。
 */
const meta: Meta<typeof SharedPage> = {
  component: SharedPage,
  parameters: {
    routePath: '/s/:token',
    route: '/s/01TESTSHARETOKEN00000001',
    globals: { viewport: { value: '375-812' } },
  },
};

export default meta;

type Story = StoryObj<typeof SharedPage>;

/** 通常: 組み合わせタブ・結果入力が開いている */
export const Default: Story = {
  parameters: {
    msw: { handlers: handlersFor.shared.normal() },
  },
};

/** 一部の対局が結果入力済み(自己申告が一致し確定) */
export const WithReportedResult: Story = {
  parameters: {
    msw: { handlers: handlersFor.shared.withReportedResult() },
  },
};

/** 無効トークン: 再発行等で無効化された共有URL */
export const InvalidToken: Story = {
  parameters: {
    msw: { handlers: handlersFor.shared.invalidToken() },
  },
};
