import type { Match, ReportMatchResultInput } from '../types/round';
import type { SharedTournament } from '../types/shared';
import type { ReportTeamMatchResultInput, TeamMatch } from '../types/team';
import { apiClient } from './apiClient';

/** 共有トークン経由のAPI(認証不要。S10/S11) */
export async function fetchSharedTournament(token: string): Promise<SharedTournament> {
  return apiClient.get<SharedTournament>(`/shared/${token}`);
}

/** 自己申告(reportedBy側の主張)。両者の申告が一致すると自動確定する */
export async function inputSharedResult(
  token: string,
  matchId: string,
  input: ReportMatchResultInput,
): Promise<Match> {
  return apiClient.put<Match>(`/shared/${token}/matches/${matchId}/result`, input);
}

/** 団体戦の結果自己申告(ボード配列をまとめて送信。確定はボード単位で独立に行う) */
export async function inputSharedTeamMatchResult(
  token: string,
  matchId: string,
  input: ReportTeamMatchResultInput,
): Promise<TeamMatch> {
  return apiClient.put<TeamMatch>(`/shared/${token}/team-matches/${matchId}/result`, input);
}
