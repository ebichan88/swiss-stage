import { expect, test } from '@playwright/test';

import {
  confirmRound,
  createTeamTournament,
  fetchTeamRounds,
  generateRound,
  importTeamsCsv,
  inputAllTeamResults,
  loginAsOrganizer,
  publishShareUrl,
  startTournament,
} from './helpers';

/**
 * CP6: 団体戦の一気通貫(12_e2e_test_design.md)。
 * ログイン → 団体戦作成(3人制) → チーム+メンバーCSVインポート(4チーム)
 * → 開始 → 第1R生成 → 全ボード結果を一括入力 → 確定 → 第2R生成(チームの再戦なし)
 * → 順位表・ラウンド管理に個人名(メンバー氏名)が出ないこと
 */
test('E2E-AC-007: CP6: 団体戦をログインから順位表まで一気通貫で運営できる', async ({ page }) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTeamTournament(page, `CP6団体戦 ${Date.now()}`, 3, 3);
  await importTeamsCsv(page, tournamentId, 'teams_4.csv', 4);
  await startTournament(page, tournamentId);

  // 第1ラウンド: 4チーム → 2卓 × 3ボード(3人制)
  await generateRound(page, tournamentId, 1);
  await expect(page.getByRole('combobox', { name: /卓.+の結果/ })).toHaveCount(6);
  // メンバー氏名(CSVで登録した個人名)は表示されない
  await expect(page.getByText('山田太郎')).not.toBeVisible();
  await inputAllTeamResults(page);
  await confirmRound(page, 1);

  // 第2ラウンド: 制約緩和なし(=チームの再戦なし)で生成される
  await page.getByRole('button', { name: '第2ラウンドの組み合わせを生成' }).click();
  await expect(page.getByText('第2ラウンドの組み合わせを生成しました')).toBeVisible();
  await expect(page.getByRole('tab', { name: '第2R' })).toBeVisible();
  await expect(page.getByText('制約を緩和して組み合わせを生成しました')).toBeHidden();

  // APIでもチームの再戦がないことを検証(同じ2チームの組が両ラウンドに現れない)
  const rounds = await fetchTeamRounds(page, tournamentId);
  const pairKey = (t1: string, t2: string) => [t1, t2].sort().join('|');
  const round1Pairs = new Set(
    rounds[0].matches.filter((m) => m.team2 !== null).map((m) => pairKey(m.team1.id, m.team2!.id)),
  );
  for (const match of rounds[1].matches) {
    if (match.team2 === null) continue;
    expect(round1Pairs.has(pairKey(match.team1.id, match.team2.id))).toBe(false);
  }

  // 順位表: 4チーム、個人名は出さずチーム名のみ表示する
  await page.getByRole('link', { name: '順位' }).click();
  const rows = page.getByTestId('team-standing-row');
  await expect(rows).toHaveCount(4);
  await expect(page.getByText('山田太郎')).not.toBeVisible();
  await expect(page.getByText('Aチーム')).toBeVisible();
});

/**
 * CP6補: 団体戦の共有ページから両チームが結果を自己申告し、
 * ボードごとに両者の申告が一致した時点で運営者画面に反映される
 */
test('E2E-AC-008: CP6補: 団体戦の共有ページから両チームの自己申告が一致すると結果が確定する', async ({
  page,
  browser,
}) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTeamTournament(page, `CP6共有 ${Date.now()}`, 3, 3);
  await importTeamsCsv(page, tournamentId, 'teams_4.csv', 4);
  await startTournament(page, tournamentId);
  await generateRound(page, tournamentId, 1);
  const token = await publishShareUrl(page, tournamentId, { allowResultInput: true });

  const rounds = await fetchTeamRounds(page, tournamentId);
  const myMatch = rounds[0].matches.find((m) => m.tableNumber === 1);
  if (!myMatch || myMatch.team2 === null) throw new Error('卓1のチーム対局が見つかりません');
  const boardLabels = ['主将', '副将', '三将'];

  const phone = await browser.newContext({ viewport: { width: 375, height: 667 } });
  const phonePage = await phone.newPage();

  // 組み合わせ表にチーム名のみ表示され、メンバー氏名は表示されない
  await phonePage.goto(`http://localhost:5173/s/${token}`);
  await expect(phonePage.getByText(myMatch.team1.name).first()).toBeVisible();
  await expect(phonePage.getByText('山田太郎')).not.toBeVisible();

  // team1側: 全ボード「勝ち」で申告
  await phonePage.goto(`http://localhost:5173/s/${token}/team-matches/${myMatch.id}`);
  await phonePage.getByRole('button', { name: myMatch.team1.name }).click();
  for (const label of boardLabels) {
    await phonePage.getByRole('combobox', { name: label }).click();
    await phonePage.getByRole('option', { name: '勝ち' }).click();
  }
  await phonePage.getByRole('button', { name: '確認して申告する' }).click();
  await phonePage.getByRole('button', { name: '申告する' }).click();
  await expect(phonePage.getByText('申告を送信しました')).toBeVisible();

  // team2側: team1の勝ちと一致する内容(自分から見て「負け」)で申告
  await phonePage.goto(`http://localhost:5173/s/${token}/team-matches/${myMatch.id}`);
  await phonePage.getByRole('button', { name: myMatch.team2.name }).click();
  for (const label of boardLabels) {
    await phonePage.getByRole('combobox', { name: label }).click();
    await phonePage.getByRole('option', { name: '負け' }).click();
  }
  await phonePage.getByRole('button', { name: '確認して申告する' }).click();
  await phonePage.getByRole('button', { name: '申告する' }).click();
  await expect(phonePage.getByText('申告を送信しました')).toBeVisible();
  await phone.close();

  // 運営者側: 両者の申告が一致した全ボードがteam1の勝ちで確定している
  const updated = await fetchTeamRounds(page, tournamentId);
  const updatedMatch = updated[0].matches.find((m) => m.id === myMatch.id);
  expect(updatedMatch?.boardResults.every((b) => b.result === 'PLAYER1_WIN')).toBe(true);
});
