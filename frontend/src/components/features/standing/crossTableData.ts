import { resultMark } from '../round/matchDisplay';

import type { Match, Round } from '../../../types/round';
import type { ParticipantSummary } from '../../../types/participant';
import type { Standing } from '../../../types/standing';

export interface CrossTableCell {
  /** 対戦相手。null かつ isBye=false は対局なし(離脱・後入り等)、isBye=true は不戦勝 */
  opponent: ParticipantSummary | null;
  isBye: boolean;
  /** resultMark() 互換('○'/'●'/'△')。未対局・未入力・不戦勝は null */
  mark: string | null;
}

export interface CrossTableRow {
  standing: Standing;
  cells: CrossTableCell[];
}

function findMatch(matches: Match[], participantId: string): Match | undefined {
  return matches.find((m) => m.player1.id === participantId || m.player2?.id === participantId);
}

function cellFor(match: Match | undefined, participantId: string): CrossTableCell {
  if (!match) {
    return { opponent: null, isBye: false, mark: null };
  }
  if (match.player2 === null) {
    return { opponent: null, isBye: true, mark: null };
  }
  const side = match.player1.id === participantId ? 'player1' : 'player2';
  const opponent = side === 'player1' ? match.player2 : match.player1;
  return { opponent, isBye: false, mark: resultMark(match, side) };
}

/** 参加者×ラウンドの対戦成績一覧(戦績一覧表)を組み立てる。1グループ分の rounds/standings を渡す */
export function buildCrossTableRows(rounds: Round[], groupStandings: Standing[]): CrossTableRow[] {
  const sorted = [...groupStandings].sort(
    (a, b) => a.participant.entryOrder - b.participant.entryOrder,
  );
  return sorted.map((standing) => ({
    standing,
    cells: rounds.map((round) =>
      cellFor(findMatch(round.matches, standing.participant.id), standing.participant.id),
    ),
  }));
}
