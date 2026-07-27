import { Box } from '@mui/material';

import { PrintCrossTable } from '../components/features/print/PrintCrossTable';
import { PrintGlobalStyles } from '../components/features/print/PrintGlobalStyles';
import { PrintReportHeader } from '../components/features/print/PrintReportHeader';
import { PrintTeamCrossTable } from '../components/features/print/PrintTeamCrossTable';
import { breakAfterPageSx } from '../components/features/print/printSx';
import { useTournamentContext } from '../components/layouts/tournamentContext';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useGroups } from '../hooks/useGroups';
import { useParticipants } from '../hooks/useParticipants';
import { useTeams } from '../hooks/useTeams';

/**
 * 戦績一覧表(印刷)。大会開始前に印刷する記入用シートのため、参加者/チームとグループのみを取得する
 * (rounds/standingsは使わない。対戦相手・結果・勝点等は手書き)。団体戦(competitionType=TEAM)は
 * チーム版に切り替わる。hooksを条件分岐なしで呼ぶため、本体は競技形式ごとに別コンポーネントに分ける
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
        message="戦績一覧表の取得に失敗しました"
        onRetry={() => {
          void refetchParticipants();
          void refetchGroups();
        }}
      />
    );
  }

  const singleGroup = groups.length <= 1;

  return (
    <Box sx={{ p: 3, '@media print': { p: 0 } }}>
      <PrintGlobalStyles orientation="landscape" />
      {groups.map((group, index) => (
        <Box key={group.id} sx={index < groups.length - 1 ? breakAfterPageSx : undefined}>
          <PrintReportHeader
            tournamentName={tournament.name}
            eventDate={tournament.eventDate}
            reportTitle="戦績一覧表"
            groupName={singleGroup ? null : group.name}
          />
          <PrintCrossTable
            participants={participants.filter((p) => p.groupId === group.id)}
            totalRounds={tournament.totalRounds}
          />
        </Box>
      ))}
    </Box>
  );
}

function PrintTeamCrossTablePage() {
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
        message="戦績一覧表の取得に失敗しました"
        onRetry={() => {
          void refetchTeams();
          void refetchGroups();
        }}
      />
    );
  }

  const singleGroup = groups.length <= 1;

  return (
    <Box sx={{ p: 3, '@media print': { p: 0 } }}>
      <PrintGlobalStyles orientation="landscape" />
      {groups.map((group, index) => (
        <Box key={group.id} sx={index < groups.length - 1 ? breakAfterPageSx : undefined}>
          <PrintReportHeader
            tournamentName={tournament.name}
            eventDate={tournament.eventDate}
            reportTitle="戦績一覧表"
            groupName={singleGroup ? null : group.name}
          />
          <PrintTeamCrossTable
            teams={teams.filter((t) => t.groupId === group.id)}
            totalRounds={tournament.totalRounds}
          />
        </Box>
      ))}
    </Box>
  );
}
