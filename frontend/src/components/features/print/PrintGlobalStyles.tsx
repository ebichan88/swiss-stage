import { GlobalStyles } from '@mui/material';

export interface PrintGlobalStylesProps {
  orientation: 'portrait' | 'landscape';
}

/**
 * 印刷ページの `@page`/`@media print` を適用する。ルート単位で1帳票=1向きが確定しているため、
 * 各印刷ページが自身の向きでこれを1回レンダリングする(04_layout_system.md §5)。
 */
export function PrintGlobalStyles({ orientation }: PrintGlobalStylesProps) {
  return (
    <GlobalStyles
      styles={(theme) => ({
        '@page': { size: `A4 ${orientation}`, margin: theme.print.pageMargin },
        '@media print': {
          'html, body': {
            background: '#fff',
            fontSize: theme.print.bodyFontSize,
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
