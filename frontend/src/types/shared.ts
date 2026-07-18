import type { GameType, TournamentStatus } from './enums';
import type { Round } from './round';
import type { GroupStandings } from './standing';

/**
 * 共有ページ(S10)向けの大会集約(backend: SharedTournamentDto)。
 * shareToken・ownerSub は含まれない(13_security_design.md §6)
 */
export interface SharedTournament {
  tournament: SharedTournamentSummary;
  rounds: Round[];
  /** グループなし大会は group=null の単一要素 */
  standings: GroupStandings[];
}

export interface SharedTournamentSummary {
  name: string;
  gameType: GameType;
  totalRounds: number;
  currentRound: number;
  status: TournamentStatus;
  /** true なら参加者(トークン保持者)が結果入力できる */
  resultInputEnabled: boolean;
}
