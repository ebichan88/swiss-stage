/** 画面には出すが印刷しない(戻るボタン・印刷ボタン等) */
export const screenOnlySx = { '@media print': { display: 'none' } } as const;

/** 直後で改ページ(グループ区切り・シート区切り) */
export const breakAfterPageSx = { '@media print': { breakAfter: 'page' } } as const;

/** 途中で切らない(対局カード1枚・表の1行) */
export const avoidBreakSx = { breakInside: 'avoid' } as const;
