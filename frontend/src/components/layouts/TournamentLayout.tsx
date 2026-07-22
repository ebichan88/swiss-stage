import DashboardIcon from '@mui/icons-material/Dashboard';
import FormatListNumberedIcon from '@mui/icons-material/FormatListNumbered';
import GroupsIcon from '@mui/icons-material/Groups';
import LeaderboardIcon from '@mui/icons-material/Leaderboard';
import SettingsIcon from '@mui/icons-material/Settings';
import TableChartIcon from '@mui/icons-material/TableChart';
import {
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Container,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Paper,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import {
  Link,
  Outlet,
  useLocation,
  useNavigate,
  useOutletContext,
  useParams,
} from 'react-router-dom';

import { useTournament } from '../../hooks/useTournaments';
import { paths } from '../../routes';
import type { Tournament } from '../../types/tournament';
import { ErrorState, FullPageSpinner } from '../ui/QueryStates';
import { StatusBadge } from '../ui/StatusBadge';

/** 配下ページから大会情報を参照するためのOutletコンテキスト */
export function useTournamentContext(): Tournament {
  return useOutletContext<Tournament>();
}

const SIDEBAR_WIDTH = 200;

/**
 * 大会管理系画面(S05〜S09)の共通レイアウト(04_screen_transition_design.md §4)。
 * 大会名・状態バッジを常時表示し、md以上は左サイドバー / md未満は下部固定タブで移動する
 */
export function TournamentLayout() {
  const { id = '' } = useParams();
  const { data: tournament, isPending, isError, refetch } = useTournament(id);
  const location = useLocation();
  const navigate = useNavigate();
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));

  if (!id) {
    throw new Error('route misconfiguration: tournament id がありません');
  }
  if (isPending) {
    return <FullPageSpinner />;
  }
  if (isError) {
    return <ErrorState message="大会情報の取得に失敗しました" onRetry={() => void refetch()} />;
  }

  const navItems = [
    { label: '概要', icon: <DashboardIcon />, to: paths.tournament(id) },
    { label: '参加者', icon: <GroupsIcon />, to: paths.participants(id) },
    { label: 'ラウンド', icon: <FormatListNumberedIcon />, to: paths.rounds(id) },
    { label: '順位', icon: <LeaderboardIcon />, to: paths.standings(id) },
    { label: '戦績一覧', icon: <TableChartIcon />, to: paths.crossTable(id) },
    { label: '設定', icon: <SettingsIcon />, to: paths.settings(id) },
  ];
  const currentIndex = navItems.findLastIndex((item) => location.pathname.startsWith(item.to));

  return (
    <Box sx={{ display: 'flex', flexGrow: 1 }}>
      {isDesktop && (
        <Box
          component="nav"
          sx={{ width: SIDEBAR_WIDTH, flexShrink: 0, borderRight: 1, borderColor: 'divider' }}
        >
          <List>
            {navItems.map((item, index) => (
              <ListItemButton
                key={item.to}
                component={Link}
                to={item.to}
                selected={index === currentIndex}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ))}
          </List>
        </Box>
      )}
      <Container maxWidth="lg" sx={{ py: 3, pb: isDesktop ? 3 : 12 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3, flexWrap: 'wrap' }}>
          <Typography variant="h2" component="h1">
            {tournament.name}
          </Typography>
          <StatusBadge status={tournament.status} />
        </Box>
        <Outlet context={tournament} />
      </Container>
      {!isDesktop && (
        <Paper
          sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: 'appBar' }}
          elevation={3}
        >
          <BottomNavigation
            showLabels
            value={currentIndex}
            onChange={(_event, newIndex: number) => navigate(navItems[newIndex].to)}
            sx={{
              // 項目数が多いスマホでも6項目(概要・参加者・ラウンド・順位・戦績一覧・設定)が
              // 横スクロールなしで収まるよう、標準より詰めた余白にする
              '& .MuiBottomNavigationAction-root': { minWidth: 0, px: 0.5 },
              '& .MuiBottomNavigationAction-label': { fontSize: 'caption.fontSize' },
            }}
          >
            {navItems.map((item) => (
              <BottomNavigationAction key={item.to} label={item.label} icon={item.icon} />
            ))}
          </BottomNavigation>
        </Paper>
      )}
    </Box>
  );
}
