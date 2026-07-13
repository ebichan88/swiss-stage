import { useQuery } from '@tanstack/react-query';

import { fetchStandings } from '../services/standingService';
import { queryKeys } from './queryKeys';

export function useStandings(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.standings(tournamentId),
    queryFn: () => fetchStandings(tournamentId),
    refetchInterval: 30_000,
  });
}
