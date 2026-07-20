import type { components } from './generated/api';

/**
 * 共有ページ(S10)向けの大会集約。
 * shareToken・ownerSub は含まれない(13_security_design.md §6)
 */
export type SharedTournament = components['schemas']['SharedTournament'];

export type SharedTournamentSummary = components['schemas']['SharedTournamentSummary'];
