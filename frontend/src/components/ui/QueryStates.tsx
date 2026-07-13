import { Box, Button, CircularProgress, Typography } from '@mui/material';

/** 一覧系の「読み込み中」表示(10_frontend_design.md §2: 3状態必須) */
export function LoadingState() {
  return (
    <Box
      sx={{ py: 8, display: 'flex', justifyContent: 'center' }}
      role="status"
      aria-label="読み込み中"
    >
      <CircularProgress />
    </Box>
  );
}

export interface ErrorStateProps {
  message?: string;
  onRetry: () => void;
}

/** 一覧系の「エラー(再試行付き)」表示 */
export function ErrorState({ message = 'データの取得に失敗しました', onRetry }: ErrorStateProps) {
  return (
    <Box sx={{ py: 8, textAlign: 'center' }}>
      <Typography variant="body1" color="error" gutterBottom>
        {message}
      </Typography>
      <Button variant="outlined" onClick={onRetry} sx={{ mt: 1 }}>
        再試行
      </Button>
    </Box>
  );
}

/** 認証判定中などの全画面スピナー */
export function FullPageSpinner() {
  return (
    <Box
      sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
      role="status"
      aria-label="読み込み中"
    >
      <CircularProgress />
    </Box>
  );
}
