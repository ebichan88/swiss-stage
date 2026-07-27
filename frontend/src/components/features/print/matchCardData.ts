import { boardPositionLabel, rankLabel } from '../../../utils/labels';
import type { Group } from '../../../types/group';
import type { Participant } from '../../../types/participant';
import type { Team } from '../../../types/team';

export interface MatchCard {
  entryOrder: number;
  /** 個人戦=氏名 / 団体戦=チーム名 */
  name: string;
  /** 団体戦は常に null(個人名を出さない) */
  organization: string | null;
  /** rankLabel() 済み。団体戦は空文字(段級位はメンバーごとのため対局カードには載せない) */
  rankText: string;
  /** 単一グループ大会は null(見出し・プレフィックスを出さない) */
  groupName: string | null;
  /** 記入行数(= totalRounds) */
  rowCount: number;
  /** 団体戦のみ。ボード役割(主将・副将…)の記入欄見出し */
  boardLabels: string[];
}

export interface CardLayout {
  columns: number;
  rows: number;
}

/** 団体戦は常に2列×4行(8面、A7相当)固定。ボード列(最大5将)が入る幅を確保するため個人戦より広い面が必要 */
export const TEAM_CARD_LAYOUT: CardLayout = { columns: 2, rows: 4 };

/**
 * 個人戦の面付けを決める。4列×4行(16面、A8相当)が既定だが、記入行の高さは行数(縦の分割数)で決まるため、
 * totalRoundsが多い大会では行が潰れる。記入行が6mm以上を確保できるよう、totalRounds≥7は4列×3行(12面)に落とす
 */
export function decideLayout(totalRounds: number): CardLayout {
  return totalRounds >= 7 ? { columns: 4, rows: 3 } : { columns: 4, rows: 4 };
}

/** ACTIVEのみ・グループ順(APIが返す順=作成順)→entryOrder順に並べる */
export function buildMatchCards(
  participants: Participant[],
  groups: Group[],
  totalRounds: number,
): MatchCard[] {
  const groupOrder = new Map(groups.map((g, index) => [g.id, index]));
  const groupNameOf = new Map(groups.map((g) => [g.id, g.name]));
  const singleGroup = groups.length <= 1;

  return participants
    .filter((p) => p.status === 'ACTIVE')
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
      rowCount: totalRounds,
      boardLabels: [],
    }));
}

/** ACTIVEのみ・グループ順→entryOrder順に並べる。個人名は含めない(チーム名+ボード役割のみ) */
export function buildTeamMatchCards(
  teams: Team[],
  groups: Group[],
  totalRounds: number,
  teamSize: number,
): MatchCard[] {
  const groupOrder = new Map(groups.map((g, index) => [g.id, index]));
  const groupNameOf = new Map(groups.map((g) => [g.id, g.name]));
  const singleGroup = groups.length <= 1;
  const boardLabels = Array.from({ length: teamSize }, (_, i) => boardPositionLabel(i + 1));

  return teams
    .filter((t) => t.status === 'ACTIVE')
    .sort((a, b) => {
      const orderA = groupOrder.get(a.groupId) ?? 0;
      const orderB = groupOrder.get(b.groupId) ?? 0;
      return orderA !== orderB ? orderA - orderB : a.entryOrder - b.entryOrder;
    })
    .map((t) => ({
      entryOrder: t.entryOrder,
      name: t.name,
      organization: null,
      rankText: '',
      groupName: singleGroup ? null : (groupNameOf.get(t.groupId) ?? ''),
      rowCount: totalRounds,
      boardLabels,
    }));
}

/**
 * 1ページ perPage 枚に詰めてページ分割する。グループ境界では必ずページを割る
 * (`cards` は呼び出し側で既にグループ順→entryOrder順にソート済みである前提)
 */
export function chunkIntoPages(cards: MatchCard[], perPage: number): MatchCard[][] {
  const pages: MatchCard[][] = [];
  let current: MatchCard[] = [];
  let currentGroup: string | null = null;

  for (const card of cards) {
    const startsNewGroup = current.length > 0 && card.groupName !== currentGroup;
    if (current.length >= perPage || startsNewGroup) {
      pages.push(current);
      current = [];
    }
    if (current.length === 0) {
      currentGroup = card.groupName;
    }
    current.push(card);
  }
  if (current.length > 0) {
    pages.push(current);
  }
  return pages;
}
