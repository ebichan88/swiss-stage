import type { ParticipantStatus, Rank } from './enums';

export interface Participant {
  id: string;
  name: string;
  organization: string | null;
  rank: Rank | null;
  seedOrder: number;
  status: ParticipantStatus;
  /** グループ割当(null = 未割当。グループなし大会は常にnull) */
  groupId: string | null;
}

/** 対局・順位表で使う参加者の要約 */
export interface ParticipantSummary {
  id: string;
  name: string;
  organization: string | null;
}

/** POST /participants(backend: CreateParticipantRequest) */
export interface CreateParticipantInput {
  name: string;
  organization: string | null;
  rank: Rank | null;
  groupId?: string | null;
}

/**
 * PATCH /participants/{pid}(backend: UpdateParticipantRequest)。未指定の項目は変更しない。
 * 棋力を未入力に戻すには clearRank: true(rank との同時指定は400)。
 * グループ割当も同型: 未割当に戻すには clearGroup: true(変更は PREPARING 中のみ)
 */
export interface UpdateParticipantInput {
  name?: string;
  organization?: string;
  rank?: Rank;
  clearRank?: boolean;
  groupId?: string;
  clearGroup?: boolean;
  status?: ParticipantStatus;
}

/** CSVインポート結果(全行正常時のみ取り込む) */
export interface CsvImportResult {
  importedCount: number;
  participants: Participant[];
}
