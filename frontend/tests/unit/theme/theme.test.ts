import { describe, expect, it } from 'vitest';

import { theme } from '../../../src/theme';

/** `#fff` のような3桁短縮形を6桁に展開する */
function expandHex(hex: string): string {
  if (hex.length === 4) {
    return `#${[...hex.slice(1)].map((c) => c + c).join('')}`;
  }
  return hex;
}

/** 相対輝度(WCAG 2.x)。sRGBのガンマ補正を行ってから係数を掛ける */
function relativeLuminance(hexInput: string): number {
  const hex = expandHex(hexInput);
  const [r, g, b] = [0, 2, 4].map((i) => parseInt(hex.slice(1 + i, 3 + i), 16) / 255);
  const linear = (c: number) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
  return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b);
}

/** WCAG 2.x のコントラスト比計算 */
function contrastRatio(hexA: string, hexB: string): number {
  const [l1, l2] = [relativeLuminance(hexA) + 0.05, relativeLuminance(hexB) + 0.05];
  return Math.max(l1, l2) / Math.min(l1, l2);
}

describe('theme', () => {
  it('SHR-AC-018: 共有ページ・結果入力ページの本文タイポグラフィ(body1)が16px以上である', () => {
    const fontSize = theme.typography.body1.fontSize;
    const px =
      typeof fontSize === 'number'
        ? fontSize
        : parseFloat(fontSize ?? '0') * theme.typography.htmlFontSize;
    expect(px).toBeGreaterThanOrEqual(16);
  });

  it('SHR-AC-019: テーマの文字色と背景色の主要な組み合わせがWCAG AA(4.5:1)以上のコントラスト比を満たす', () => {
    const { palette } = theme;
    const pairs: [string, string, string][] = [
      ['text.primary / background.default', palette.text.primary, palette.background.default],
      ['text.primary / background.paper', palette.text.primary, palette.background.paper],
      ['text.secondary / background.default', palette.text.secondary, palette.background.default],
      [
        'primary.contrastText / primary.main(表ヘッダー)',
        palette.primary.contrastText,
        palette.primary.main,
      ],
      ['error.main / background.paper', palette.error.main, palette.background.paper],
      ['success.main / background.paper', palette.success.main, palette.background.paper],
      ['warning.main / background.paper', palette.warning.main, palette.background.paper],
      [
        'text.primary / primary.light(共有ページのヘッダー帯)',
        palette.text.primary,
        palette.primary.light,
      ],
      [
        'text.secondary / primary.light(共有ページのヘッダー帯)',
        palette.text.secondary,
        palette.primary.light,
      ],
    ];

    for (const [label, fg, bg] of pairs) {
      expect(contrastRatio(fg, bg), `${label}: ${fg} vs ${bg}`).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('TRN-AC-024: TextField(outlined variant)の既定背景色がbackground.paperになる', () => {
    const styleOverrides = theme.components?.MuiOutlinedInput?.styleOverrides as {
      root?: { backgroundColor?: string };
    };
    expect(styleOverrides.root?.backgroundColor).toBe(theme.palette.background.paper);
  });

  it('TRN-AC-025: outlinedボタンの既定背景色がbackground.paperになり、containedボタンの配色は変更されない', () => {
    const styleOverrides = theme.components?.MuiButton?.styleOverrides as {
      root?: { backgroundColor?: string };
      outlined?: { backgroundColor?: string };
    };
    expect(styleOverrides.outlined?.backgroundColor).toBe(theme.palette.background.paper);
    expect(styleOverrides.root?.backgroundColor).toBeUndefined();
  });
});
