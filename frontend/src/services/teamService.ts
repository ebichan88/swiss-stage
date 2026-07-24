import type {
  AddTeamMemberInput,
  CreateTeamInput,
  Team,
  TeamCsvImportResult,
  UpdateTeamInput,
  UpdateTeamMemberInput,
} from '../types/team';
import { apiClient } from './apiClient';

export async function fetchTeams(tournamentId: string): Promise<Team[]> {
  return apiClient.get<Team[]>(`/tournaments/${tournamentId}/teams`);
}

export async function createTeam(tournamentId: string, input: CreateTeamInput): Promise<Team> {
  return apiClient.post<Team>(`/tournaments/${tournamentId}/teams`, input);
}

export async function importTeamsCsv(
  tournamentId: string,
  file: File,
): Promise<TeamCsvImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  return apiClient.postMultipart<TeamCsvImportResult>(
    `/tournaments/${tournamentId}/teams/csv-import`,
    formData,
  );
}

export async function updateTeam(
  tournamentId: string,
  teamId: string,
  input: UpdateTeamInput,
): Promise<Team> {
  return apiClient.patch<Team>(`/tournaments/${tournamentId}/teams/${teamId}`, input);
}

export async function deleteTeam(tournamentId: string, teamId: string): Promise<void> {
  await apiClient.delete<void>(`/tournaments/${tournamentId}/teams/${teamId}`);
}

export async function addTeamMember(
  tournamentId: string,
  teamId: string,
  input: AddTeamMemberInput,
): Promise<Team> {
  return apiClient.post<Team>(`/tournaments/${tournamentId}/teams/${teamId}/members`, input);
}

export async function updateTeamMember(
  tournamentId: string,
  teamId: string,
  memberId: string,
  input: UpdateTeamMemberInput,
): Promise<Team> {
  return apiClient.patch<Team>(
    `/tournaments/${tournamentId}/teams/${teamId}/members/${memberId}`,
    input,
  );
}

export async function deleteTeamMember(
  tournamentId: string,
  teamId: string,
  memberId: string,
): Promise<Team> {
  return apiClient.delete<Team>(`/tournaments/${tournamentId}/teams/${teamId}/members/${memberId}`);
}
