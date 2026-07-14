import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createTournament,
  deleteTournament,
  fetchTournament,
  fetchTournaments,
  finishTournament,
  regenerateShareToken,
  startTournament,
  updateTournament,
} from '../services/tournamentService';
import type { CreateTournamentInput, UpdateTournamentInput } from '../types/tournament';
import { queryKeys } from './queryKeys';

export function useTournaments() {
  return useQuery({ queryKey: queryKeys.tournaments, queryFn: fetchTournaments });
}

export function useTournament(id: string) {
  return useQuery({ queryKey: queryKeys.tournament(id), queryFn: () => fetchTournament(id) });
}

export function useCreateTournament() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateTournamentInput) => createTournament(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.tournaments });
    },
  });
}

export function useUpdateTournament(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: UpdateTournamentInput) => updateTournament(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.tournament(id) });
    },
  });
}

export function useDeleteTournament(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => deleteTournament(id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: queryKeys.tournament(id) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.tournaments });
    },
  });
}

export function useStartTournament(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => startTournament(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.tournament(id) });
    },
  });
}

export function useFinishTournament(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => finishTournament(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.tournament(id) });
    },
  });
}

export function useRegenerateShareToken(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => regenerateShareToken(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.tournament(id) });
    },
  });
}
