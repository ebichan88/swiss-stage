import LeaderboardIcon from '@mui/icons-material/Leaderboard';
import { Box, Typography } from '@mui/material';

import { StandingsTable } from '../components/features/standing/StandingsTable';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useStandings } from '../hooks/useStandings';

/** S08 順位表。順位は保存されずバックエンドで都度計算される */
export function StandingsPage() {
  const tournament = useTournamentContext();
  const { data: standings, isPending, isError, refetch } = useStandings(tournament.id);

  return (
    <Box>
      <Typography variant="h3" component="h2" sx={{ mb: 2 }}>
        順位表
        {tournament.status === 'IN_PROGRESS' && (
          <Typography variant="body2" color="text.secondary" component="span" sx={{ ml: 1 }}>
            (第{tournament.currentRound}ラウンド時点)
          </Typography>
        )}
      </Typography>
      {isPending && <LoadingState />}
      {isError && (
        <ErrorState message="順位表の取得に失敗しました" onRetry={() => void refetch()} />
      )}
      {standings && standings.length === 0 && (
        <EmptyState
          icon={<LeaderboardIcon fontSize="inherit" />}
          message="順位はまだありません。ラウンドを確定すると表示されます"
        />
      )}
      {standings && standings.length > 0 && <StandingsTable standings={standings} />}
    </Box>
  );
}
