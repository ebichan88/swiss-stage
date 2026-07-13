import type { Me } from '../types/auth';
import { apiClient } from './apiClient';

export async function fetchMe(): Promise<Me> {
  return apiClient.get<Me>('/auth/me');
}

export async function logout(): Promise<void> {
  await apiClient.post<void>('/auth/logout');
}

/** 開発・テスト用の仮ログイン(local/testプロファイル限定。Google OAuth2はPhase 5) */
export async function testLogin(): Promise<Me> {
  return apiClient.post<Me>('/auth/test-login');
}
