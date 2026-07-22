import { expect, test } from '@playwright/test';

import {
  confirmRound,
  createTournament,
  fetchRounds,
  generateRound,
  importParticipantsCsv,
  inputAllResults,
  loginAsOrganizer,
  publishShareUrl,
  startTournament,
} from './helpers';

/**
 * CP5: 棋力帯グループ分けの一気通貫(05_swiss_pairing_algorithm.md §2.4)。
 * グループ定義 → CSV(グループ列)インポート → 自動振り分け → 開始
 * → ラウンド生成(グループ内卓番号・グループ内BYE) → 確定 → グループ別順位表 → 共有ページ
 */
test('E2E-AC-006: CP5: グループ大会をグループ独立で運営できる', async ({ page }) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP5グループ大会 ${Date.now()}`, 3);

  // グループ管理: デフォルトの「A」が作成済み。続けて「B」を追加する(強い帯から順)
  await page.goto(`/tournaments/${tournamentId}/participants`);
  await page.getByRole('button', { name: 'グループ管理' }).click();
  await expect(
    page.getByRole('dialog', { name: 'グループ管理' }).getByText('A', { exact: true }),
  ).toBeVisible();
  await page.getByRole('textbox', { name: '新しいグループ名' }).fill('B');
  await page.getByRole('button', { name: '追加' }).click();
  await expect(
    page.getByRole('dialog', { name: 'グループ管理' }).getByText('B', { exact: true }),
  ).toBeVisible();
  await page.getByRole('button', { name: '閉じる' }).click();

  // CSVインポート(4列目のグループ列で A:3名 / B:2名 を割当)
  await importParticipantsCsv(page, tournamentId, 'participants_groups.csv', 5);
  await expect(page.getByRole('combobox', { name: '甲山 一郎のグループ' })).toHaveText('A');
  await expect(page.getByRole('combobox', { name: '戊木 五郎のグループ' })).toHaveText('B');

  // 段級位で自動振り分け(棋力の強い順に均等分割 → 端数は先頭のAへ。結果はCSVと同じ割当になる)
  await page.getByRole('button', { name: 'グループ管理' }).click();
  await page.getByRole('button', { name: '段級位で自動振り分け' }).click();
  await page.getByRole('button', { name: '振り分ける' }).click();
  await expect(
    page.getByText('段級位でグループを振り分けました。参加者一覧で個別調整できます'),
  ).toBeVisible();
  await page.getByRole('button', { name: '閉じる' }).click();
  await expect(page.getByRole('combobox', { name: '丙田 三郎のグループ' })).toHaveText('A');
  await expect(page.getByRole('combobox', { name: '丁野 四郎のグループ' })).toHaveText('B');

  await startTournament(page, tournamentId);

  // 第1ラウンド: グループごとに独立生成。A=3名(1卓+BYE)/ B=2名(1卓)、卓番号はグループ内1始まり
  await generateRound(page, tournamentId, 1);
  await expect(page.getByRole('heading', { name: 'A', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'B', exact: true })).toBeVisible();
  await expect(page.getByRole('combobox', { name: '卓A-1の結果' })).toBeVisible();
  await expect(page.getByRole('combobox', { name: '卓B-1の結果' })).toBeVisible();

  const round1 = (await fetchRounds(page, tournamentId))[0];
  const groupNameOf = (m: (typeof round1.matches)[number]) => m.group?.name ?? '';
  expect(round1.matches).toHaveLength(3);
  // BYEはA(奇数人数)側のみ。グループをまたぐ対局はない
  const byes = round1.matches.filter((m) => m.player2 === null);
  expect(byes).toHaveLength(1);
  expect(groupNameOf(byes[0])).toBe('A');
  expect(round1.matches.map(groupNameOf).sort()).toEqual(['A', 'A', 'B']);

  // 結果入力 → 確定 → 第2ラウンド(BYEがAの別の参加者に付く=BYE重複禁止がグループ内で機能)
  await inputAllResults(page);
  await confirmRound(page, 1);
  await generateRound(page, tournamentId, 2);
  const round2 = (await fetchRounds(page, tournamentId))[1];
  const bye2 = round2.matches.filter((m) => m.player2 === null);
  expect(bye2).toHaveLength(1);
  expect(groupNameOf(bye2[0])).toBe('A');
  expect(bye2[0].player1.id).not.toBe(byes[0].player1.id);

  // 順位表: グループごとに見出し付きで独立表示(A=3名 / B=2名、グループ内順位1始まり)
  await page.getByRole('link', { name: '順位' }).click();
  await expect(page.getByRole('heading', { name: 'A', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'B', exact: true })).toBeVisible();
  const lists = page.getByRole('list', { name: '順位' });
  await expect(lists).toHaveCount(2);
  await expect(lists.nth(0).getByTestId('standing-row')).toHaveCount(3); // A 3名
  await expect(lists.nth(1).getByTestId('standing-row')).toHaveCount(2); // B 2名
  await expect(
    lists.nth(0).getByTestId('standing-row').first().getByTestId('standing-rank'),
  ).toHaveText('1位');
  await expect(
    lists.nth(1).getByTestId('standing-row').first().getByTestId('standing-rank'),
  ).toHaveText('1位');

  // 共有ページ: 組み合わせ(グループ見出し+A-1卓)と順位表(グループ別)
  const token = await publishShareUrl(page, tournamentId, { allowResultInput: false });
  await page.goto(`/s/${token}`);
  await expect(page.getByRole('heading', { name: 'A', exact: true })).toBeVisible();
  await expect(page.getByText('A-1卓')).toBeVisible();
  await page.getByRole('tab', { name: '順位表' }).click();
  await expect(page.getByRole('heading', { name: 'B', exact: true })).toBeVisible();
  await expect(page.getByRole('list', { name: '順位' })).toHaveCount(2);
});
