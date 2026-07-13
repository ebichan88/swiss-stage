import { describe, expect, it } from 'vitest';

import { RANKS_STRONGEST_FIRST } from '../../../src/types/enums';
import { rankLabel, relaxationLabel } from '../../../src/utils/labels';

describe('rankLabel', () => {
  it('級位は「n級」で表示する', () => {
    expect(rankLabel('KYU_20')).toBe('20級');
    expect(rankLabel('KYU_1')).toBe('1級');
  });

  it('初段はDAN_1、2段以上は「n段」で表示する', () => {
    expect(rankLabel('DAN_1')).toBe('初段');
    expect(rankLabel('DAN_2')).toBe('2段');
    expect(rankLabel('DAN_9')).toBe('9段');
  });

  it('nullは未入力', () => {
    expect(rankLabel(null)).toBe('未入力');
  });

  it('全29段階が一意なラベルを持つ', () => {
    const labels = RANKS_STRONGEST_FIRST.map((rank) => rankLabel(rank));
    expect(new Set(labels).size).toBe(29);
  });
});

describe('relaxationLabel', () => {
  it('既知の緩和コードは日本語の警告文になる', () => {
    expect(relaxationLabel('REMATCH')).toContain('再戦');
    expect(relaxationLabel('BYE_REPEAT')).toContain('不戦勝');
    expect(relaxationLabel('SAME_ORGANIZATION')).toContain('所属');
  });

  it('未知のコードはそのまま表示する(表示を壊さない)', () => {
    expect(relaxationLabel('UNKNOWN_CODE')).toBe('UNKNOWN_CODE');
  });
});
