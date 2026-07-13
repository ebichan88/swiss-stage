import { describe, expect, it } from 'vitest';

import { formatDateTime, formatPoints } from '../../../src/utils/format';

describe('formatPoints', () => {
  it('整数はそのまま表示する', () => {
    expect(formatPoints(3)).toBe('3');
    expect(formatPoints(0)).toBe('0');
  });

  it('0.5刻みは小数1桁で表示する', () => {
    expect(formatPoints(2.5)).toBe('2.5');
  });
});

describe('formatDateTime', () => {
  it('ISO8601文字列を日本語表記にする', () => {
    expect(formatDateTime('2026-07-12T10:00:00+09:00')).toMatch(/2026\/7\/12/);
  });

  it('不正な値は空文字(表示を壊さない)', () => {
    expect(formatDateTime('invalid')).toBe('');
  });
});
