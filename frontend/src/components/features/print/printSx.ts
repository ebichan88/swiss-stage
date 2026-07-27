/** 画面には出すが印刷しない(戻るボタン・印刷ボタン等) */
export const screenOnlySx = { '@media print': { display: 'none' } } as const;

/** 直後で改ページ(グループ区切り・シート区切り) */
export const breakAfterPageSx = { '@media print': { breakAfter: 'page' } } as const;

/** 途中で切らない(対局カード1枚・表の1行) */
export const avoidBreakSx = { breakInside: 'avoid' } as const;

/**
 * 手書き記入用の表に格子状の罫線を引き、全列を中央揃えに統一する。MUI Tableの既定
 * (下線のみ・divider色)では記入欄の境界が薄すぎて見えないため全辺に濃色の罫線を引く。
 * 列ごとにalignがNo./名前は左・ラウンド列は中央・勝点等は右とバラバラだと読みにくいため統一する
 */
export const writableGridSx = {
  '& td, & th': {
    border: '1px solid',
    borderColor: 'text.primary',
    textAlign: 'center',
  },
} as const;
