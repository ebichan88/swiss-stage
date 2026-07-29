import '@fontsource/noto-sans-jp/400.css';
import '@fontsource/noto-sans-jp/600.css';
import '@fontsource/noto-sans-jp/700.css';

import { initialize, mswLoader } from 'msw-storybook-addon';
import type { Preview } from '@storybook/react-vite';

import { withProviders } from './decorators';

// フォントはGoogle Fontsの外部読み込み(index.html)ではなく@fontsourceで同梱する。
// VRT(将来のPhase C)でネットワーク依存によるレンダリング差分を出さないため
initialize({ onUnhandledRequest: 'error' });

const preview: Preview = {
  decorators: [withProviders],
  loaders: [mswLoader],
  parameters: {
    layout: 'fullscreen',
  },
};

export default preview;
