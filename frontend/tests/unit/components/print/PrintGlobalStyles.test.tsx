import { describe, expect, it } from 'vitest';

import { PrintGlobalStyles } from '../../../../src/components/features/print/PrintGlobalStyles';
import { renderWithProviders } from '../../../testUtils';

/**
 * jsdomは`@page`/`@media print`を解釈しないため、CSSの効果そのものは検証できない。
 * ただしMUIのGlobalStyles(emotion)が注入する<style>タグのテキストは読めるため、
 * 「@pageのmarginは0で、実際の余白はbodyのpaddingとして確保している」という
 * 回帰しやすい実装判断(ブラウザのヘッダー・フッター抑制)をここで固定する
 */
describe('PrintGlobalStyles', () => {
  it('@page の margin は0にし、実際の余白は body の padding として確保する', () => {
    renderWithProviders(<PrintGlobalStyles orientation="portrait" />);
    const css = Array.from(document.querySelectorAll('style'))
      .map((el) => el.textContent ?? '')
      .join('\n');

    expect(css).toMatch(/@page\s*\{[^}]*margin:\s*0/);
    expect(css).toMatch(/padding:\s*10mm/);
  });

  it('向き(portrait/landscape)が @page の size に反映される', () => {
    renderWithProviders(<PrintGlobalStyles orientation="landscape" />);
    const css = Array.from(document.querySelectorAll('style'))
      .map((el) => el.textContent ?? '')
      .join('\n');

    expect(css).toMatch(/size:\s*A4 landscape/);
  });
});
