import type { GeneratedRound, InputResultInput, Match, Round } from '../types/round';
import { apiClient } from './apiClient';

export async function fetchRounds(tournamentId: string): Promise<Round[]> {
  return apiClient.get<Round[]>(`/tournaments/${tournamentId}/rounds`);
}

/** 次ラウンドの組み合わせ生成。relaxations が空でなければUIに警告を表示する */
export async function generateNextRound(tournamentId: string): Promise<GeneratedRound> {
  return apiClient.post<GeneratedRound>(`/tournaments/${tournamentId}/rounds`);
}

export async function confirmRound(tournamentId: string, roundNumber: number): Promise<Round> {
  return apiClient.post<Round>(`/tournaments/${tournamentId}/rounds/${roundNumber}/confirm`);
}

export async function inputMatchResult(
  tournamentId: string,
  matchId: string,
  input: InputResultInput,
): Promise<Match> {
  return apiClient.put<Match>(`/tournaments/${tournamentId}/matches/${matchId}/result`, input);
}
