import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  addTeamMember,
  createTeam,
  deleteTeam,
  deleteTeamMember,
  fetchTeams,
  importTeamsCsv,
  updateTeam,
  updateTeamMember,
} from '../services/teamService';
import type {
  AddTeamMemberInput,
  CreateTeamInput,
  UpdateTeamInput,
  UpdateTeamMemberInput,
} from '../types/team';
import { queryKeys } from './queryKeys';

export function useTeams(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.teams(tournamentId),
    queryFn: () => fetchTeams(tournamentId),
  });
}

export function useCreateTeam(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CreateTeamInput) => createTeam(tournamentId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams(tournamentId) });
    },
  });
}

export function useImportTeamsCsv(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => importTeamsCsv(tournamentId, file),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams(tournamentId) });
    },
  });
}

export function useUpdateTeam(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, input }: { teamId: string; input: UpdateTeamInput }) =>
      updateTeam(tournamentId, teamId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams(tournamentId) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.teamStandings(tournamentId) });
    },
  });
}

export function useDeleteTeam(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (teamId: string) => deleteTeam(tournamentId, teamId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams(tournamentId) });
    },
  });
}

export function useAddTeamMember(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, input }: { teamId: string; input: AddTeamMemberInput }) =>
      addTeamMember(tournamentId, teamId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams(tournamentId) });
    },
  });
}

export function useUpdateTeamMember(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      teamId,
      memberId,
      input,
    }: {
      teamId: string;
      memberId: string;
      input: UpdateTeamMemberInput;
    }) => updateTeamMember(tournamentId, teamId, memberId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams(tournamentId) });
    },
  });
}

export function useDeleteTeamMember(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, memberId }: { teamId: string; memberId: string }) =>
      deleteTeamMember(tournamentId, teamId, memberId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teams(tournamentId) });
    },
  });
}
