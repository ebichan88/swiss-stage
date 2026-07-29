import    { createTheme } from '@mui/material/styles';

interface RankPaletteColor {
  main: string;
  background: string;
}

/** 帳票印刷(A4)専用のトークン。mm/pt は8pxグリッドで表現できないため独立トークンとして持つ */
interface PrintTokens {
  /** @page の余白 */
  pageMargin: string;
  /** 対局カード間の切り取り余白 */
  cardGap: string;
  /** 名簿・対戦結果表の本文サイズ */
  bodyFontSize: string;
  /** 名簿・対戦結果表のセルサイズ */
  tableFontSize: string;
  /** 対局カードの本文サイズ(A8相当は文字が小さくなるため別トークン) */
  cardFontSize: string;
  /** 表ヘッダーの地色。緑ベタ塗りはインクを食うためモノクロ運用にする */
  headerBg: string;
  /** 手書き記入欄を持つ表(対戦結果表)のデータ行の高さ */
  writableRowHeight: string;
  /** 対局カード(団体戦)の手書き記入行の高さ */
  cardWritableRowHeight: string;
  /** 対戦結果表の「相手」列の幅(No.の数字のみで足りるため狭くし、結果列を相対的に広くする) */
  opponentColWidth: string;
  /** 帳票ヘッダーの開催日が未設定のときに手書き用に確保する記入枠(下線)の幅 */
  eventDateBlankWidth: string;
}

declare module '@mui/material/styles' {
  interface Palette {
    rank: { gold: RankPaletteColor; silver: RankPaletteColor; bronze: RankPaletteColor };
  }
  interface PaletteOptions {
    rank?: { gold: RankPaletteColor; silver: RankPaletteColor; bronze: RankPaletteColor };
  }
  interface Theme {
    print: PrintTokens;
  }
  interface ThemeOptions {
    print?: PrintTokens;
  }
}

/**
 * デザイントークンの実装。
 * 値の定義: .claude/02_design_system/01_design_principles.md(変更時は同期すること)
 */
export const theme = createTheme({
  palette: {
    primary: { main: '#1B5E43', dark: '#0F3D2A', light: '#E8F2ED' },
    secondary: { main: '#8D6E3F' },
    success: { main: '#2E7D32' },
    error: { main: '#C62828' },
    warning: { main: '#EF6C00' },
    info: { main: '#1565C0' },
    text: { primary: '#1A1A1A', secondary: '#5F6368' },
    background: { default: '#FAFAF7', paper: '#FFFFFF' },
    divider: '#E0E0DC',
    rank: {
      gold: { main: '#F5C542', background: '#FBE8A6' },
      silver: { main: '#9AA0A6', background: '#E4E5E7' },
      bronze: { main: '#C97A3D', background: '#EFD3BC' },
    },
  },
  print: {
    pageMargin: '10mm',
    cardGap: '4mm',
    bodyFontSize: '10pt',
    tableFontSize: '9pt',
    cardFontSize: '7pt',
    headerBg: '#EEEEEE',
    writableRowHeight: '14mm',
    cardWritableRowHeight: '8mm',
    opponentColWidth: '10mm',
    eventDateBlankWidth: '30mm',
  },
  typography: {
    fontFamily: '"Noto Sans JP", "Hiragino Sans", "Yu Gothic", sans-serif',
    h1: { fontSize: '2rem', fontWeight: 700 },
    h2: { fontSize: '1.5rem', fontWeight: 700 },
    h3: { fontSize: '1.25rem', fontWeight: 600 },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { textTransform: 'none' },
      },
    },
    MuiCssBaseline: {
      // 縦スクロールバーの有無でページ幅が変わり、中央寄せされたコンテナ
      // (TournamentLayoutのタイトル等)の左位置がページごとにズレるのを防ぐ
      styleOverrides: {
        html: { scrollbarGutter: 'stable' },
      },
    },
  },
});
