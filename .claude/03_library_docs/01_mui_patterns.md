# Material-UI (MUI v5) 実装パターン集

> MUIでハマりやすいポイントと、このプロジェクトでの標準パターン。

---

## 1. テーマ設定の標準形

```typescript
// src/theme/index.ts
import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: { main: '#1B5E43', dark: '#0F3D2A', light: '#E8F2ED' },
    // ... 01_design_principles.md のトークンを反映
  },
  typography: {
    fontFamily: '"Noto Sans JP", "Hiragino Sans", "Yu Gothic", sans-serif',
  },
  shape: { borderRadius: 8 },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: { root: { textTransform: 'none' } }, // 英字の勝手な大文字化を防ぐ
    },
  },
});
```

```tsx
// main.tsx — CssBaselineを忘れない
<ThemeProvider theme={theme}>
  <CssBaseline />
  <App />
</ThemeProvider>
```

### ハマりポイント

- `Button` はデフォルトで `text-transform: uppercase`。**必ず `textTransform: 'none'` を設定**(上記のようにテーマで一括)
- 日本語フォントを設定しないとOSごとに見た目がバラつく。テーマの `fontFamily` 必須

---

## 2. import の注意

```typescript
// OK: バレルimport(Vite + MUI v5 はtree-shakingされる)
import { Button, Dialog } from '@mui/material';

// アイコンは個別import必須(バレルは開発サーバーが激重になる)
import DeleteIcon from '@mui/icons-material/Delete';
// NG: import { Delete } from '@mui/icons-material';
```

---

## 3. sx prop の使い方

```tsx
// OK: テーマ経由の値
<Box sx={{ p: 2, bgcolor: 'primary.light', borderRadius: 2 }} />

// OK: レスポンシブ値
<Box sx={{ display: { xs: 'block', md: 'flex' }, gap: 2 }} />

// NG: 生の色・px指定
<Box sx={{ padding: '16px', backgroundColor: '#E8F2ED' }} />
```

- `sx` の `p: 2` は `theme.spacing(2)` = 16px
- 同じ `sx` の組み合わせを3箇所以上で使ったら `ui/` コンポーネント化する

---

## 4. Dialog の標準パターン

```tsx
// 破壊的操作の確認(components/ui/ConfirmDialog.tsx)
<Dialog open={open} onClose={onCancel} maxWidth="xs" fullWidth>
  <DialogTitle>ラウンドを確定しますか?</DialogTitle>
  <DialogContent>
    <DialogContentText>確定後は結果を変更できません。</DialogContentText>
  </DialogContent>
  <DialogActions>
    <Button variant="outlined" onClick={onCancel}>キャンセル</Button>
    <Button variant="contained" onClick={onConfirm} disabled={loading}>確定する</Button>
  </DialogActions>
</Dialog>
```

### ハマりポイント

- ダイアログ内フォームの状態は、ダイアログを閉じても残る(unmountされない)。
  → `TransitionProps={{ onExited: reset }}` でリセット、または `key` を変えて強制再マウント
- スマホでは `fullWidth` + `maxWidth="xs"` を基本にする(端まで広がりすぎ防止)

---

## 5. React Hook Form との接続

```tsx
<Controller
  name="name"
  control={control}
  rules={{ required: '大会名は必須です', maxLength: { value: 100, message: '100文字以内' } }}
  render={({ field, fieldState }) => (
    <TextField
      {...field}
      label="大会名"
      error={!!fieldState.error}
      helperText={fieldState.error?.message}
      fullWidth
    />
  )}
/>
```

- `TextField` を非制御(`register`)で使うとMUIのラベル挙動が壊れることがある。**必ず `Controller` 経由**

---

## 6. Table のスマホ対応

- `TableContainer` で包み `overflow-x: auto` にする(ページ全体の横スクロール禁止)
- 列を減らす対応は `sx={{ display: { xs: 'none', sm: 'table-cell' } }}` で列単位に非表示
- 1行の情報量が多い場合はスマホのみカード表示に切り替える(`useMediaQuery`)

---

## 7. Snackbar の一元管理

- 各コンポーネントで `<Snackbar>` をレンダリングしない
- `SnackbarProvider`(Context)+ `useSnackbar()` フックを自作し、アプリで1つの Snackbar を使い回す
- 同時に複数表示しない(後発が上書き)
