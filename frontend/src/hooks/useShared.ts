import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { fetchSharedTournament, inputSharedResult } from '../services/sharedService';
import type { ReportMatchResultInput } from '../types/round';
import { queryKeys } from './queryKeys';

/**
 * 共有ページ(S10)の大会集約。会場で開きっぱなしにされるため、
 * 30秒ごとの自動更新で組み合わせ・順位を追従させる。
 * 無効トークン(INVALID_SHARE_TOKEN)はリトライしても回復しないためリトライしない。
 */
export function useSharedTournament(token: string) {
  return useQuery({
    queryKey: queryKeys.shared(token),
    queryFn: () => fetchSharedTournament(token),
    refetchInterval: 30_000,
    retry: false,
  });
}

/** 共有トークン経由の結果自己申告(S11)。成功時は共有集約を再取得する */
export function useInputSharedResult(token: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ matchId, input }: { matchId: string; input: ReportMatchResultInput }) =>
      inputSharedResult(token, matchId, input),
    onSettled: () => {
      // 競合(409)時も最新状態を取り直して画面を追従させる
      void queryClient.invalidateQueries({ queryKey: queryKeys.shared(token) });
    },
  });
}
