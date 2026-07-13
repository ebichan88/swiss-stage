import { setupServer } from 'msw/node';

/** APIモックはMSWで行う(09_test_strategy.md §4: fetchの手モック禁止)。ハンドラは各テストで登録する */
export const server = setupServer();

/** バックエンドの統一成功レスポンス(ApiResponse<T>)を組み立てる */
export function apiSuccess<T>(data: T) {
  return { success: true, data, meta: { timestamp: '2026-07-13T10:00:00+09:00' } };
}

/** バックエンドの統一エラーレスポンスを組み立てる */
export function apiError(
  code: string,
  message: string,
  details?: { field: string; reason: string }[],
) {
  return { success: false, error: { code, message, ...(details ? { details } : {}) } };
}
