import type { GameType, TournamentStatus, Visibility } from './enums';

/** 大会DTO(07_type_definitions.md §3)。shareToken は運営者にのみ返る */
export interface Tournament {
  id: string;
  name: string;
  gameType: GameType;
  totalRounds: number;
  currentRound: number;
  status: TournamentStatus;
  visibility: Visibility;
  shareToken: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** POST /tournaments(backend: CreateTournamentRequest) */
export interface CreateTournamentInput {
  name: string;
  gameType: GameType;
  totalRounds: number;
}

/** PATCH /tournaments/{id}(backend: UpdateTournamentRequest)。nullの項目は変更しない */
export interface UpdateTournamentInput {
  name?: string;
  visibility?: Visibility;
  version: number;
}
