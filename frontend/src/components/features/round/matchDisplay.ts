import type { Group } from '../../../types/group';
import type { MatchResult } from '../../../types/enums';
import type { Match } from '../../../types/round';

/**
 * 卓番号の表示。複数グループ大会は「A-1」形式、
 * グループが1つだけの大会は表示上グループを見せないため数字のみ
 */
export function tableLabel(match: Match, multiGroup: boolean): string {
  return multiGroup ? `${match.group.name}-${match.tableNumber}` : String(match.tableNumber);
}

export interface MatchSection {
  group: Group;
  matches: Match[];
}

/**
 * 対局をグループごとのセクションに分ける(入力はグループ→卓番号順で並んでいる前提)。
 * グループ見出しはセクションが2つ以上のときのみ表示する
 */
export function matchSections(matches: Match[]): MatchSection[] {
  const sections: MatchSection[] = [];
  for (const match of matches) {
    const last = sections.at(-1);
    if (last && last.group.id === match.group.id) {
      last.matches.push(match);
    } else {
      sections.push({ group: match.group, matches: [match] });
    }
  }
  return sections;
}

/** 対局者の勝敗マーク(○=勝ち / ●=負け / △=引き分け)。未入力・BYEは null */
export function resultMark(match: Match, side: 'player1' | 'player2'): string | null {
  switch (match.result) {
    case 'PLAYER1_WIN':
      return side === 'player1' ? '○' : '●';
    case 'PLAYER2_WIN':
      return side === 'player2' ? '○' : '●';
    case 'DRAW':
      return '△';
    case 'BOTH_LOSE':
      return '●';
    default:
      return null;
  }
}

/**
 * トークン経由の自己申告の状態(05_swiss_pairing_algorithm.mdの対象外・結果確定の運用ルール)。
 * NOT_REPORTED = 未申告 / WAITING = 片方のみ申告 / CONFLICTING = 申告不一致 / DECIDED = 確定済み
 */
export type MatchReportStatus = 'NOT_REPORTED' | 'WAITING' | 'CONFLICTING' | 'DECIDED';

export function matchReportStatus(match: Match): MatchReportStatus {
  if (match.result !== 'NONE') {
    return 'DECIDED';
  }
  const { player1ReportedResult: p1, player2ReportedResult: p2 } = match;
  if (p1 === 'NONE' && p2 === 'NONE') {
    return 'NOT_REPORTED';
  }
  if (p1 === 'NONE' || p2 === 'NONE') {
    return 'WAITING';
  }
  return 'CONFLICTING';
}

/** 任意のMatchResult値のラベル化(確定結果・自己申告いずれの表示にも使う) */
export function outcomeLabel(match: Match, value: MatchResult): string {
  switch (value) {
    case 'NONE':
      return '未申告';
    case 'PLAYER1_WIN':
      return `${match.player1.name} の勝ち`;
    case 'PLAYER2_WIN':
      return `${match.player2?.name ?? ''} の勝ち`;
    case 'DRAW':
      return '引き分け';
    case 'BOTH_LOSE':
      return '両者負け';
    case 'BYE':
      return '不戦勝';
  }
}

/** 結果の表示テキスト(確定済みラウンド等、入力コントロールを出さない場面用) */
export function matchResultText(match: Match): string {
  return match.result === 'NONE' ? '未入力' : outcomeLabel(match, match.result);
}

/** side側が申告した内容のラベル(未申告は「未申告」) */
export function reportedResultLabel(match: Match, side: 'player1' | 'player2'): string {
  const value = side === 'player1' ? match.player1ReportedResult : match.player2ReportedResult;
  return outcomeLabel(match, value);
}

/**
 * 確定済みの結果と、確定後に変化した自己申告が食い違うか(13_security_design.mdの結果確定の運用ルール§5)。
 * バックエンドの追加フィールドなしで既存の result/player1ReportedResult/player2ReportedResult から算出する
 */
export function hasReportMismatch(match: Match): boolean {
  if (match.result === 'NONE' || match.result === 'BYE') {
    return false;
  }
  const { player1ReportedResult: p1, player2ReportedResult: p2 } = match;
  return (p1 !== 'NONE' && p1 !== match.result) || (p2 !== 'NONE' && p2 !== match.result);
}
