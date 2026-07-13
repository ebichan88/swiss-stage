import LogoutIcon from '@mui/icons-material/Logout';
import { AppBar, Box, Button, Toolbar, Typography } from '@mui/material';
import { Outlet, Link, useNavigate } from 'react-router-dom';

import { useAuth, useLogout } from '../../hooks/useAuth';
import { useSnackbar } from '../../hooks/useSnackbar';
import { paths } from '../../routes';

/** 運営者画面の共通レイアウト(AppBar: ロゴ / ユーザー名 / ログアウト) */
export function AppLayout() {
  const { user } = useAuth();
  const logoutMutation = useLogout();
  const navigate = useNavigate();
  const { showError } = useSnackbar();

  const handleLogout = () => {
    logoutMutation.mutate(undefined, {
      onSuccess: () => navigate(paths.top),
      onError: () => showError('ログアウトに失敗しました'),
    });
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky">
        <Toolbar>
          <Typography
            variant="h3"
            component={Link}
            to={paths.tournaments}
            sx={{ color: 'inherit', textDecoration: 'none', flexGrow: 1 }}
          >
            Swiss Stage
          </Typography>
          <Typography variant="body2" sx={{ mr: 2, display: { xs: 'none', sm: 'block' } }}>
            {user?.name}
          </Typography>
          <Button
            color="inherit"
            startIcon={<LogoutIcon />}
            onClick={handleLogout}
            disabled={logoutMutation.isPending}
          >
            ログアウト
          </Button>
        </Toolbar>
      </AppBar>
      <Outlet />
    </Box>
  );
}
