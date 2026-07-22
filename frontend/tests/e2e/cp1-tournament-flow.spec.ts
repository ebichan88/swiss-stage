import { expect, test } from '@playwright/test';

import {
  confirmRound,
  createTournament,
  fetchRounds,
  generateRound,
  importParticipantsCsv,
  inputAllResults,
  loginAsOrganizer,
  startTournament,
} from './helpers';

/**
 * CP1: 大会運営の一気通貫(12_e2e_test_design.md)。
 * ログイン → 大会作成 → CSVインポート(16名) → 開始 → 第1R生成 → 全結果入力
 * → 確定 → 第2R生成(再戦なし) → 順位表(勝点順・同点はSOS順)
 */
test('E2E-AC-001: CP1: ログインから順位表まで大会運営を一気通貫できる', async ({ page }) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP1一気通貫 ${Date.now()}`, 5);
  await importParticipantsCsv(page, tournamentId, 'participants_16.csv', 16);
  await startTournament(page, tournamentId);

  // 第1ラウンド: 16名 → 8卓
  await generateRound(page, tournamentId, 1);
  await expect(page.getByRole('combobox', { name: /卓\d+の結果/ })).toHaveCount(8);
  await inputAllResults(page);
  await confirmRound(page, 1);

  // 第2ラウンド: 制約緩和なし(=再戦なし)で生成される
  await page.getByRole('button', { name: '第2ラウンドの組み合わせを生成' }).click();
  await expect(page.getByText('第2ラウンドの組み合わせを生成しました')).toBeVisible();
  await expect(page.getByRole('tab', { name: '第2R' })).toBeVisible();
  await expect(page.getByText('制約を緩和して組み合わせを生成しました')).toBeHidden();

  // APIでも再戦がないことを検証(同じ2人の組が両ラウンドに現れない)
  const rounds = await fetchRounds(page, tournamentId);
  const pairKey = (p1: string, p2: string) => [p1, p2].sort().join('|');
  const round1Pairs = new Set(
    rounds[0].matches
      .filter((m) => m.player2 !== null)
      .map((m) => pairKey(m.player1.id, m.player2!.id)),
  );
  for (const match of rounds[1].matches) {
    if (match.player2 === null) continue;
    expect(round1Pairs.has(pairKey(match.player1.id, match.player2.id))).toBe(false);
  }

  // 順位表: 16名、順位1〜16、勝点降順・同勝点はSOS降順
  await page.getByRole('link', { name: '順位' }).click();
  const rows = page.getByTestId('standing-row');
  await expect(rows).toHaveCount(16);

  const numeric = (text: string) => Number(text.replace(/[^\d.]/g, ''));
  const standings: { rank: number; wins: number; sos: number }[] = [];
  for (let i = 0; i < 16; i++) {
    const row = rows.nth(i);
    standings.push({
      rank: numeric(await row.getByTestId('standing-rank').innerText()),
      wins: numeric(await row.getByTestId('standing-wins').innerText()),
      sos: numeric(await row.getByTestId('standing-sos').innerText()),
    });
  }
  // 順位は同順位あり(1,2,2,4形式)。タイでなければ i+1、タイなら直前と同じ順位
  expect(standings[0].rank).toBe(1);
  for (let i = 1; i < standings.length; i++) {
    const prev = standings[i - 1];
    const curr = standings[i];
    expect([prev.rank, i + 1]).toContain(curr.rank);
    expect(curr.wins).toBeLessThanOrEqual(prev.wins);
    if (curr.wins === prev.wins) {
      expect(curr.sos).toBeLessThanOrEqual(prev.sos);
    }
  }
});

test('E2E-AC-002: CP1補: Shift_JISのCSVもインポートできる', async ({ page }) => {
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP1 SJIS ${Date.now()}`, 3);
  await importParticipantsCsv(page, tournamentId, 'participants_16_sjis.csv', 16);
  await expect(page.getByText('井山 太郎')).toBeVisible();
});
