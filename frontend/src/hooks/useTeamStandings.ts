import { useQuery } from '@tanstack/react-query';

import { fetchTeamStandings } from '../services/teamStandingService';
import { queryKeys } from './queryKeys';

export function useTeamStandings(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.teamStandings(tournamentId),
    queryFn: () => fetchTeamStandings(tournamentId),
    refetchInterval: 30_000,
  });
}
