import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import PrintIcon from '@mui/icons-material/Print';
import { Box, Button } from '@mui/material';
import { Link, Outlet, useParams } from 'react-router-dom';

import { useTournament } from '../../hooks/useTournaments';
import { paths } from '../../routes';
import { printPage } from '../../utils/printPage';
import { ErrorState, FullPageSpinner } from '../ui/QueryStates';
import { screenOnlySx } from '../features/print/printSx';

/**
 * 帳票印刷ページの共通レイアウト。AppLayout/TournamentLayout の外に置き、
 * 運営者画面のAppBar・サイドバー・下部タブを一切継承しない(@pageはルート単位=文書単位でしか
 * 切り替えられないため、印刷対象外のクロームが混入する余地自体を無くす)。
 */
export function PrintLayout() {
  const { id = '' } = useParams();
  const { data: tournament, isPending, isError, refetch } = useTournament(id);

  if (!id) {
    throw new Error('route misconfiguration: tournament id がありません');
  }
  if (isPending) {
    return <FullPageSpinner />;
  }
  if (isError) {
    return <ErrorState message="大会情報の取得に失敗しました" onRetry={() => void refetch()} />;
  }

  return (
    <Box>
      <Box
        sx={{
          ...screenOnlySx,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          p: 2,
          borderBottom: 1,
          borderColor: 'divider',
        }}
      >
        <Button
          component={Link}
          to={paths.tournament(id)}
          startIcon={<ArrowBackIcon />}
          variant="outlined"
        >
          大会管理に戻る
        </Button>
        <Button variant="contained" startIcon={<PrintIcon />} onClick={printPage}>
          印刷する
        </Button>
      </Box>
      <Outlet context={tournament} />
    </Box>
  );
}
