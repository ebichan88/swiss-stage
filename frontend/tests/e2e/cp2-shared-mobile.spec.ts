import { expect, test } from '@playwright/test';

import {
  createTournament,
  fetchRounds,
  generateRound,
  importParticipantsCsv,
  loginAsOrganizer,
  publishShareUrl,
  startTournament,
} from './helpers';

/**
 * CP2: 参加者の結果送信(スマホ)(12_e2e_test_design.md)。
 * 共有URLにモバイルビューポートでアクセス → 自分の卓を確認 → 結果入力 → 送信
 * → 運営者側に反映されていること
 */
test('CP2: 参加者がスマホの共有ページから結果を送信できる', async ({ page, browser }) => {
  // 運営者: 大会準備 + 共有URL発行(結果入力許可)
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP2共有 ${Date.now()}`, 3);
  await importParticipantsCsv(page, tournamentId, 'participants_16.csv', 16);
  await startTournament(page, tournamentId);
  await generateRound(page, tournamentId, 1);
  const token = await publishShareUrl(page, tournamentId, { allowResultInput: true });

  // 参加者: ログインなし・モバイルビューポート(375x667)の別コンテキスト
  const phone = await browser.newContext({ viewport: { width: 375, height: 667 } });
  const phonePage = await phone.newPage();
  await phonePage.goto(`http://localhost:5173/s/${token}`);

  // 組み合わせ表で自分(卓1のplayer1)の卓を確認する
  const rounds = await fetchRounds(page, tournamentId);
  const myMatch = rounds[0].matches.find((m) => m.tableNumber === 1);
  if (!myMatch) throw new Error('卓1が見つかりません');
  await expect(phonePage.getByText(myMatch.player1.name).first()).toBeVisible();

  // 卓1の結果入力 → player1の勝ち → 登録
  await phonePage.getByRole('link', { name: '結果入力' }).first().click();
  await phonePage.getByRole('button', { name: `${myMatch.player1.name} の勝ち` }).click();
  await phonePage.getByRole('button', { name: '登録する' }).click();
  await expect(phonePage.getByText('結果を登録しました')).toBeVisible();
  await phone.close();

  // 運営者画面に反映されている(リロード後の卓1に○が表示され、APIでも確定)
  await page.goto(`/tournaments/${tournamentId}/rounds`);
  await expect(
    page.getByRole('combobox', { name: '卓1の結果' }).filter({ hasText: '○' }),
  ).toBeVisible();
  const updated = await fetchRounds(page, tournamentId);
  expect(updated[0].matches.find((m) => m.tableNumber === 1)?.result).toBe('PLAYER1_WIN');
});
