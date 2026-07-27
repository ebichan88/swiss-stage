import { describe, expect, it } from 'vitest';

import { formatDateTime, formatEventDate, formatPoints } from '../../../src/utils/format';

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

describe('formatEventDate', () => {
  it('YYYY-MM-DD をゼロ埋めなしの日本語表記にする', () => {
    expect(formatEventDate('2026-08-15')).toBe('2026/8/15');
    expect(formatEventDate('2026-01-05')).toBe('2026/1/5');
  });

  it('タイムゾーンに影響されず日付がずれない(月初・月末)', () => {
    expect(formatEventDate('2026-01-01')).toBe('2026/1/1');
    expect(formatEventDate('2026-12-31')).toBe('2026/12/31');
  });

  it('未設定(null)・不正な値は空文字(表示を壊さない)', () => {
    expect(formatEventDate(null)).toBe('');
    expect(formatEventDate('2026/08/15')).toBe('');
    expect(formatEventDate('invalid')).toBe('');
  });
});
