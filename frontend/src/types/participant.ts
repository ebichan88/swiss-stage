import type { components } from './generated/api';

export type Participant = components['schemas']['Participant'];

/** 対局・順位表で使う参加者の要約 */
export type ParticipantSummary = components['schemas']['ParticipantSummary'];

/** POST /participants。groupId 省略時は先頭グループに割当 */
export type CreateParticipantInput = components['schemas']['CreateParticipantRequest'];

/**
 * PATCH /participants/{pid}。未指定の項目は変更しない。
 * 棋力を未入力に戻すには clearRank: true(rank との同時指定は400)
 */
export type UpdateParticipantInput = components['schemas']['UpdateParticipantRequest'];

/** CSVインポート結果(全行正常時のみ取り込む) */
export type CsvImportResult = components['schemas']['CsvImportResult'];
