import type { Meta, StoryObj } from '@storybook/react-vite';

import { handlersFor } from '../../tests/msw/handlers';
import { LoginPage } from './LoginPage';

/** S02 ログイン */
const meta: Meta<typeof LoginPage> = {
  component: LoginPage,
  parameters: {
    msw: { handlers: handlersFor.auth.unauthenticated() },
  },
};

export default meta;

type Story = StoryObj<typeof LoginPage>;

/** 通常(OAuthエラーなし。AUTH-AC-010) */
export const Default: Story = {
  parameters: { route: '/login' },
};

/** OAuth認可失敗時のエラー表示(AUTH-AC-007) */
export const OAuthError: Story = {
  parameters: { route: '/login?error=oauth', routePath: '/login' },
};
