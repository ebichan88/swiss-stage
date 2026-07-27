import { boardPositionLabel, rankLabel } from '../../../utils/labels';
import type { Group } from '../../../types/group';
import type { Team } from '../../../types/team';

export interface PrintTeamRosterRow {
  teamName: string;
  memberName: string;
  rankText: string;
  positionLabel: string;
  /** 単一グループ大会は null(見出し・列を出さない) */
  groupName: string | null;
  withdrawn: boolean;
}

/**
 * グループ順(APIが返す順=作成順)→チームentryOrder順→ボード順(主将→補欠)に、
 * メンバー1人につき1行で並べる(CSVエクスポートと同じ「メンバー1人1行」構造)
 */
export function buildTeamRosterRows(teams: Team[], groups: Group[]): PrintTeamRosterRow[] {
  const groupOrder = new Map(groups.map((g, index) => [g.id, index]));
  const groupNameOf = new Map(groups.map((g) => [g.id, g.name]));
  const singleGroup = groups.length <= 1;

  const sortedTeams = [...teams].sort((a, b) => {
    const orderA = groupOrder.get(a.groupId) ?? 0;
    const orderB = groupOrder.get(b.groupId) ?? 0;
    return orderA !== orderB ? orderA - orderB : a.entryOrder - b.entryOrder;
  });

  return sortedTeams.flatMap((team) =>
    [...team.members]
      .sort((a, b) => (a.boardPosition ?? Infinity) - (b.boardPosition ?? Infinity))
      .map((member) => ({
        teamName: team.name,
        memberName: member.name,
        rankText: rankLabel(member.rank),
        positionLabel: boardPositionLabel(member.boardPosition),
        groupName: singleGroup ? null : (groupNameOf.get(team.groupId) ?? ''),
        withdrawn: team.status === 'WITHDRAWN',
      })),
  );
}
