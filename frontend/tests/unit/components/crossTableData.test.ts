import { describe, expect, it } from 'vitest';

import { buildCrossTableRows } from '../../../src/components/features/standing/crossTableData';
import { groupOf, matchOf, roundOf, standingOf, summaryOf } from '../../fixtures';

describe('buildCrossTableRows', () => {
  it('行はentryOrder昇順に並び、対局は自分の視点で相手とマークを解決する', () => {
    const taro = summaryOf({ id: 'p1', name: '架空 太郎', entryOrder: 2 });
    const hanako = summaryOf({ id: 'p2', name: '仮名 花子', entryOrder: 1 });
    const rounds = [
      roundOf({
        roundNumber: 1,
        matches: [matchOf({ player1: taro, player2: hanako, result: 'PLAYER1_WIN' })],
      }),
    ];
    const standings = [standingOf({ participant: taro }), standingOf({ participant: hanako })];

    const rows = buildCrossTableRows(rounds, standings);

    expect(rows.map((r) => r.standing.participant.id)).toEqual(['p2', 'p1']);
    // 花子(player2)視点: 相手は太郎、負け
    expect(rows[0].cells[0]).toEqual({ opponent: taro, isBye: false, mark: '●' });
    // 太郎(player1)視点: 相手は花子、勝ち
    expect(rows[1].cells[0]).toEqual({ opponent: hanako, isBye: false, mark: '○' });
  });

  it('BYEの対局は相手null・isBye trueになる', () => {
    const taro = summaryOf({ id: 'p1', entryOrder: 1 });
    const rounds = [
      roundOf({
        roundNumber: 1,
        matches: [matchOf({ player1: taro, player2: null, result: 'BYE' })],
      }),
    ];
    const standings = [standingOf({ participant: taro })];

    const rows = buildCrossTableRows(rounds, standings);

    expect(rows[0].cells[0]).toEqual({ opponent: null, isBye: true, mark: null });
  });

  it('そのラウンドに対局がない参加者は相手null・isBye falseになる(離脱・後入り等)', () => {
    const taro = summaryOf({ id: 'p1', entryOrder: 1 });
    const jiro = summaryOf({ id: 'p3', name: '試験 次郎', entryOrder: 2 });
    const rounds = [
      roundOf({
        roundNumber: 1,
        matches: [matchOf({ player1: taro, player2: summaryOf({ id: 'p2' }) })],
      }),
    ];
    const standings = [standingOf({ participant: taro }), standingOf({ participant: jiro })];

    const rows = buildCrossTableRows(rounds, standings);

    const jiroRow = rows.find((r) => r.standing.participant.id === 'p3');
    expect(jiroRow?.cells[0]).toEqual({ opponent: null, isBye: false, mark: null });
  });

  it('複数グループの対局が混在していても参加者IDで解決できる(グループ絞り込みは呼び出し側が行う前提)', () => {
    const groupA = groupOf({ id: 'gA', name: 'A' });
    const groupB = groupOf({ id: 'gB', name: 'B' });
    const aPlayer = summaryOf({ id: 'a1', entryOrder: 1 });
    const bPlayer1 = summaryOf({ id: 'b1', entryOrder: 1 });
    const bPlayer2 = summaryOf({ id: 'b2', entryOrder: 2 });
    const rounds = [
      roundOf({
        roundNumber: 1,
        matches: [
          matchOf({ group: groupA, player1: aPlayer, player2: summaryOf({ id: 'a2' }) }),
          matchOf({
            group: groupB,
            player1: bPlayer1,
            player2: bPlayer2,
            result: 'PLAYER2_WIN',
          }),
        ],
      }),
    ];
    // グループBの順位表だけを渡す
    const standings = [
      standingOf({ participant: bPlayer1 }),
      standingOf({ participant: bPlayer2 }),
    ];

    const rows = buildCrossTableRows(rounds, standings);

    expect(rows).toHaveLength(2);
    expect(rows[0].cells[0]).toEqual({ opponent: bPlayer2, isBye: false, mark: '●' });
    expect(rows[1].cells[0]).toEqual({ opponent: bPlayer1, isBye: false, mark: '○' });
  });
});
