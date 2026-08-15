import type { Meta, StoryObj } from '@storybook/react-vite';

import { handlersFor } from '../../tests/msw/handlers';
import { TopPage } from './TopPage';

/** S01 トップ(LP) */
const meta: Meta<typeof TopPage> = {
  component: TopPage,
  parameters: {
    msw: { handlers: handlersFor.auth.unauthenticated() },
  },
};

export default meta;

type Story = StoryObj<typeof TopPage>;

/** 通常(未ログイン。AUTH-AC-006) */
export const Default: Story = {};
