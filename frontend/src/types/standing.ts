import type { Group } from './group';
import type { ParticipantSummary } from './participant';

/** 順位表の1行。rank は同順位あり(1,2,2,4 形式)。グループ大会ではグループ内順位。勝点は0.5刻みあり得るため number */
export interface Standing {
  rank: number;
  participant: ParticipantSummary;
  wins: number;
  losses: number;
  sos: number;
  sosos: number;
  hadBye: boolean;
}

/**
 * グループ別順位表(backend: GroupStandingsDto)。
 * グループごとに1要素(group は常に非null。グループが1つだけの大会は単一要素)
 */
export interface GroupStandings {
  group: Group;
  standings: Standing[];
}
