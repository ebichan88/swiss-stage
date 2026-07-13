import type { InputResultInput, Match } from '../types/round';
import type { SharedTournament } from '../types/shared';
import { apiClient } from './apiClient';

/** 共有トークン経由のAPI(認証不要。S10/S11) */
export async function fetchSharedTournament(token: string): Promise<SharedTournament> {
  return apiClient.get<SharedTournament>(`/shared/${token}`);
}

export async function inputSharedResult(
  token: string,
  matchId: string,
  input: InputResultInput,
): Promise<Match> {
  return apiClient.put<Match>(`/shared/${token}/matches/${matchId}/result`, input);
}
