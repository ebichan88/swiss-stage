import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { expect, test } from '@playwright/test';

const dirname = path.dirname(fileURLToPath(import.meta.url));

/**
 * Visual Regression Test(09_test_strategy.md)。
 * storybook-static/index.json から全ページストーリーを列挙し、それぞれのiframeをスクリーンショット比較する。
 * ストーリーを追加すれば自動的にVRT対象が増える。
 *
 * ベースライン画像はローカルで生成しない。必ず frontend/scripts/vrt.sh(CIと同じPlaywrightコンテナ)で実行する。
 */

interface StorybookIndexEntry {
  type: string;
  id: string;
  title: string;
  name: string;
}

interface StorybookIndex {
  entries: Record<string, StorybookIndexEntry>;
}

function loadStoryIds(): { id: string; label: string }[] {
  const indexPath = path.resolve(dirname, '../../storybook-static/index.json');
  const index = JSON.parse(readFileSync(indexPath, 'utf-8')) as StorybookIndex;
  return Object.values(index.entries)
    .filter((entry) => entry.type === 'story')
    .map((entry) => ({ id: entry.id, label: `${entry.title} / ${entry.name}` }))
    .sort((a, b) => a.id.localeCompare(b.id));
}

const stories = loadStoryIds();

test.describe('storybook pages', () => {
  for (const { id, label } of stories) {
    test(label, async ({ page }) => {
      // 外部フォントに依存していないことを保証する(@fontsource/noto-sans-jpを同梱済み)
      await page.route('https://fonts.googleapis.com/**', (route) => route.abort());
      await page.route('https://fonts.gstatic.com/**', (route) => route.abort());

      await page.goto(`/iframe.html?id=${id}&viewMode=story`, { waitUntil: 'networkidle' });

      // アニメーション全停止(MUIのripple対策)。Playwrightの animations:'disabled' は
      // CSSアニメーション/トランジションを止めるが、caret-color(テキストカーソルの点滅)は対象外のため追加する
      await page.addStyleTag({
        content:
          '*{animation:none!important;transition:none!important;caret-color:transparent!important}',
      });

      await page.evaluate(() => document.fonts.ready);

      await expect(page).toHaveScreenshot(`${id}.png`, { fullPage: true });
    });
  }
});
