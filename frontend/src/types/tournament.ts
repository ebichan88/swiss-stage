import type { components } from './generated/api';

/** 大会DTO。shareToken は運営者にのみ返る */
export type Tournament = components['schemas']['Tournament'];

/** POST /tournaments */
export type CreateTournamentInput = components['schemas']['CreateTournamentRequest'];

/** PATCH /tournaments/{id}。未指定の項目は変更しない */
export type UpdateTournamentInput = components['schemas']['UpdateTournamentRequest'];
