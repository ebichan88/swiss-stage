import type { GroupStandings } from '../types/standing';
import { apiClient } from './apiClient';

export async function fetchStandings(tournamentId: string): Promise<GroupStandings[]> {
  return apiClient.get<GroupStandings[]>(`/tournaments/${tournamentId}/standings`);
}
