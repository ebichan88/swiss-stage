import GoogleIcon from '@mui/icons-material/Google';
import LoginIcon from '@mui/icons-material/Login';
import { Alert, Box, Button, Container, Paper, Typography } from '@mui/material';
import type { Meta, StoryObj } from '@storybook/react-vite';

/**
 * S02 ログイン。Plan PR時点のプレースホルダー実装(Issue #173、
 * `04_development_process.md` §5.1の例外)。`LoginPage.tsx` 本体はまだ旧レイアウトのため、
 * ここに提案レイアウトを直接書く。実装PRで `LoginPage.tsx` を書き換え、このストーリーを
 * 実importへ差し替える(`.claude/07_plans/11_login_top_redesign.md` 参照)。
 *
 * 認証状態のロード中(`FullPageSpinner`)・ログイン済みリダイレクトは既存ロジックを維持するため
 * ここでは扱わない(§2 画面シナリオ参照)。
 */
interface LoginPagePlaceholderProps {
  /** OAuth認可失敗時のエラー表示(`?error=oauth`) */
  oauthFailed?: boolean;
  /** 開発ビルド限定の開発用ログインボタン(`import.meta.env.DEV`) */
  showDevLogin?: boolean;
}

function LoginPagePlaceholder({ oauthFailed, showDevLogin }: LoginPagePlaceholderProps) {
  return (
    <Container component="main" maxWidth="sm">
      <Box sx={{ py: 8, textAlign: 'center' }}>
        <Box
          component="img"
          src="/swiss-stage.svg"
          alt=""
          sx={{ height: 56, width: 56, mb: 2, borderRadius: 1 }}
        />
        <Typography variant="h1" gutterBottom>
          Swiss Stage
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          運営者としてログインしてください
        </Typography>

        <Paper
          variant="outlined"
          sx={{
            p: 4,
            borderTop: 3,
            borderColor: 'primary.main',
            textAlign: 'left',
          }}
        >
          {oauthFailed && (
            <Alert severity="error" sx={{ mb: 3 }}>
              Googleログインに失敗しました。再度お試しください。
            </Alert>
          )}
          <Button
            variant="contained"
            size="large"
            fullWidth
            startIcon={<GoogleIcon />}
            component="a"
            href="/api/v1/auth/login"
          >
            Googleでログイン
          </Button>
          {showDevLogin && (
            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Button variant="outlined" fullWidth startIcon={<LoginIcon />}>
                開発用ログイン
              </Button>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                開発ビルド限定(backendのlocalプロファイルが必要)
              </Typography>
            </Box>
          )}
        </Paper>
      </Box>
    </Container>
  );
}

const meta: Meta<typeof LoginPagePlaceholder> = {
  title: 'pages/LoginPage',
  component: LoginPagePlaceholder,
};

export default meta;

type Story = StoryObj<typeof LoginPagePlaceholder>;

/** 通常(提案レイアウト。§3 UI仕様参照) */
export const Default: Story = {
  args: { showDevLogin: true },
};

/** OAuth認可失敗時のエラー表示(AUTH-AC-007) */
export const OAuthError: Story = {
  args: { oauthFailed: true, showDevLogin: true },
};
