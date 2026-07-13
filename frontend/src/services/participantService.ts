import type {
  CreateParticipantInput,
  CsvImportResult,
  Participant,
  UpdateParticipantInput,
} from '../types/participant';
import { apiClient } from './apiClient';

export async function fetchParticipants(tournamentId: string): Promise<Participant[]> {
  return apiClient.get<Participant[]>(`/tournaments/${tournamentId}/participants`);
}

export async function addParticipant(
  tournamentId: string,
  input: CreateParticipantInput,
): Promise<Participant> {
  return apiClient.post<Participant>(`/tournaments/${tournamentId}/participants`, input);
}

export async function importParticipantsCsv(
  tournamentId: string,
  file: File,
): Promise<CsvImportResult> {
  const formData = new FormData();
  formData.append('file', file);
  return apiClient.postMultipart<CsvImportResult>(
    `/tournaments/${tournamentId}/participants/import`,
    formData,
  );
}

export async function updateParticipant(
  tournamentId: string,
  participantId: string,
  input: UpdateParticipantInput,
): Promise<Participant> {
  return apiClient.patch<Participant>(
    `/tournaments/${tournamentId}/participants/${participantId}`,
    input,
  );
}

export async function deleteParticipant(
  tournamentId: string,
  participantId: string,
): Promise<void> {
  await apiClient.delete<void>(`/tournaments/${tournamentId}/participants/${participantId}`);
}
