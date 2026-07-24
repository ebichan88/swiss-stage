/**
 * 共通enum。型は schema/openapi.yaml からの生成型(./generated/api)を唯一の正とし、
 * ここでは実行時に使う定数オブジェクトを定義する(`satisfies` でスキーマとの網羅・整合を検査)。
 */
import type { components } from './generated/api';

type Schemas = components['schemas'];

export type GameType = Schemas['GameType'];
export const GameType = {
  GO: 'GO',
  SHOGI: 'SHOGI',
} as const satisfies { [K in GameType]: K };

/** 個人戦(INDIVIDUAL)か団体戦(TEAM)か。大会作成後は変更不可 */
export type CompetitionType = Schemas['CompetitionType'];
export const CompetitionType = {
  INDIVIDUAL: 'INDIVIDUAL',
  TEAM: 'TEAM',
} as const satisfies { [K in CompetitionType]: K };

export type TournamentStatus = Schemas['TournamentStatus'];
export const TournamentStatus = {
  PREPARING: 'PREPARING',
  IN_PROGRESS: 'IN_PROGRESS',
  FINISHED: 'FINISHED',
} as const satisfies { [K in TournamentStatus]: K };

export type Visibility = Schemas['Visibility'];
export const Visibility = {
  PRIVATE: 'PRIVATE',
  TOKEN: 'TOKEN',
  PUBLIC: 'PUBLIC',
} as const satisfies { [K in Visibility]: K };

export type ParticipantStatus = Schemas['ParticipantStatus'];
export const ParticipantStatus = {
  ACTIVE: 'ACTIVE',
  WITHDRAWN: 'WITHDRAWN',
} as const satisfies { [K in ParticipantStatus]: K };

export type RoundStatus = Schemas['RoundStatus'];
export const RoundStatus = {
  PAIRING: 'PAIRING',
  PLAYING: 'PLAYING',
  CONFIRMED: 'CONFIRMED',
} as const satisfies { [K in RoundStatus]: K };

export type MatchResult = Schemas['MatchResult'];
export const MatchResult = {
  NONE: 'NONE',
  PLAYER1_WIN: 'PLAYER1_WIN',
  PLAYER2_WIN: 'PLAYER2_WIN',
  DRAW: 'DRAW',
  BOTH_LOSE: 'BOTH_LOSE',
  BYE: 'BYE',
} as const satisfies { [K in MatchResult]: K };

export type MatchSide = Schemas['MatchSide'];
export const MatchSide = {
  PLAYER1: 'PLAYER1',
  PLAYER2: 'PLAYER2',
} as const satisfies { [K in MatchSide]: K };

/**
 * 棋力(段級位)。29段階、強い順(9段 → 20級)。
 * スキーマ上はnull許容(null = 未入力)のため、非nullの値集合をRankとする。
 * 順序はordinalではなく明示的な並び(backend Rank.sortOrder と同期)で管理する。
 */
export type Rank = NonNullable<Schemas['Rank']>;
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
] as const satisfies readonly Rank[];
