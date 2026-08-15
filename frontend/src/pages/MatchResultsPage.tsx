import PrintIcon from '@mui/icons-material/Print';
import TableChartIcon from '@mui/icons-material/TableChart';
import { Box, Button, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

import { MatchResultsTable } from '../components/features/standing/MatchResultsTable';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useRounds } from '../hooks/useRounds';
import { useStandings } from '../hooks/useStandings';
import { paths } from '../routes';
import { TeamMatchResultsPage } from './TeamMatchResultsPage';

/**
 * 対戦結果(参加者×ラウンドの対戦相手・結果)。順位表とは別メニュー。グループ大会はグループごとに表示。
 * 団体戦(competitionType=TEAM)はTeamMatchResultsPageに切り替わる
 */
export function MatchResultsPage() {
  const tournament = useTournamentContext();
  if (tournament.competitionType === 'TEAM') {
    return <TeamMatchResultsPage />;
  }
  return <IndividualMatchResultsPage />;
}

function IndividualMatchResultsPage() {
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
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          mb: 2,
          gap: 1,
          flexWrap: 'wrap',
        }}
      >
        <Typography
          variant="h3"
          component="h2"
          sx={{ borderLeft: 4, borderColor: 'primary.main', pl: 1.5 }}
        >
          対戦結果
        </Typography>
        <Button
          variant="outlined"
          startIcon={<PrintIcon />}
          component={Link}
          to={paths.printMatchResults(tournament.id)}
        >
          印刷
        </Button>
      </Box>
      {isPending && <LoadingState />}
      {isError && (
        <ErrorState
          message="対戦結果の取得に失敗しました"
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
            <MatchResultsTable
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
