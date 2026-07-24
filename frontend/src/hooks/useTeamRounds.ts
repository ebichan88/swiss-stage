import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  confirmTeamRound,
  fetchTeamRounds,
  generateNextTeamRound,
  inputTeamMatchResult,
} from '../services/teamRoundService';
import type { InputTeamMatchResultInput } from '../types/team';
import { queryKeys } from './queryKeys';

/** 大会当日に使う画面のためポーリングする(10_frontend_design.md §3) */
export function useTeamRounds(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.teamRounds(tournamentId),
    queryFn: () => fetchTeamRounds(tournamentId),
    refetchInterval: 30_000,
  });
}

export function useGenerateNextTeamRound(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => generateNextTeamRound(tournamentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teamRounds(tournamentId) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.tournament(tournamentId) });
    },
  });
}

export function useConfirmTeamRound(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (roundNumber: number) => confirmTeamRound(tournamentId, roundNumber),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teamRounds(tournamentId) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.teamStandings(tournamentId) });
    },
  });
}

export function useInputTeamMatchResult(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ matchId, input }: { matchId: string; input: InputTeamMatchResultInput }) =>
      inputTeamMatchResult(tournamentId, matchId, input),
    // 楽観ロック競合(409)時も最新のversionを取り直せるよう、失敗時にも再取得する
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.teamRounds(tournamentId) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.teamStandings(tournamentId) });
    },
  });
}
