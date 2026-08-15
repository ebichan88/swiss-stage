import LeaderboardIcon from '@mui/icons-material/Leaderboard';
import { Box, Typography } from '@mui/material';

import { TeamRankingBoard } from '../components/features/team/TeamRankingBoard';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useTeamRounds } from '../hooks/useTeamRounds';
import { useTeamStandings } from '../hooks/useTeamStandings';

/** S08 順位表(団体戦)。順位は保存されずバックエンドで都度計算される。個人名は含めない */
export function TeamStandingsPage() {
  const tournament = useTournamentContext();
  const {
    data: groupStandings,
    isPending: standingsPending,
    isError: standingsError,
    refetch: refetchStandings,
  } = useTeamStandings(tournament.id);
  const {
    data: rounds,
    isPending: roundsPending,
    isError: roundsError,
    refetch: refetchRounds,
  } = useTeamRounds(tournament.id);

  const isPending = standingsPending || roundsPending;
  const isError = standingsError || roundsError;
  // ラウンド1確定前は全員rank=1で返るため、確定済みラウンドが1つもない間は順位表を出さない
  const round1Confirmed = rounds?.some((r) => r.status === 'CONFIRMED') ?? false;
  const isEmpty =
    !round1Confirmed || (groupStandings?.every((g) => g.standings.length === 0) ?? false);

  return (
    <Box>
      <Typography
        variant="h3"
        component="h2"
        sx={{ mb: 2, borderLeft: 4, borderColor: 'primary.main', pl: 1.5 }}
      >
        順位
        {tournament.status === 'IN_PROGRESS' && (
          <Typography variant="body2" color="text.secondary" component="span" sx={{ ml: 1 }}>
            (第{tournament.currentRound}ラウンド時点)
          </Typography>
        )}
      </Typography>
      {isPending && <LoadingState />}
      {isError && (
        <ErrorState
          message="順位表の取得に失敗しました"
          onRetry={() => {
            void refetchStandings();
            void refetchRounds();
          }}
        />
      )}
      {!isPending && !isError && groupStandings && isEmpty && (
        <EmptyState
          icon={<LeaderboardIcon fontSize="inherit" />}
          message="順位はまだありません。ラウンドを確定すると表示されます"
        />
      )}
      {!isPending &&
        !isError &&
        groupStandings &&
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
