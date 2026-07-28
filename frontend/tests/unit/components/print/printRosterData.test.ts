import { describe, expect, it } from 'vitest';

import { buildRosterRows } from '../../../../src/components/features/print/printRosterData';
import { groupOf, participantOf } from '../../../fixtures';

describe('buildRosterRows', () => {
  it('単一グループ大会は groupName が null になる', () => {
    const rows = buildRosterRows([participantOf()], [groupOf()]);
    expect(rows[0].groupName).toBeNull();
  });

  it('複数グループ大会はグループ順→entryOrder順に並び、groupName が入る', () => {
    const groupA = groupOf({ id: 'gA', name: 'A' });
    const groupB = groupOf({ id: 'gB', name: 'B' });
    const rows = buildRosterRows(
      [
        participantOf({ id: 'p3', entryOrder: 2, groupId: 'gB', name: '三郎' }),
        participantOf({ id: 'p1', entryOrder: 2, groupId: 'gA', name: '一郎' }),
        participantOf({ id: 'p2', entryOrder: 1, groupId: 'gA', name: '二郎' }),
      ],
      [groupA, groupB],
    );
    expect(rows.map((r) => r.name)).toEqual(['二郎', '一郎', '三郎']);
    expect(rows[0].groupName).toBe('A');
    expect(rows[2].groupName).toBe('B');
  });

  it('棄権(WITHDRAWN)は除外されず一覧に残る(出欠・備考欄は受付で手書き記入する)', () => {
    const rows = buildRosterRows([participantOf({ status: 'WITHDRAWN' })], [groupOf()]);
    expect(rows).toHaveLength(1);
  });

  it('段級位は rankLabel() 済み・未入力は「未入力」になる', () => {
    const rows = buildRosterRows([participantOf({ rank: null })], [groupOf()]);
    expect(rows[0].rankText).toBe('未入力');
  });
});
