import { defineConfig, devices } from '@playwright/test';

/**
 * E2Eテスト設定(12_e2e_test_design.md)。
 *
 * 前提: DynamoDB Local(:8000)+ バックエンド(:8080、localプロファイル)起動済み。
 *   cd backend && docker compose up -d dynamodb-local && ./scripts/create-table.sh
 *   ./gradlew bootRun --args='--spring.profiles.active=local'
 *
 * Vite開発サーバー(:5173)はwebServerで自動起動する(起動済みならそれを使う)。
 */
export default defineConfig({
  testDir: 'tests/e2e',
  // 単一バックエンド・DynamoDB Localを共有するため直列実行(E2Eはリリース前のみで速度は問わない)
  workers: 1,
  timeout: 90_000,
  expect: { timeout: 10_000 },
  retries: process.env.CI ? 1 : 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 30_000,
  },
});
