import { Box } from '@mui/material';

import { PrintCrossTable } from '../components/features/print/PrintCrossTable';
import { PrintGlobalStyles } from '../components/features/print/PrintGlobalStyles';
import { PrintReportHeader } from '../components/features/print/PrintReportHeader';
import { PrintTeamCrossTable } from '../components/features/print/PrintTeamCrossTable';
import { breakAfterPageSx } from '../components/features/print/printSx';
import { useTournamentContext } from '../components/layouts/tournamentContext';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useRounds } from '../hooks/useRounds';
import { useStandings } from '../hooks/useStandings';
import { useTeamRounds } from '../hooks/useTeamRounds';
import { useTeamStandings } from '../hooks/useTeamStandings';

/**
 * 戦績一覧表(印刷)。団体戦(competitionType=TEAM)はチーム版に切り替わる。
 * hooksを条件分岐なしで呼ぶため、本体は競技形式ごとに別コンポーネントに分ける
 */
export function PrintCrossTablePage() {
  const tournament = useTournamentContext();
  if (tournament.competitionType === 'TEAM') {
    return <PrintTeamCrossTablePage />;
  }
  return <PrintIndividualCrossTablePage />;
}

function PrintIndividualCrossTablePage() {
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

  if (standingsPending || roundsPending) {
    return <LoadingState />;
  }
  if (standingsError || roundsError || !groupStandings || !rounds) {
    return (
      <ErrorState
        message="戦績一覧の取得に失敗しました"
        onRetry={() => {
          void refetchStandings();
          void refetchRounds();
        }}
      />
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <PrintGlobalStyles orientation="landscape" />
      {groupStandings.map(({ group, standings }, index) => (
        <Box key={group.id} sx={index < groupStandings.length - 1 ? breakAfterPageSx : undefined}>
          <PrintReportHeader
            tournamentName={tournament.name}
            eventDate={tournament.eventDate}
            reportTitle="戦績一覧表"
            groupName={groupStandings.length > 1 ? group.name : null}
          />
          <PrintCrossTable
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

function PrintTeamCrossTablePage() {
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

  if (standingsPending || roundsPending) {
    return <LoadingState />;
  }
  if (standingsError || roundsError || !groupStandings || !rounds) {
    return (
      <ErrorState
        message="戦績一覧の取得に失敗しました"
        onRetry={() => {
          void refetchStandings();
          void refetchRounds();
        }}
      />
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <PrintGlobalStyles orientation="landscape" />
      {groupStandings.map(({ group, standings }, index) => (
        <Box key={group.id} sx={index < groupStandings.length - 1 ? breakAfterPageSx : undefined}>
          <PrintReportHeader
            tournamentName={tournament.name}
            eventDate={tournament.eventDate}
            reportTitle="戦績一覧表"
            groupName={groupStandings.length > 1 ? group.name : null}
          />
          <PrintTeamCrossTable
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
