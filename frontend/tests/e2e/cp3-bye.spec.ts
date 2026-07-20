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
 * CP3: 奇数人数・BYE(12_e2e_test_design.md)。
 * 15名で開始 → 各ラウンドにBYEがちょうど1件 → 全ラウンドを通して同一参加者にBYEが2回付かない
 */
test('E2E-AC-004: CP3: 15名の大会で全ラウンドを通してBYEが重複しない', async ({ page }) => {
  const totalRounds = 3;
  await loginAsOrganizer(page);
  const tournamentId = await createTournament(page, `CP3奇数BYE ${Date.now()}`, totalRounds);
  await importParticipantsCsv(page, tournamentId, 'participants_15.csv', 15);
  await startTournament(page, tournamentId);

  const byeParticipantIds: string[] = [];
  for (let roundNumber = 1; roundNumber <= totalRounds; roundNumber++) {
    await generateRound(page, tournamentId, roundNumber);

    // 画面上にBYE(不戦勝)がちょうど1卓表示される
    await expect(page.getByText('(不戦勝)')).toHaveCount(1);

    // BYE対象者をAPIで特定して記録
    const rounds = await fetchRounds(page, tournamentId);
    const byeMatches = rounds[roundNumber - 1].matches.filter((m) => m.player2 === null);
    expect(byeMatches).toHaveLength(1);
    byeParticipantIds.push(byeMatches[0].player1.id);

    await inputAllResults(page);
    await confirmRound(page, roundNumber);
  }

  // 同一参加者にBYEが2回付いていない(絶対制約: 05_swiss_pairing_algorithm.md)
  expect(new Set(byeParticipantIds).size).toBe(byeParticipantIds.length);
});
