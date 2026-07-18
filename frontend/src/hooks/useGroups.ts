import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  autoAssignGroups,
  createGroup,
  deleteGroup,
  fetchGroups,
  renameGroup,
} from '../services/groupService';
import type { GroupInput } from '../types/group';
import { queryKeys } from './queryKeys';

export function useGroups(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.groups(tournamentId),
    queryFn: () => fetchGroups(tournamentId),
  });
}

export function useCreateGroup(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: GroupInput) => createGroup(tournamentId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.groups(tournamentId) });
    },
  });
}

export function useRenameGroup(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ groupId, input }: { groupId: string; input: GroupInput }) =>
      renameGroup(tournamentId, groupId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.groups(tournamentId) });
    },
  });
}

export function useDeleteGroup(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: string) => deleteGroup(tournamentId, groupId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.groups(tournamentId) });
      // 削除で割当済み参加者が未割当に戻る
      void queryClient.invalidateQueries({ queryKey: queryKeys.participants(tournamentId) });
    },
  });
}

export function useAutoAssignGroups(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => autoAssignGroups(tournamentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.participants(tournamentId) });
    },
  });
}
