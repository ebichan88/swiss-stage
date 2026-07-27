/** 画面には出すが印刷しない(戻るボタン・印刷ボタン等) */
export const screenOnlySx = { '@media print': { display: 'none' } } as const;

/** 直後で改ページ(グループ区切り・シート区切り) */
export const breakAfterPageSx = { '@media print': { breakAfter: 'page' } } as const;

/** 途中で切らない(対局カード1枚・表の1行) */
export const avoidBreakSx = { breakInside: 'avoid' } as const;

/**
 * 手書き記入用の表に格子状の罫線を引く。MUI Tableの既定(下線のみ・divider色)では
 * 記入欄の境界が薄すぎて見えないため、全辺に濃色の罫線を引く
 */
export const writableGridSx = {
  '& td, & th': { border: '1px solid', borderColor: 'text.primary' },
} as const;
