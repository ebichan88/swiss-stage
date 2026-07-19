import LeaderboardIcon from '@mui/icons-material/Leaderboard';
import { Box, Typography } from '@mui/material';

import { StandingsTable } from '../components/features/standing/StandingsTable';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useStandings } from '../hooks/useStandings';

/** S08 順位表。順位は保存されずバックエンドで都度計算される。グループ大会はグループごとに表示 */
export function StandingsPage() {
  const tournament = useTournamentContext();
  const { data: groupStandings, isPending, isError, refetch } = useStandings(tournament.id);

  const isEmpty = groupStandings?.every((g) => g.standings.length === 0) ?? false;

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
      {groupStandings && isEmpty && (
        <EmptyState
          icon={<LeaderboardIcon fontSize="inherit" />}
          message="順位はまだありません。ラウンドを確定すると表示されます"
        />
      )}
      {groupStandings &&
        !isEmpty &&
        groupStandings.map(({ group, standings }) => (
          <Box key={group.id} sx={{ mb: 3 }}>
            {groupStandings.length > 1 && (
              <Typography variant="h4" component="h3" sx={{ mb: 1 }}>
                {group.name}
              </Typography>
            )}
            <StandingsTable standings={standings} />
          </Box>
        ))}
    </Box>
  );
}
