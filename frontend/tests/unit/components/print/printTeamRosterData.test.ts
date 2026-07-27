import { describe, expect, it } from 'vitest';

import { buildTeamRosterRows } from '../../../../src/components/features/print/printTeamRosterData';
import { groupOf, teamMemberOf, teamOf } from '../../../fixtures';

describe('buildTeamRosterRows', () => {
  it('メンバー1人につき1行になる(1チーム1行ではない)', () => {
    const team = teamOf({
      members: [
        teamMemberOf({ id: 'm1', name: '架空 主将', boardPosition: 1 }),
        teamMemberOf({ id: 'm2', name: '架空 副将', boardPosition: 2 }),
      ],
    });
    const rows = buildTeamRosterRows([team], [groupOf()]);
    expect(rows).toHaveLength(2);
    expect(rows.map((r) => r.memberName)).toEqual(['架空 主将', '架空 副将']);
  });

  it('ボード順(主将→補欠)に並ぶ', () => {
    const team = teamOf({
      members: [
        teamMemberOf({ id: 'm3', name: '補欠', boardPosition: null }),
        teamMemberOf({ id: 'm1', name: '主将', boardPosition: 1 }),
        teamMemberOf({ id: 'm2', name: '副将', boardPosition: 2 }),
      ],
    });
    const rows = buildTeamRosterRows([team], [groupOf()]);
    expect(rows.map((r) => r.memberName)).toEqual(['主将', '副将', '補欠']);
    expect(rows.map((r) => r.positionLabel)).toEqual(['主将', '副将', '補欠']);
  });

  it('単一グループ大会は groupName が null、複数グループ大会は名前が入る', () => {
    const team = teamOf({ groupId: 'gA', members: [teamMemberOf()] });
    expect(buildTeamRosterRows([team], [groupOf({ id: 'gA' })])[0].groupName).toBeNull();
    expect(
      buildTeamRosterRows(
        [team],
        [groupOf({ id: 'gA', name: 'A' }), groupOf({ id: 'gB', name: 'B' })],
      )[0].groupName,
    ).toBe('A');
  });

  it('棄権(WITHDRAWN)チームは除外されず一覧に残る(出欠・備考欄は受付で手書き記入する)', () => {
    const team = teamOf({
      status: 'WITHDRAWN',
      members: [teamMemberOf(), teamMemberOf({ id: 'm2' })],
    });
    const rows = buildTeamRosterRows([team], [groupOf()]);
    expect(rows).toHaveLength(2);
  });

  it('チーム順はグループ順→entryOrder順', () => {
    const groupA = groupOf({ id: 'gA', name: 'A' });
    const groupB = groupOf({ id: 'gB', name: 'B' });
    const rows = buildTeamRosterRows(
      [
        teamOf({
          id: 't3',
          entryOrder: 2,
          groupId: 'gB',
          name: 'チーム3',
          members: [teamMemberOf()],
        }),
        teamOf({
          id: 't1',
          entryOrder: 2,
          groupId: 'gA',
          name: 'チーム1',
          members: [teamMemberOf()],
        }),
        teamOf({
          id: 't2',
          entryOrder: 1,
          groupId: 'gA',
          name: 'チーム2',
          members: [teamMemberOf()],
        }),
      ],
      [groupA, groupB],
    );
    expect(rows.map((r) => r.teamName)).toEqual(['チーム2', 'チーム1', 'チーム3']);
  });
});
