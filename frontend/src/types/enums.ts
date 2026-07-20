/**
 * 共通enum(文字列リテラル + as const)。
 * 定義: .claude/01_development_docs/07_type_definitions.md §2(バックエンドと同期)
 */

export const GameType = {
  GO: 'GO',
  SHOGI: 'SHOGI',
} as const;
export type GameType = (typeof GameType)[keyof typeof GameType];

export const TournamentStatus = {
  PREPARING: 'PREPARING',
  IN_PROGRESS: 'IN_PROGRESS',
  FINISHED: 'FINISHED',
} as const;
export type TournamentStatus = (typeof TournamentStatus)[keyof typeof TournamentStatus];

export const Visibility = {
  PRIVATE: 'PRIVATE',
  TOKEN: 'TOKEN',
  PUBLIC: 'PUBLIC',
} as const;
export type Visibility = (typeof Visibility)[keyof typeof Visibility];

export const ParticipantStatus = {
  ACTIVE: 'ACTIVE',
  WITHDRAWN: 'WITHDRAWN',
} as const;
export type ParticipantStatus = (typeof ParticipantStatus)[keyof typeof ParticipantStatus];

export const RoundStatus = {
  PAIRING: 'PAIRING',
  PLAYING: 'PLAYING',
  CONFIRMED: 'CONFIRMED',
} as const;
export type RoundStatus = (typeof RoundStatus)[keyof typeof RoundStatus];

export const MatchResult = {
  NONE: 'NONE',
  PLAYER1_WIN: 'PLAYER1_WIN',
  PLAYER2_WIN: 'PLAYER2_WIN',
  DRAW: 'DRAW',
  BOTH_LOSE: 'BOTH_LOSE',
  BYE: 'BYE',
} as const;
export type MatchResult = (typeof MatchResult)[keyof typeof MatchResult];

/**
 * 棋力(段級位)。29段階、強い順(9段 → 20級)。
 * 順序はordinalではなく明示的な並び(backend Rank.sortOrder と同期)で管理する。
 */
export const RANKS_STRONGEST_FIRST = [
  'DAN_9',
  'DAN_8',
  'DAN_7',
  'DAN_6',
  'DAN_5',
  'DAN_4',
  'DAN_3',
  'DAN_2',
  'DAN_1',
  'KYU_1',
  'KYU_2',
  'KYU_3',
  'KYU_4',
  'KYU_5',
  'KYU_6',
  'KYU_7',
  'KYU_8',
  'KYU_9',
  'KYU_10',
  'KYU_11',
  'KYU_12',
  'KYU_13',
  'KYU_14',
  'KYU_15',
  'KYU_16',
  'KYU_17',
  'KYU_18',
  'KYU_19',
  'KYU_20',
] as const;
export type Rank = (typeof RANKS_STRONGEST_FIRST)[number];
