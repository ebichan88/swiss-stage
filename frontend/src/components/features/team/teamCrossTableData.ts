import { teamAggregatePoints, teamResultMark } from './teamMatchDisplay';
import type { TeamMatch, TeamRound, TeamStanding, TeamSummary } from '../../../types/team';

export interface TeamCrossTableCell {
  /** 対戦相手。null かつ isBye=false は対局なし(離脱・後入り等)、isBye=true は不戦勝 */
  opponent: TeamSummary | null;
  isBye: boolean;
  /** teamResultMark() 互換('○'/'●'/'△')。未対局・未入力・不戦勝は null */
  mark: string | null;
  /** ボード内訳(例: 「2-1」)。全ボード決着前・不戦勝時は null */
  breakdown: string | null;
}

export interface TeamCrossTableRow {
  standing: TeamStanding;
  cells: TeamCrossTableCell[];
}

function findMatch(matches: TeamMatch[], teamId: string): TeamMatch | undefined {
  return matches.find((m) => m.team1.id === teamId || m.team2?.id === teamId);
}

function cellFor(match: TeamMatch | undefined, teamId: string): TeamCrossTableCell {
  if (!match) {
    return { opponent: null, isBye: false, mark: null, breakdown: null };
  }
  if (match.team2 === null) {
    return { opponent: null, isBye: true, mark: null, breakdown: null };
  }
  const side = match.team1.id === teamId ? 'team1' : 'team2';
  const opponent = side === 'team1' ? match.team2 : match.team1;
  const mark = teamResultMark(match, side);
  if (mark === null) {
    return { opponent, isBye: false, mark: null, breakdown: null };
  }
  const { team1, team2 } = teamAggregatePoints(match);
  const own = side === 'team1' ? team1 : team2;
  const other = side === 'team1' ? team2 : team1;
  const breakdown = `${own / 2}-${other / 2}`;
  return { opponent, isBye: false, mark, breakdown };
}

/** チーム×ラウンドの対戦成績一覧(戦績一覧表)を組み立てる。1グループ分の rounds/standings を渡す */
export function buildTeamCrossTableRows(
  rounds: TeamRound[],
  groupStandings: TeamStanding[],
): TeamCrossTableRow[] {
  const sorted = [...groupStandings].sort((a, b) => a.team.entryOrder - b.team.entryOrder);
  return sorted.map((standing) => ({
    standing,
    cells: rounds.map((round) =>
      cellFor(findMatch(round.matches, standing.team.id), standing.team.id),
    ),
  }));
}
