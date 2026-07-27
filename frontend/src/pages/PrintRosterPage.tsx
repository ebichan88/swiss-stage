import { Box } from '@mui/material';

import { PrintGlobalStyles } from '../components/features/print/PrintGlobalStyles';
import { PrintReportHeader } from '../components/features/print/PrintReportHeader';
import { PrintRoster } from '../components/features/print/PrintRoster';
import { PrintTeamRoster } from '../components/features/print/PrintTeamRoster';
import { useTournamentContext } from '../components/layouts/tournamentContext';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useGroups } from '../hooks/useGroups';
import { useParticipants } from '../hooks/useParticipants';
import { useTeams } from '../hooks/useTeams';

/**
 * 参加者名簿(印刷)。団体戦(competitionType=TEAM)は運営専用のチーム名簿に切り替わる。
 * hooksを条件分岐なしで呼ぶため、本体は競技形式ごとに別コンポーネントに分ける
 */
export function PrintRosterPage() {
  const tournament = useTournamentContext();
  if (tournament.competitionType === 'TEAM') {
    return <PrintTeamRosterPage />;
  }
  return <PrintIndividualRosterPage />;
}

function PrintIndividualRosterPage() {
  const tournament = useTournamentContext();
  const {
    data: participants,
    isPending: participantsPending,
    isError: participantsError,
    refetch: refetchParticipants,
  } = useParticipants(tournament.id);
  const {
    data: groups,
    isPending: groupsPending,
    isError: groupsError,
    refetch: refetchGroups,
  } = useGroups(tournament.id);

  if (participantsPending || groupsPending) {
    return <LoadingState />;
  }
  if (participantsError || groupsError || !participants || !groups) {
    return (
      <ErrorState
        message="参加者名簿の取得に失敗しました"
        onRetry={() => {
          void refetchParticipants();
          void refetchGroups();
        }}
      />
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <PrintGlobalStyles orientation="portrait" />
      <PrintReportHeader
        tournamentName={tournament.name}
        eventDate={tournament.eventDate}
        reportTitle="参加者名簿"
      />
      <PrintRoster participants={participants} groups={groups} />
    </Box>
  );
}

function PrintTeamRosterPage() {
  const tournament = useTournamentContext();
  const {
    data: teams,
    isPending: teamsPending,
    isError: teamsError,
    refetch: refetchTeams,
  } = useTeams(tournament.id);
  const {
    data: groups,
    isPending: groupsPending,
    isError: groupsError,
    refetch: refetchGroups,
  } = useGroups(tournament.id);

  if (teamsPending || groupsPending) {
    return <LoadingState />;
  }
  if (teamsError || groupsError || !teams || !groups) {
    return (
      <ErrorState
        message="参加者名簿の取得に失敗しました"
        onRetry={() => {
          void refetchTeams();
          void refetchGroups();
        }}
      />
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <PrintGlobalStyles orientation="portrait" />
      <PrintReportHeader
        tournamentName={tournament.name}
        eventDate={tournament.eventDate}
        reportTitle="参加者名簿"
      />
      <PrintTeamRoster teams={teams} groups={groups} />
    </Box>
  );
}
