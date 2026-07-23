import TableChartIcon from '@mui/icons-material/TableChart';
import { Box, Typography } from '@mui/material';

import { TeamCrossTable } from '../components/features/team/TeamCrossTable';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useTeamRounds } from '../hooks/useTeamRounds';
import { useTeamStandings } from '../hooks/useTeamStandings';

/** 団体戦の戦績一覧(チーム×ラウンドの対戦相手・結果)。順位表とは別メニュー。個人名は含めない */
export function TeamCrossTablePage() {
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
  const isEmpty = groupStandings?.every((g) => g.standings.length === 0) ?? false;

  return (
    <Box>
      <Typography variant="h3" component="h2" sx={{ mb: 2 }}>
        戦績一覧
      </Typography>
      {isPending && <LoadingState />}
      {isError && (
        <ErrorState
          message="戦績一覧の取得に失敗しました"
          onRetry={() => {
            void refetchStandings();
            void refetchRounds();
          }}
        />
      )}
      {groupStandings && rounds && isEmpty && (
        <EmptyState
          icon={<TableChartIcon fontSize="inherit" />}
          message="戦績はまだありません。ラウンドを確定すると表示されます"
        />
      )}
      {groupStandings &&
        rounds &&
        !isEmpty &&
        groupStandings.map(({ group, standings }) => (
          <Box key={group.id} sx={{ mb: 4 }}>
            {groupStandings.length > 1 && (
              <Typography variant="h4" component="h3" sx={{ mb: 1 }}>
                {group.name}
              </Typography>
            )}
            <TeamCrossTable
              rounds={rounds.map((round) => ({
                ...round,
                matches: round.matches.filter((m) => m.group.id === group.id),
              }))}
              standings={standings}
            />
          </Box>
        ))}
    </Box>
  );
}
