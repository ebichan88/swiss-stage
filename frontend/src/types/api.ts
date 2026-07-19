import type { components } from './generated/api';

/**
 * API統一レスポンス型。
 * 個別のDTO型は schema/openapi.yaml からの生成型(./generated/api)を唯一の正とする。
 * ApiResponse はジェネリクスのため手書き(形は生成側の各レスポンス定義と同一)
 */
export type ApiResponse<T> =
  { success: true; data: T; meta: { timestamp: string } } | { success: false; error: ApiErrorBody };

export type ApiErrorBody = components['schemas']['ApiErrorBody'];
