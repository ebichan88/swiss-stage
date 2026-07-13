import type { ParticipantSummary } from './participant';

/** 順位表の1行。rank は同順位あり(1,2,2,4 形式)。勝点は0.5刻みあり得るため number */
export interface Standing {
  rank: number;
  participant: ParticipantSummary;
  wins: number;
  losses: number;
  sos: number;
  sosos: number;
  hadBye: boolean;
}
