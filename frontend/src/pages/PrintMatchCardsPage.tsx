import { Box } from '@mui/material';

import {
  buildMatchCards,
  buildTeamMatchCards,
  chunkIntoPages,
  decideLayout,
  TEAM_CARD_LAYOUT,
} from '../components/features/print/matchCardData';
import { MatchCardSheet } from '../components/features/print/MatchCardSheet';
import { PrintGlobalStyles } from '../components/features/print/PrintGlobalStyles';
import { PrintReportHeader } from '../components/features/print/PrintReportHeader';
import { TeamMatchCardSheet } from '../components/features/print/TeamMatchCardSheet';
import { useTournamentContext } from '../components/layouts/tournamentContext';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useGroups } from '../hooks/useGroups';
import { useParticipants } from '../hooks/useParticipants';
import { useTeams } from '../hooks/useTeams';

/**
 * 対局カード(印刷)。大会前に全員分を一括印刷し、卓番号・対戦相手・結果は参加者が手書きする(rounds は不要)。
 * 団体戦(competitionType=TEAM)はチーム版(1チーム1枚・個人名なし)に切り替わる。
 * hooksを条件分岐なしで呼ぶため、本体は競技形式ごとに別コンポーネントに分ける
 */
export function PrintMatchCardsPage() {
  const tournament = useTournamentContext();
  if (tournament.competitionType === 'TEAM') {
    return <PrintTeamMatchCardsPage />;
  }
  return <PrintIndividualMatchCardsPage />;
}

function PrintIndividualMatchCardsPage() {
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
        message="対局カードの取得に失敗しました"
        onRetry={() => {
          void refetchParticipants();
          void refetchGroups();
        }}
      />
    );
  }

  const layout = decideLayout(tournament.totalRounds);
  const cards = buildMatchCards(participants, groups, tournament.totalRounds);
  const pages = chunkIntoPages(cards, layout.columns * layout.rows);

  return (
    <Box sx={{ p: 3 }}>
      <PrintGlobalStyles orientation="portrait" />
      <PrintReportHeader
        tournamentName={tournament.name}
        eventDate={tournament.eventDate}
        reportTitle="対局カード"
      />
      <MatchCardSheet pages={pages} layout={layout} />
    </Box>
  );
}

function PrintTeamMatchCardsPage() {
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
        message="対局カードの取得に失敗しました"
        onRetry={() => {
          void refetchTeams();
          void refetchGroups();
        }}
      />
    );
  }

  const layout = TEAM_CARD_LAYOUT;
  const cards = buildTeamMatchCards(
    teams,
    groups,
    tournament.totalRounds,
    tournament.teamSize ?? 3,
  );
  const pages = chunkIntoPages(cards, layout.columns * layout.rows);

  return (
    <Box sx={{ p: 3 }}>
      <PrintGlobalStyles orientation="portrait" />
      <PrintReportHeader
        tournamentName={tournament.name}
        eventDate={tournament.eventDate}
        reportTitle="対局カード"
      />
      <TeamMatchCardSheet pages={pages} layout={layout} />
    </Box>
  );
}
