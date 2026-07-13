import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError } from '../services/apiClient';
import { fetchMe, logout, testLogin } from '../services/authService';
import type { Me } from '../types/auth';
import { queryKeys } from './queryKeys';

/** 未ログイン(401)はエラーではなく user: null として扱う */
async function fetchMeOrNull(): Promise<Me | null> {
  try {
    return await fetchMe();
  } catch (error) {
    if (error instanceof ApiError && error.code === 'UNAUTHORIZED') {
      return null;
    }
    throw error;
  }
}

export function useAuth() {
  const { data, isPending } = useQuery({
    queryKey: queryKeys.me,
    queryFn: fetchMeOrNull,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
  return { user: data ?? null, isLoading: isPending };
}

/** 開発用ログイン(Google OAuth2はPhase 5で置き換え) */
export function useTestLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: testLogin,
    onSuccess: (me) => {
      queryClient.setQueryData(queryKeys.me, me);
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(queryKeys.me, null);
      queryClient.removeQueries({ queryKey: queryKeys.tournaments });
    },
  });
}
