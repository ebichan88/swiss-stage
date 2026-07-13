import LoginIcon from '@mui/icons-material/Login';
import { Box, Button, CircularProgress, Container, Typography } from '@mui/material';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';

import { FullPageSpinner } from '../components/ui/QueryStates';
import { useAuth, useTestLogin } from '../hooks/useAuth';
import { useSnackbar } from '../hooks/useSnackbar';
import { paths } from '../routes';

/**
 * S02 ログイン。Google OAuth2 は Phase 5 で追加する。
 * それまでは開発用ログイン(バックエンドの local/test プロファイル限定)のみ
 */
export function LoginPage() {
  const { user, isLoading } = useAuth();
  const testLoginMutation = useTestLogin();
  const navigate = useNavigate();
  const location = useLocation();
  const { showError } = useSnackbar();

  // ログイン前に開いていたページへ戻す(ディープリンク維持)
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname;
  const redirectTo = from ?? paths.tournaments;

  if (isLoading) {
    return <FullPageSpinner />;
  }
  if (user) {
    return <Navigate to={redirectTo} replace />;
  }

  const handleTestLogin = () => {
    testLoginMutation.mutate(undefined, {
      onSuccess: () => navigate(redirectTo, { replace: true }),
      onError: () => showError('ログインに失敗しました'),
    });
  };

  return (
    <Container maxWidth="sm">
      <Box sx={{ py: 8, textAlign: 'center' }}>
        <Typography variant="h1" gutterBottom>
          Swiss Stage
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          運営者としてログインしてください
        </Typography>
        <Button
          variant="contained"
          size="large"
          startIcon={
            testLoginMutation.isPending ? (
              <CircularProgress size={16} color="inherit" />
            ) : (
              <LoginIcon />
            )
          }
          onClick={handleTestLogin}
          disabled={testLoginMutation.isPending}
        >
          開発用ログイン
        </Button>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
          Googleログインは今後追加予定です
        </Typography>
      </Box>
    </Container>
  );
}
