import type { ApiErrorBody, ApiResponse } from '../types/api';

const BASE_PATH = '/api/v1';

/**
 * APIエラー。error.code は .claude/01_development_docs/06_error_handling_design.md の表に従う。
 * ネットワーク断は code: 'NETWORK_ERROR' に正規化する。
 */
export class ApiError extends Error {
  readonly code: string;
  readonly details?: ApiErrorBody['details'];

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.code = body.code;
    this.details = body.details;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BASE_PATH}${path}`, {
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', ...init?.headers },
      ...init,
    });
  } catch {
    throw new ApiError({
      code: 'NETWORK_ERROR',
      message: '通信に失敗しました。電波状況を確認して再度お試しください',
    });
  }

  if (response.status === 204) {
    return undefined as T;
  }

  let body: ApiResponse<T>;
  try {
    body = (await response.json()) as ApiResponse<T>;
  } catch {
    throw new ApiError({
      code: 'INTERNAL_ERROR',
      message: '予期しないエラーが発生しました',
    });
  }

  if (!body.success) {
    throw new ApiError(body.error);
  }
  return body.data;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: data === undefined ? undefined : JSON.stringify(data),
    }),
  put: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(data) }),
  patch: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(data) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
