import type {
  GameType,
  ParticipantStatus,
  Rank,
  RoundStatus,
  TournamentStatus,
  Visibility,
} from '../types/enums';

/** enum値の日本語表示ラベル。値の定義は types/enums.ts(バックエンドと同期) */

export const gameTypeLabels: Record<GameType, string> = {
  GO: '囲碁',
  SHOGI: '将棋',
};

export const tournamentStatusLabels: Record<TournamentStatus, string> = {
  PREPARING: '準備中',
  IN_PROGRESS: '開催中',
  FINISHED: '終了',
};

export const visibilityLabels: Record<Visibility, string> = {
  PRIVATE: '非公開(運営者のみ)',
  TOKEN: '共有URLを知っている人のみ',
  PUBLIC: '公開',
};

export const participantStatusLabels: Record<ParticipantStatus, string> = {
  ACTIVE: '参加中',
  WITHDRAWN: '棄権',
};

export const roundStatusLabels: Record<RoundStatus, string> = {
  PAIRING: '組み合わせ中',
  PLAYING: '対局中',
  CONFIRMED: '確定',
};

/** 組み合わせ生成時の制約緩和コード(backend: PairingRelaxation)の警告文 */
export const relaxationLabels: Record<string, string> = {
  REMATCH: '再戦が発生しています',
  BYE_REPEAT: '同じ参加者に2回目の不戦勝が発生しています',
  SAME_ORGANIZATION: '同一所属同士の対局が発生しています',
};

export function relaxationLabel(code: string): string {
  return relaxationLabels[code] ?? code;
}

/**
 * 棋力の表示名(backend Rank.displayName と同期)。
 * KYU_n = 「n級」、DAN_1 = 「初段」、DAN_2〜DAN_9 = 「2段」〜「9段」
 */
export function rankLabel(rank: Rank | null): string {
  if (rank === null) {
    return '未入力';
  }
  if (rank === 'DAN_1') {
    return '初段';
  }
  const [kind, num] = rank.split('_');
  return kind === 'DAN' ? `${num}段` : `${num}級`;
}
