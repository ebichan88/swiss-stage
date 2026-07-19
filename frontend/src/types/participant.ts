import type { ParticipantStatus, Rank } from './enums';

export interface Participant {
  id: string;
  name: string;
  organization: string | null;
  rank: Rank | null;
  seedOrder: number;
  status: ParticipantStatus;
  /** グループ割当(必須。常にいずれかのグループに帰属する) */
  groupId: string;
}

/** 対局・順位表で使う参加者の要約 */
export interface ParticipantSummary {
  id: string;
  name: string;
  organization: string | null;
}

/** POST /participants(backend: CreateParticipantRequest)。groupId 省略時は先頭グループに割当 */
export interface CreateParticipantInput {
  name: string;
  organization: string | null;
  rank: Rank | null;
  groupId?: string;
}

/**
 * PATCH /participants/{pid}(backend: UpdateParticipantRequest)。未指定の項目は変更しない。
 * 棋力を未入力に戻すには clearRank: true(rank との同時指定は400)。
 * groupId は割当先グループの変更(PREPARING 中のみ。未割当状態は存在しない)
 */
export interface UpdateParticipantInput {
  name?: string;
  organization?: string;
  rank?: Rank;
  clearRank?: boolean;
  groupId?: string;
  status?: ParticipantStatus;
}

/** CSVインポート結果(全行正常時のみ取り込む) */
export interface CsvImportResult {
  importedCount: number;
  participants: Participant[];
}
