import type { Group, GroupInput } from '../types/group';
import type { Participant } from '../types/participant';
import { apiClient } from './apiClient';

export async function fetchGroups(tournamentId: string): Promise<Group[]> {
  return apiClient.get<Group[]>(`/tournaments/${tournamentId}/groups`);
}

export async function createGroup(tournamentId: string, input: GroupInput): Promise<Group> {
  return apiClient.post<Group>(`/tournaments/${tournamentId}/groups`, input);
}

export async function renameGroup(
  tournamentId: string,
  groupId: string,
  input: GroupInput,
): Promise<Group> {
  return apiClient.patch<Group>(`/tournaments/${tournamentId}/groups/${groupId}`, input);
}

export async function deleteGroup(tournamentId: string, groupId: string): Promise<void> {
  await apiClient.delete<void>(`/tournaments/${tournamentId}/groups/${groupId}`);
}

/** 段級位で全ACTIVE参加者を一括振り分け。更新後の参加者一覧が返る */
export async function autoAssignGroups(tournamentId: string): Promise<Participant[]> {
  return apiClient.post<Participant[]>(`/tournaments/${tournamentId}/groups/auto-assign`, {});
}
