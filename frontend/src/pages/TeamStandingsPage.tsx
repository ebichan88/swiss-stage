import LeaderboardIcon from '@mui/icons-material/Leaderboard';
import { Box, Typography } from '@mui/material';

import { TeamRankingBoard } from '../components/features/team/TeamRankingBoard';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useTeamStandings } from '../hooks/useTeamStandings';

/** S08 順位表(団体戦)。順位は保存されずバックエンドで都度計算される。個人名は含めない */
export function TeamStandingsPage() {
  const tournament = useTournamentContext();
  const { data: groupStandings, isPending, isError, refetch } = useTeamStandings(tournament.id);

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
          <Box key={group.id} sx={{ mb: 4 }}>
            {groupStandings.length > 1 && (
              <Typography variant="h4" component="h3" sx={{ mb: 1 }}>
                {group.name}
              </Typography>
            )}
            <TeamRankingBoard standings={standings} />
          </Box>
        ))}
    </Box>
  );
}
