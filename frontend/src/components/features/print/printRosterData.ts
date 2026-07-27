import { rankLabel } from '../../../utils/labels';
import type { Group } from '../../../types/group';
import type { Participant } from '../../../types/participant';

export interface PrintRosterRow {
  entryOrder: number;
  name: string;
  organization: string | null;
  rankText: string;
  /** 単一グループ大会は null(見出し・列を出さない) */
  groupName: string | null;
  withdrawn: boolean;
}

/** グループ順(APIが返す順=作成順)→entryOrder順に並べる */
export function buildRosterRows(participants: Participant[], groups: Group[]): PrintRosterRow[] {
  const groupOrder = new Map(groups.map((g, index) => [g.id, index]));
  const groupNameOf = new Map(groups.map((g) => [g.id, g.name]));
  const singleGroup = groups.length <= 1;

  return [...participants]
    .sort((a, b) => {
      const orderA = groupOrder.get(a.groupId) ?? 0;
      const orderB = groupOrder.get(b.groupId) ?? 0;
      return orderA !== orderB ? orderA - orderB : a.entryOrder - b.entryOrder;
    })
    .map((p) => ({
      entryOrder: p.entryOrder,
      name: p.name,
      organization: p.organization,
      rankText: rankLabel(p.rank),
      groupName: singleGroup ? null : (groupNameOf.get(p.groupId) ?? ''),
      withdrawn: p.status === 'WITHDRAWN',
    }));
}
