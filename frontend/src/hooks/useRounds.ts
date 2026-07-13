import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  confirmRound,
  fetchRounds,
  generateNextRound,
  inputMatchResult,
} from '../services/roundService';
import type { InputResultInput } from '../types/round';
import { queryKeys } from './queryKeys';

/** 大会当日に使う画面のためポーリングする(10_frontend_design.md §3) */
export function useRounds(tournamentId: string) {
  return useQuery({
    queryKey: queryKeys.rounds(tournamentId),
    queryFn: () => fetchRounds(tournamentId),
    refetchInterval: 30_000,
  });
}

export function useGenerateNextRound(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => generateNextRound(tournamentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.rounds(tournamentId) });
      // currentRound が進むため大会情報も更新する
      void queryClient.invalidateQueries({ queryKey: queryKeys.tournament(tournamentId) });
    },
  });
}

export function useConfirmRound(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (roundNumber: number) => confirmRound(tournamentId, roundNumber),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.rounds(tournamentId) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.standings(tournamentId) });
    },
  });
}

export function useInputMatchResult(tournamentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ matchId, input }: { matchId: string; input: InputResultInput }) =>
      inputMatchResult(tournamentId, matchId, input),
    // 楽観ロック競合(409)時も最新のversionを取り直せるよう、失敗時にも再取得する
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.rounds(tournamentId) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.standings(tournamentId) });
    },
  });
}
