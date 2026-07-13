import type { Standing } from '../types/standing';
import { apiClient } from './apiClient';

export async function fetchStandings(tournamentId: string): Promise<Standing[]> {
  return apiClient.get<Standing[]>(`/tournaments/${tournamentId}/standings`);
}
