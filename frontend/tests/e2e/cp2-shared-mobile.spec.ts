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
 * 共有URLにモバイルビューポートでアクセス → 自分の卓を確認 → 自己申告(あなたはどちら
 * ですか → 勝敗選択 → 確認ダイアログ → 送信)→ 両者の申告が一致した時点で運営者側に
 * 確定として反映されること(2c17b8a: 対戦結果を両者の自己申告の突き合わせで確定する方式)
 */
test('E2E-AC-003: CP2: 参加者がスマホの共有ページから結果を送信できる', async ({
  page,
  browser,
}) => {
  // 運営者: 大会準備 + 共有URL発行(結果入力許可)
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP2共有 ${Date.now()}`, 3);
  await importParticipantsCsv(page, tournamentId, 'participants_16.csv', 16);
  await startTournament(page, tournamentId);
  await generateRound(page, tournamentId, 1);
  const token = await publishShareUrl(page, tournamentId, { allowResultInput: true });

  const rounds = await fetchRounds(page, tournamentId);
  const myMatch = rounds[0].matches.find((m) => m.tableNumber === 1);
  if (!myMatch || myMatch.player2 === null) throw new Error('卓1の対局が見つかりません');

  // player1側: ログインなし・モバイルビューポート(375x667)の別コンテキスト
  const phone1 = await browser.newContext({ viewport: { width: 375, height: 667 } });
  const phone1Page = await phone1.newPage();

  // 組み合わせ表で自分(卓1のplayer1)の卓を確認する
  await phone1Page.goto(`http://localhost:5173/s/${token}`);
  await expect(phone1Page.getByText(myMatch.player1.name).first()).toBeVisible();

  // player1側: 自己申告(あなたはどちらですか → player1 → 勝ち → 確認 → 申告する)
  await phone1Page.goto(`http://localhost:5173/s/${token}/matches/${myMatch.id}`);
  await phone1Page.getByRole('button', { name: myMatch.player1.name, exact: true }).click();
  await phone1Page.getByRole('button', { name: '勝ち' }).click();
  await phone1Page.getByRole('button', { name: '申告する' }).click();
  await expect(phone1Page.getByText('申告を送信しました')).toBeVisible();
  await phone1.close();

  // まだ片方の申告のみなので運営者側は未確定のまま
  const afterFirstReport = await fetchRounds(page, tournamentId);
  expect(afterFirstReport[0].matches.find((m) => m.tableNumber === 1)?.result).toBe('NONE');

  // player2側: player1の勝ちと一致する内容(自分から見て「負け」)で申告
  const phone2 = await browser.newContext({ viewport: { width: 375, height: 667 } });
  const phone2Page = await phone2.newPage();
  await phone2Page.goto(`http://localhost:5173/s/${token}/matches/${myMatch.id}`);
  await phone2Page.getByRole('button', { name: myMatch.player2.name, exact: true }).click();
  await phone2Page.getByRole('button', { name: '負け', exact: true }).click();
  await phone2Page.getByRole('button', { name: '申告する' }).click();
  await expect(phone2Page.getByText('申告を送信しました')).toBeVisible();
  await phone2.close();

  // 運営者画面に反映されている(リロード後の卓1に○が表示され、APIでも確定)
  await page.goto(`/tournaments/${tournamentId}/rounds`);
  await expect(
    page.getByRole('combobox', { name: '卓1の結果' }).filter({ hasText: '○' }),
  ).toBeVisible();
  const updated = await fetchRounds(page, tournamentId);
  expect(updated[0].matches.find((m) => m.tableNumber === 1)?.result).toBe('PLAYER1_WIN');
});
