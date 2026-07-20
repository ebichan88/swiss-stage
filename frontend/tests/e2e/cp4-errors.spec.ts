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
 * CP4: 異常系(大会当日に起きること)(12_e2e_test_design.md)。
 * - 未入力があるラウンドは確定できない(警告表示)
 * - 同じ対局への同時入力は後勝ちにならず409エラー表示
 * - 無効化した共有トークンはエラーページ
 */

test('E2E-AC-005: CP4-1: 結果未入力の対局があるとラウンドを確定できず警告が表示される', async ({
  page,
}) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP4未入力 ${Date.now()}`, 3);
  await importParticipantsCsv(page, tournamentId, 'participants_16.csv', 16);
  await startTournament(page, tournamentId);
  await generateRound(page, tournamentId, 1);

  await expect(
    page.getByText(
      '全対局の結果を入力するとラウンドを確定できます。確定すると次のラウンドを生成できます。',
    ),
  ).toBeVisible();
  await expect(page.getByText(/未入力 8件/)).toBeVisible();
  await expect(page.getByRole('button', { name: '第1ラウンドを確定する' })).toBeDisabled();
});

test('E2E-AC-005: CP4-2: 2つのブラウザで同じ対局に別の結果を入力すると後勝ちにならず409になる', async ({
  page,
  browser,
}) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP4競合 ${Date.now()}`, 3);
  await importParticipantsCsv(page, tournamentId, 'participants_16.csv', 16);
  await startTournament(page, tournamentId);
  await generateRound(page, tournamentId, 1);

  // 2つ目のブラウザ(同じ運営者)。ラウンド取得を最初のレスポンスで固定し、
  // 「古い画面を開いたまま」の状態を決定的に再現する
  const staleContext = await browser.newContext();
  const stalePage = await staleContext.newPage();
  await loginAsOrganizer(stalePage);
  let cachedRounds: string | null = null;
  await stalePage.route('**/api/v1/tournaments/*/rounds', async (route) => {
    if (cachedRounds !== null) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: cachedRounds,
      });
      return;
    }
    const response = await route.fetch();
    cachedRounds = await response.text();
    await route.fulfill({ response, body: cachedRounds });
  });
  await stalePage.goto(`/tournaments/${tournamentId}/rounds`);
  await expect(stalePage.getByRole('combobox', { name: '卓1の結果' })).toBeVisible();

  // ブラウザA: 卓1に「player1の勝ち」を入力(versionが進む)
  await page.goto(`/tournaments/${tournamentId}/rounds`);
  await page.getByRole('combobox', { name: '卓1の結果' }).click();
  await page.getByRole('option', { name: /^○/ }).first().click();
  await expect(page.getByRole('option', { name: /^○/ }).first()).toBeHidden();
  await expect(page.getByText(/未入力 7件/)).toBeVisible();

  // ブラウザB(古い画面): 同じ卓1に別の結果 → 409のエラー表示、後勝ちしない
  await stalePage.getByRole('combobox', { name: '卓1の結果' }).click();
  await stalePage.getByRole('option', { name: /^○/ }).nth(1).click();
  await expect(
    stalePage.getByText('ほかの端末で更新されました。画面を更新して再度お試しください'),
  ).toBeVisible();
  await staleContext.close();

  // 結果はブラウザAの入力のまま(後勝ちになっていない)
  const rounds = await fetchRounds(page, tournamentId);
  expect(rounds[0].matches.find((m) => m.tableNumber === 1)?.result).toBe('PLAYER1_WIN');
});

test('E2E-AC-005: CP4-3: 再発行で無効化した共有トークンはエラーページになる', async ({
  page,
  browser,
}) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP4無効トークン ${Date.now()}`, 3);
  await importParticipantsCsv(page, tournamentId, 'participants_16.csv', 16);
  await startTournament(page, tournamentId);
  const oldToken = await publishShareUrl(page, tournamentId, { allowResultInput: false });

  // 参加者が旧トークンで閲覧できることを確認してから、運営者が再発行する
  const phone = await browser.newContext({ viewport: { width: 375, height: 667 } });
  const phonePage = await phone.newPage();
  await phonePage.goto(`http://localhost:5173/s/${oldToken}`);
  await expect(phonePage.getByRole('tab', { name: '組み合わせ' })).toBeVisible();

  await page.getByRole('button', { name: '再発行する' }).click();
  await page.getByRole('dialog').getByRole('button', { name: '再発行する' }).click();
  await expect(page.getByText('共有URLを再発行しました')).toBeVisible();

  // 旧トークンでのアクセスはエラーページ
  await phonePage.reload();
  await expect(phonePage.getByRole('heading', { name: 'このURLは無効です' })).toBeVisible();
  await phone.close();
});
