/**
 * バックエンドの統一レスポンス形式を組み立てる純粋関数。
 * msw/node(Node専用)に依存しないよう server.ts から分離している。
 * Storybook(ブラウザ)からもこのファイル経由で使う(server.ts 経由だと
 * setupServer(msw/node)がVite依存解決に巻き込まれてビルドが壊れる)
 */

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
