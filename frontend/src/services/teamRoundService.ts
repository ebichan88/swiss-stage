import type {
  GeneratedTeamRound,
  InputTeamMatchResultInput,
  TeamMatch,
  TeamRound,
} from '../types/team';
import { apiClient } from './apiClient';

export async function fetchTeamRounds(tournamentId: string): Promise<TeamRound[]> {
  return apiClient.get<TeamRound[]>(`/tournaments/${tournamentId}/team-rounds`);
}

/** 次ラウンドの組み合わせ生成。relaxations が空でなければUIに警告を表示する */
export async function generateNextTeamRound(tournamentId: string): Promise<GeneratedTeamRound> {
  return apiClient.post<GeneratedTeamRound>(`/tournaments/${tournamentId}/team-rounds`);
}

export async function confirmTeamRound(
  tournamentId: string,
  roundNumber: number,
): Promise<TeamRound> {
  return apiClient.post<TeamRound>(
    `/tournaments/${tournamentId}/team-rounds/${roundNumber}/confirm`,
  );
}

export async function inputTeamMatchResult(
  tournamentId: string,
  matchId: string,
  input: InputTeamMatchResultInput,
): Promise<TeamMatch> {
  return apiClient.put<TeamMatch>(
    `/tournaments/${tournamentId}/team-matches/${matchId}/result`,
    input,
  );
}
