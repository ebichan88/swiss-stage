import { describe, expect, it } from 'vitest';

import {
  buildMatchCards,
  buildTeamMatchCards,
  chunkIntoPages,
  INDIVIDUAL_CARD_LAYOUT,
  TEAM_CARD_LAYOUT,
} from '../../../../src/components/features/print/matchCardData';
import { groupOf, participantOf, teamMemberOf, teamOf } from '../../../fixtures';

describe('INDIVIDUAL_CARD_LAYOUT', () => {
  it('個人戦は2列×6行(転置レイアウトだが個人勝敗行が無く2列並べられる)', () => {
    expect(INDIVIDUAL_CARD_LAYOUT).toEqual({ columns: 2, rows: 6 });
  });
});

describe('TEAM_CARD_LAYOUT', () => {
  it('団体戦は1列×3行(横に広い転置レイアウトのため1枚をページ幅いっぱいに使う)', () => {
    expect(TEAM_CARD_LAYOUT).toEqual({ columns: 1, rows: 3 });
  });
});

describe('buildMatchCards', () => {
  it('rowCount は totalRounds になる', () => {
    const cards = buildMatchCards([participantOf()], [groupOf()], 8);
    expect(cards[0].rowCount).toBe(8);
  });

  it('PRT-AC-008,PRT-AC-012: 棄権(WITHDRAWN)は除外する', () => {
    const cards = buildMatchCards(
      [
        participantOf({ id: 'p1', status: 'ACTIVE' }),
        participantOf({ id: 'p2', status: 'WITHDRAWN' }),
      ],
      [groupOf()],
      5,
    );
    expect(cards).toHaveLength(1);
  });

  it('PRT-AC-008: グループ順→entryOrder順に並ぶ', () => {
    const groupA = groupOf({ id: 'gA', name: 'A' });
    const groupB = groupOf({ id: 'gB', name: 'B' });
    const cards = buildMatchCards(
      [
        participantOf({ id: 'p3', entryOrder: 2, groupId: 'gB', name: '三郎' }),
        participantOf({ id: 'p1', entryOrder: 2, groupId: 'gA', name: '一郎' }),
        participantOf({ id: 'p2', entryOrder: 1, groupId: 'gA', name: '二郎' }),
      ],
      [groupA, groupB],
      5,
    );
    expect(cards.map((c) => c.name)).toEqual(['二郎', '一郎', '三郎']);
  });

  it('単一グループ大会は groupName が null になる', () => {
    const cards = buildMatchCards([participantOf()], [groupOf()], 5);
    expect(cards[0].groupName).toBeNull();
  });

  it('boardLabels は空配列(個人戦にボード役割は無い)', () => {
    const cards = buildMatchCards([participantOf()], [groupOf()], 5);
    expect(cards[0].boardLabels).toEqual([]);
  });
});

describe('buildTeamMatchCards', () => {
  it('PRT-AC-007: 個人名を一切含めない(チーム名のみ)', () => {
    const team = teamOf({ members: [teamMemberOf({ name: '架空 主将' })] });
    const cards = buildTeamMatchCards([team], [groupOf()], 5, 3);
    expect(cards[0].name).toBe(team.name);
    expect(JSON.stringify(cards[0])).not.toContain('架空 主将');
  });

  it('boardLabels は teamSize 分のボード役割(主将〜)になる', () => {
    const team = teamOf({ members: [] });
    const cards = buildTeamMatchCards([team], [groupOf()], 5, 3);
    expect(cards[0].boardLabels).toEqual(['主将', '副将', '三将']);
  });

  it('PRT-AC-008,PRT-AC-012: 棄権(WITHDRAWN)チームは除外する', () => {
    const cards = buildTeamMatchCards(
      [
        teamOf({ id: 't1', status: 'ACTIVE', members: [] }),
        teamOf({ id: 't2', status: 'WITHDRAWN', members: [] }),
      ],
      [groupOf()],
      5,
      3,
    );
    expect(cards).toHaveLength(1);
  });
});

describe('chunkIntoPages', () => {
  function cardsOf(count: number, groupName: string | null = null) {
    return Array.from({ length: count }, (_, i) => ({
      entryOrder: i + 1,
      name: `参加者${i + 1}`,
      organization: null,
      rankText: '',
      groupName,
      rowCount: 5,
      boardLabels: [],
    }));
  }

  it('perPage枚ごとにページを割る', () => {
    const pages = chunkIntoPages(cardsOf(20), 16);
    expect(pages).toHaveLength(2);
    expect(pages[0]).toHaveLength(16);
    expect(pages[1]).toHaveLength(4);
  });

  it('グループ境界では枚数に余裕があっても必ずページを割る', () => {
    const groupACards = cardsOf(3, 'A');
    const groupBCards = [{ ...cardsOf(1, 'B')[0], entryOrder: 100 }];
    const pages = chunkIntoPages([...groupACards, ...groupBCards], 16);
    expect(pages).toHaveLength(2);
    expect(pages[0]).toHaveLength(3);
    expect(pages[1]).toHaveLength(1);
  });
});
