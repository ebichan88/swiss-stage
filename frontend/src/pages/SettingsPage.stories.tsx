import type { Meta, StoryObj } from '@storybook/react-vite';

import { tournamentOf } from '../../tests/fixtures';
import { SettingsPage } from './SettingsPage';

/** S09 大会設定(名前・公開範囲・結果入力許可・共有URL・削除) */
const meta: Meta<typeof SettingsPage> = {
  component: SettingsPage,
  parameters: {
    route: '/settings',
    tournament: tournamentOf(),
  },
};

export default meta;

type Story = StoryObj<typeof SettingsPage>;

/** 通常: 共有URL未発行 */
export const Default: Story = {};

/** 共有URL発行済み */
export const ShareUrlIssued: Story = {
  parameters: {
    tournament: tournamentOf({
      visibility: 'PUBLIC',
      shareToken: '01TESTSHARETOKEN00000000000',
    }),
  },
};
