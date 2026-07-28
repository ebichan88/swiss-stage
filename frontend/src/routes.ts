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
  matchResults: (id: string) => `/tournaments/${id}/match-results`,
  settings: (id: string) => `/tournaments/${id}/settings`,
  printRoster: (id: string) => `/tournaments/${id}/print/roster`,
  printMatchResults: (id: string) => `/tournaments/${id}/print/match-results`,
  printMatchCards: (id: string) => `/tournaments/${id}/print/match-cards`,
  shared: (token: string) => `/s/${token}`,
  sharedMatch: (token: string, matchId: string) => `/s/${token}/matches/${matchId}`,
  sharedTeamMatch: (token: string, matchId: string) => `/s/${token}/team-matches/${matchId}`,
} as const;
