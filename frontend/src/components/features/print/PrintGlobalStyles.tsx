import { GlobalStyles } from '@mui/material';

export interface PrintGlobalStylesProps {
  orientation: 'portrait' | 'landscape';
}

/**
 * 印刷ページの `@page`/`@media print` を適用する。ルート単位で1帳票=1向きが確定しているため、
 * 各印刷ページが自身の向きでこれを1回レンダリングする(04_layout_system.md §5)。
 *
 * `@page` の margin は 0 にする。Chrome/Edge はブラウザ側のヘッダー・フッター(日付・タイトル・URL・
 * ページ番号)を `@page` の余白部分に描画する仕様で、ページからは無効化できない(印刷ダイアログの
 * 「ヘッダーとフッター」設定でしか消せない)。margin=0 にすると描画の余地が無くなり出なくなるため、
 * 実際の余白は `body` の padding として自前で確保する
 */
export function PrintGlobalStyles({ orientation }: PrintGlobalStylesProps) {
  return (
    <GlobalStyles
      styles={(theme) => ({
        '@page': { size: `A4 ${orientation}`, margin: 0 },
        '@media print': {
          'html, body': {
            background: '#fff',
            fontSize: theme.print.bodyFontSize,
            padding: theme.print.pageMargin,
            // Chrome/Edgeの既定は背景非印刷。罫線・ヘッダー地色を確実に出す
            printColorAdjust: 'exact',
            WebkitPrintColorAdjust: 'exact',
          },
          // 表ヘッダーを各ページに繰り返す(MUI TableHead → <thead>)
          thead: { display: 'table-header-group' },
          tr: { breakInside: 'avoid' },
        },
      })}
    />
  );
}
