/** パス生成の一元管理(.claude/03_library_docs/04_react_router_patterns.md §4) */
export const paths = {
  top: '/',
  login: '/login',
  tournaments: '/tournaments',
  tournamentNew: '/tournaments/new',
  tournament: (id: string) => `/tournaments/${id}`,
  participants: (id: string) => `/tournaments/${id}/participants`,
  rounds: (id: string) => `/tournaments/${id}/rounds`,
  standings: (id: string) => `/tournaments/${id}/standings`,
  settings: (id: string) => `/tournaments/${id}/settings`,
  shared: (token: string) => `/s/${token}`,
  sharedMatch: (token: string, matchId: string) => `/s/${token}/matches/${matchId}`,
} as const;
