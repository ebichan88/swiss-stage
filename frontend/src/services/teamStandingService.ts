import type { GroupTeamStandings } from '../types/team';
import { apiClient } from './apiClient';

export async function fetchTeamStandings(tournamentId: string): Promise<GroupTeamStandings[]> {
  return apiClient.get<GroupTeamStandings[]>(`/tournaments/${tournamentId}/team-standings`);
}
