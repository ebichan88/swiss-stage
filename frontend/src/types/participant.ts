import type { ParticipantStatus, Rank } from './enums';

export interface Participant {
  id: string;
  name: string;
  organization: string | null;
  rank: Rank | null;
  seedOrder: number;
  status: ParticipantStatus;
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
}

/** PATCH /participants/{pid}(backend: UpdateParticipantRequest)。nullの項目は変更しない */
export interface UpdateParticipantInput {
  name?: string;
  organization?: string;
  rank?: Rank;
  status?: ParticipantStatus;
}

/** CSVインポート結果(全行正常時のみ取り込む) */
export interface CsvImportResult {
  importedCount: number;
  participants: Participant[];
}
