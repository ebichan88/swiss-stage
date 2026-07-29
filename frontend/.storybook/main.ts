import type { StorybookConfig } from '@storybook/react-vite';

const config: StorybookConfig = {
  stories: ['../src/**/*.stories.tsx'],
  framework: {
    name: '@storybook/react-vite',
    options: {},
  },
  // public/mockServiceWorker.js を配信するために必要(msw-storybook-addonのworker登録先)
  staticDirs: ['../public'],
};

export default config;
