import type { Group } from '../../../types/group';
import type { MatchResult } from '../../../types/enums';
import type { BoardResult, TeamMatch } from '../../../types/team';

/**
 * 卓番号の表示。複数グループ大会は「A-1」形式、
 * グループが1つだけの大会は表示上グループを見せないため数字のみ
 */
export function teamTableLabel(match: TeamMatch, multiGroup: boolean): string {
  return multiGroup ? `${match.group.name}-${match.tableNumber}` : String(match.tableNumber);
}

export interface TeamMatchSection {
  group: Group;
  matches: TeamMatch[];
}

/** 対局をグループごとのセクションに分ける(入力はグループ→卓番号順で並んでいる前提) */
export function teamMatchSections(matches: TeamMatch[]): TeamMatchSection[] {
  const sections: TeamMatchSection[] = [];
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

/** ボード結果1件分の点数(2倍値。team1視点)。backendのMatchResult点数変換表と同じ */
function boardPointsForTeam1(result: MatchResult): number {
  switch (result) {
    case 'PLAYER1_WIN':
      return 2;
    case 'DRAW':
      return 1;
    default:
      return 0;
  }
}

function boardPointsForTeam2(result: MatchResult): number {
  switch (result) {
    case 'PLAYER2_WIN':
      return 2;
    case 'DRAW':
      return 1;
    default:
      return 0;
  }
}

/** 全ボードが決着済み(BYEも含む)か。チーム全体の勝敗を導出できるかの判定に使う */
export function isTeamMatchFullyDecided(match: TeamMatch): boolean {
  return match.team2 === null || match.boardResults.every((b) => b.result !== 'NONE');
}

/** チームのボード点数の合計(2倍値)。全ボード決着前でも表示用の途中経過として計算できる */
export function teamAggregatePoints(match: TeamMatch): { team1: number; team2: number } {
  let team1 = 0;
  let team2 = 0;
  for (const board of match.boardResults) {
    team1 += boardPointsForTeam1(board.result);
    team2 += boardPointsForTeam2(board.result);
  }
  return { team1, team2 };
}

/** チームの勝敗マーク(○=勝ち / ●=負け / △=引き分け)。全ボード決着前は null */
export function teamResultMark(match: TeamMatch, side: 'team1' | 'team2'): string | null {
  if (match.team2 === null) {
    return side === 'team1' ? '○' : null;
  }
  if (!isTeamMatchFullyDecided(match)) {
    return null;
  }
  const { team1, team2 } = teamAggregatePoints(match);
  if (team1 === team2) {
    return '△';
  }
  const winner = team1 > team2 ? 'team1' : 'team2';
  return side === winner ? '○' : '●';
}

/**
 * トークン経由の自己申告の状態(ボード単位。13_security_design.mdの結果確定の運用ルール)。
 * NOT_REPORTED = 未申告 / WAITING = 片方のみ申告 / CONFLICTING = 申告不一致 / DECIDED = 確定済み
 */
export type BoardReportStatus = 'NOT_REPORTED' | 'WAITING' | 'CONFLICTING' | 'DECIDED';

export function boardReportStatus(board: BoardResult): BoardReportStatus {
  if (board.result !== 'NONE') {
    return 'DECIDED';
  }
  const { team1ReportedResult: t1, team2ReportedResult: t2 } = board;
  if (t1 === 'NONE' && t2 === 'NONE') {
    return 'NOT_REPORTED';
  }
  if (t1 === 'NONE' || t2 === 'NONE') {
    return 'WAITING';
  }
  return 'CONFLICTING';
}

/** ボードが1件でも申告待ち・申告不一致なら true(組み合わせ表・ラウンド管理の警告表示に使う) */
export function teamMatchNeedsAttention(match: TeamMatch): boolean {
  return match.boardResults.some((b) => {
    const status = boardReportStatus(b);
    return status === 'WAITING' || status === 'CONFLICTING';
  });
}

/**
 * 確定済みのボード結果と、確定後に変化した自己申告が食い違うか
 * (13_security_design.mdの結果確定の運用ルール§5をボード単位に適用)
 */
export function boardHasReportMismatch(board: BoardResult): boolean {
  if (board.result === 'NONE') {
    return false;
  }
  const { team1ReportedResult: t1, team2ReportedResult: t2 } = board;
  return (t1 !== 'NONE' && t1 !== board.result) || (t2 !== 'NONE' && t2 !== board.result);
}

export function teamMatchHasReportMismatch(match: TeamMatch): boolean {
  return match.boardResults.some(boardHasReportMismatch);
}

/** 任意のMatchResult値のラベル化(team1/team2はチーム名で表示する) */
export function boardOutcomeLabel(match: TeamMatch, value: MatchResult): string {
  switch (value) {
    case 'NONE':
      return '未申告';
    case 'PLAYER1_WIN':
      return `${match.team1.name}の勝ち`;
    case 'PLAYER2_WIN':
      return `${match.team2?.name ?? ''}の勝ち`;
    case 'DRAW':
      return '引き分け';
    case 'BOTH_LOSE':
      return '両者負け';
    case 'BYE':
      return '不戦勝';
  }
}

/** ボード結果の表示テキスト(確定済みラウンド等、入力コントロールを出さない場面用) */
export function boardResultText(match: TeamMatch, board: BoardResult): string {
  return board.result === 'NONE' ? '未入力' : boardOutcomeLabel(match, board.result);
}

/** side側が申告した内容のラベル(未申告は「未申告」) */
export function boardReportedResultLabel(
  match: TeamMatch,
  board: BoardResult,
  side: 'team1' | 'team2',
): string {
  const value = side === 'team1' ? board.team1ReportedResult : board.team2ReportedResult;
  return boardOutcomeLabel(match, value);
}
