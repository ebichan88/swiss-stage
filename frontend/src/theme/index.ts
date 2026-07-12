import { createTheme } from '@mui/material/styles';

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
  },
});
