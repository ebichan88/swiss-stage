import type { CreateTournamentInput, Tournament, UpdateTournamentInput } from '../types/tournament';
import { apiClient } from './apiClient';

export async function fetchTournaments(): Promise<Tournament[]> {
  return apiClient.get<Tournament[]>('/tournaments');
}

export async function fetchTournament(id: string): Promise<Tournament> {
  return apiClient.get<Tournament>(`/tournaments/${id}`);
}

export async function createTournament(input: CreateTournamentInput): Promise<Tournament> {
  return apiClient.post<Tournament>('/tournaments', input);
}

export async function updateTournament(
  id: string,
  input: UpdateTournamentInput,
): Promise<Tournament> {
  return apiClient.patch<Tournament>(`/tournaments/${id}`, input);
}

export async function deleteTournament(id: string): Promise<void> {
  await apiClient.delete<void>(`/tournaments/${id}`);
}

export async function startTournament(id: string): Promise<Tournament> {
  return apiClient.post<Tournament>(`/tournaments/${id}/start`);
}

export async function finishTournament(id: string): Promise<Tournament> {
  return apiClient.post<Tournament>(`/tournaments/${id}/finish`);
}

/** 共有トークンの発行・再発行(旧トークンは即時無効) */
export async function regenerateShareToken(id: string): Promise<Tournament> {
  return apiClient.post<Tournament>(`/tournaments/${id}/share-token/regenerate`);
}
