import { defineConfig, devices } from '@playwright/test';

/**
 * Visual Regression Test設定(09_test_strategy.md / 11_cicd_design.md)。
 *
 * playwright.config.ts(E2E)とは完全に分離する。VRTはバックエンド・DynamoDBを必要とせず、
 * Storybookの静的ビルド(storybook-static/)に対して実行する。
 *
 * ベースライン画像はローカルで生成しない(WSL2とCIランナーでフォントレンダリングが異なり
 * 100%差分になるため)。必ず frontend/scripts/vrt.sh でCIと同じPlaywrightコンテナから実行する。
 */
export default defineConfig({
  testDir: 'tests/vrt',
  workers: 1,
  timeout: 30_000,
  expect: {
    timeout: 10_000,
    toHaveScreenshot: { maxDiffPixelRatio: 0 },
  },
  retries: 0,
  reporter: [['list']],
  // {arg} は呼び出し側が渡す名前(拡張子除く)、{ext} が拡張子('.png')
  snapshotPathTemplate: '{testDir}/__screenshots__/{projectName}/{arg}{ext}',
  use: {
    baseURL: 'http://localhost:6100',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } },
    },
    {
      name: 'mobile',
      use: { ...devices['Desktop Chrome'], viewport: { width: 375, height: 667 } },
    },
  ],
  webServer: {
    // serveのデフォルト(cleanUrls)は `/iframe.html?id=...` を `/iframe` に301リダイレクトし
    // クエリ文字列(id・viewMode)を消してしまう(Storybookが「No Preview」になる原因になった)。
    // tests/vrt/serve.json(cleanUrls:false)をビルド後のstorybook-static/にコピーして無効化する
    command:
      'pnpm run build-storybook && cp tests/vrt/serve.json storybook-static/serve.json && pnpm exec serve storybook-static -l 6100 -n -L',
    url: 'http://localhost:6100',
    reuseExistingServer: false,
    timeout: 60_000,
  },
});
