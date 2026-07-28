import { expect, test } from '@playwright/test';

import {
  createTournament,
  importParticipantsCsv,
  loginAsOrganizer,
  stubWindowPrint,
} from './helpers';

/**
 * CP7: 帳票印刷。jsdomは@media print/@pageを解釈しないため、印刷メディアでの見え方は
 * PlaywrightのemulateMediaでしか検証できない(12_e2e_test_design.md の「クリティカルパスのみ」の
 * 技術的な例外)。@page(向き・A4面付け)自体はCSSOMから確認できないため、Chromeの印刷プレビューでの
 * 目視確認をverify手順側で行う。
 */
test('E2E-AC-009,PRT-AC-001,PRT-AC-004: 印刷画面は印刷メディアでツールバー・ナビゲーションを非表示にする', async ({
  page,
}) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP7印刷テスト大会 ${Date.now()}`, 5);
  await importParticipantsCsv(page, tournamentId, 'participants_16.csv', 16);

  await stubWindowPrint(page);
  await page.goto(`/tournaments/${tournamentId}/print/roster`);
  await expect(page.getByRole('table')).toBeVisible();
  await expect(page.getByRole('cell', { name: '井山 太郎' })).toBeVisible();

  // 運営者画面(AppLayout/TournamentLayout)のナビゲーションを一切継承していない
  await expect(page.getByRole('navigation')).toHaveCount(0);

  const printButton = page.getByRole('button', { name: '印刷する' });
  const backButton = page.getByRole('link', { name: '大会管理に戻る' });
  await expect(printButton).toBeVisible();
  await expect(backButton).toBeVisible();

  await printButton.click();
  expect(
    await page.evaluate(() => (window as unknown as { __printCalled?: boolean }).__printCalled),
  ).toBe(true);

  await page.emulateMedia({ media: 'print' });
  await expect(printButton).toBeHidden();
  await expect(backButton).toBeHidden();
  // 帳票本体は印刷メディアでも表示され続ける
  await expect(page.getByRole('table')).toBeVisible();
});
