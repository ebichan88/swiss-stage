/** TanStack Query の queryKey 一元管理(10_frontend_design.md §3) */
export const queryKeys = {
  me: ['auth', 'me'] as const,
  tournaments: ['tournaments'] as const,
  tournament: (id: string) => ['tournaments', id] as const,
  participants: (id: string) => ['tournaments', id, 'participants'] as const,
  rounds: (id: string) => ['tournaments', id, 'rounds'] as const,
  standings: (id: string) => ['tournaments', id, 'standings'] as const,
  shared: (token: string) => ['shared', token] as const,
};
