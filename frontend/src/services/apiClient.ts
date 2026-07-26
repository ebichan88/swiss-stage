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

async function doFetch(path: string, init?: RequestInit): Promise<Response> {
  const headers = new Headers(init?.headers);
  // FormData はブラウザが boundary 付き Content-Type を付与するため手動指定しない
  if (!(init?.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  try {
    return await fetch(`${BASE_PATH}${path}`, {
      credentials: 'same-origin',
      ...init,
      headers,
    });
  } catch {
    throw new ApiError({
      code: 'NETWORK_ERROR',
      message: '通信に失敗しました。電波状況を確認して再度お試しください',
    });
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await doFetch(path, init);

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

/** ファイルダウンロード用。成功時はJSONパースせずBlobをそのまま返す(失敗時のみJSONをApiErrorとしてパースする) */
async function requestBlob(path: string): Promise<{ blob: Blob; filename: string }> {
  const response = await doFetch(path);
  if (!response.ok) {
    let body: ApiResponse<never>;
    try {
      body = (await response.json()) as ApiResponse<never>;
    } catch {
      throw new ApiError({ code: 'INTERNAL_ERROR', message: '予期しないエラーが発生しました' });
    }
    if (!body.success) {
      throw new ApiError(body.error);
    }
    throw new ApiError({ code: 'INTERNAL_ERROR', message: '予期しないエラーが発生しました' });
  }
  const blob = await response.blob();
  return {
    blob,
    filename: filenameFromContentDisposition(response.headers.get('Content-Disposition')),
  };
}

function filenameFromContentDisposition(header: string | null): string {
  if (!header) {
    return 'download.csv';
  }
  const star = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (star) {
    return decodeURIComponent(star[1]);
  }
  const plain = /filename="?([^";]+)"?/i.exec(header);
  return plain ? plain[1] : 'download.csv';
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: data === undefined ? undefined : JSON.stringify(data),
    }),
  postMultipart: <T>(path: string, formData: FormData) =>
    request<T>(path, { method: 'POST', body: formData }),
  getBlob: (path: string) => requestBlob(path),
  put: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(data) }),
  patch: <T>(path: string, data: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(data) }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
