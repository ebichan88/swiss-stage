import type { components } from './generated/api';

/**
 * 対局。player2 が null なら不戦勝(BYE)。
 * tableNumber はグループ内で1始まり(複数グループ大会の表示は「A-1」形式)
 */
export type Match = components['schemas']['Match'];

export type Round = components['schemas']['Round'];

/**
 * POST /rounds(組み合わせ生成)のレスポンス。
 * relaxations が空でなければUIに警告を表示する(REMATCH / BYE_REPEAT / SAME_ORGANIZATION)
 */
export type GeneratedRound = components['schemas']['GeneratedRound'];

/** PUT /matches/{mid}/result(運営者による直接確定)。versionは楽観ロック用 */
export type InputResultInput = components['schemas']['InputResultRequest'];

/**
 * PUT /shared/{token}/matches/{mid}/result(トークン経由の自己申告)。
 * reportedBy側の申告として送信し、両者の申告が一致すると自動確定する
 */
export type ReportMatchResultInput = components['schemas']['ReportMatchResultRequest'];
