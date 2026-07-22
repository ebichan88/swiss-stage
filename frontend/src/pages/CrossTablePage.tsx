import TableChartIcon from '@mui/icons-material/TableChart';
import { Box, Typography } from '@mui/material';

import { CrossTable } from '../components/features/standing/CrossTable';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useRounds } from '../hooks/useRounds';
import { useStandings } from '../hooks/useStandings';

/** 戦績一覧(参加者×ラウンドの対戦相手・結果)。順位表とは別メニュー。グループ大会はグループごとに表示 */
export function CrossTablePage() {
  const tournament = useTournamentContext();
  const {
    data: groupStandings,
    isPending: standingsPending,
    isError: standingsError,
    refetch: refetchStandings,
  } = useStandings(tournament.id);
  const {
    data: rounds,
    isPending: roundsPending,
    isError: roundsError,
    refetch: refetchRounds,
  } = useRounds(tournament.id);

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
            <CrossTable
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
