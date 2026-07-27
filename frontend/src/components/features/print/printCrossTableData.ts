import { rankLabel } from '../../../utils/labels';
import type { Participant } from '../../../types/participant';
import type { Team } from '../../../types/team';

export interface PrintCrossTableRow {
  entryOrder: number;
  name: string;
  rankText: string;
}

export interface PrintTeamCrossTableRow {
  entryOrder: number;
  name: string;
}

/** ACTIVEのみ・entryOrder順に並べる(グループ分割はページ側が担う。02_component_design.md §3) */
export function buildPrintCrossTableRows(participants: Participant[]): PrintCrossTableRow[] {
  return participants
    .filter((p) => p.status === 'ACTIVE')
    .sort((a, b) => a.entryOrder - b.entryOrder)
    .map((p) => ({
      entryOrder: p.entryOrder,
      name: p.name,
      rankText: rankLabel(p.rank),
    }));
}

/** ACTIVEのみ・entryOrder順に並べる(グループ分割はページ側が担う) */
export function buildPrintTeamCrossTableRows(teams: Team[]): PrintTeamCrossTableRow[] {
  return teams
    .filter((t) => t.status === 'ACTIVE')
    .sort((a, b) => a.entryOrder - b.entryOrder)
    .map((t) => ({
      entryOrder: t.entryOrder,
      name: t.name,
    }));
}
