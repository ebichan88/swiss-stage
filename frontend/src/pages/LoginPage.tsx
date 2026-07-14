import GoogleIcon from '@mui/icons-material/Google';
import LoginIcon from '@mui/icons-material/Login';
import { Alert, Box, Button, CircularProgress, Container, Typography } from '@mui/material';
import { Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';

import { FullPageSpinner } from '../components/ui/QueryStates';
import { useAuth, useTestLogin } from '../hooks/useAuth';
import { useSnackbar } from '../hooks/useSnackbar';
import { paths } from '../routes';

/** Google OAuth2の開始URL(バックエンドが認可フローへリダイレクトする) */
const GOOGLE_LOGIN_URL = '/api/v1/auth/login';

/**
 * S02 ログイン。Google OAuth2(本番)+ 開発用ログイン(DEVビルドのみ。
 * バックエンドの local/test プロファイル限定エンドポイントを使う)
 */
export function LoginPage() {
  const { user, isLoading } = useAuth();
  const testLoginMutation = useTestLogin();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { showError } = useSnackbar();

  // ログイン前に開いていたページへ戻す(ディープリンク維持)
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname;
  const redirectTo = from ?? paths.tournaments;
  const oauthFailed = searchParams.get('error') === 'oauth';

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
        {oauthFailed && (
          <Alert severity="error" sx={{ mb: 3, textAlign: 'left' }}>
            Googleログインに失敗しました。再度お試しください。
          </Alert>
        )}
        {/* OAuth2はSPA内遷移でなくバックエンドへのフルページ遷移が必要なため <a> にする */}
        <Button
          variant="contained"
          size="large"
          startIcon={<GoogleIcon />}
          component="a"
          href={GOOGLE_LOGIN_URL}
        >
          Googleでログイン
        </Button>
        {import.meta.env.DEV && (
          <Box sx={{ mt: 3 }}>
            <Button
              variant="outlined"
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
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              開発ビルド限定(backendのlocalプロファイルが必要)
            </Typography>
          </Box>
        )}
      </Box>
    </Container>
  );
}
