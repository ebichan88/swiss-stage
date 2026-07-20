/**
 * API統一レスポンス型。
 * 定義: .claude/01_development_docs/07_type_definitions.md(バックエンドと同期)
 */
export type ApiResponse<T> =
  { success: true; data: T; meta: { timestamp: string } } | { success: false; error: ApiErrorBody };

export interface ApiErrorBody {
  code: string;
  message: string;
  details?: { field: string; reason: string }[];
}
