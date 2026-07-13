import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  addParticipant,
  deleteParticipant,
  fetchParticipants,
  importParticipantsCsv,
  updateParticipant,
} from '../services/participantService';
import type { CreateParticipantInput, UpdateParticipantInput } from '../types/participant';
import { queryKeys } from './queryKeys';

export function useParticipants(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.participants(tournamentId),
    queryFn: () => fetchParticipants(tournamentId),
  });
}

export function useAddParticipant(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateParticipantInput) => addParticipant(tournamentId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.participants(tournamentId) });
    },
  });
}

export function useImportParticipantsCsv(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => importParticipantsCsv(tournamentId, file),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.participants(tournamentId) });
    },
  });
}

export function useUpdateParticipant(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      participantId,
      input,
    }: {
      participantId: string;
      input: UpdateParticipantInput;
    }) => updateParticipant(tournamentId, participantId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.participants(tournamentId) });
      // 棄権は以降の組み合わせ・順位にも影響する
      void queryClient.invalidateQueries({ queryKey: queryKeys.standings(tournamentId) });
    },
  });
}

export function useDeleteParticipant(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (participantId: string) => deleteParticipant(tournamentId, participantId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.participants(tournamentId) });
    },
  });
}
