# エラーハンドリング設計書

## 1. 基本方針

- エラーは**分類 → コード → メッセージ → ログレベル**を一貫させる
- ユーザーに見せるメッセージは日本語で、次に取るべき行動がわかる文にする
- 内部情報(スタックトレース、SQL/クエリ、キー構造)は**絶対にレスポンスに含めない**

---

## 2. エラー分類とコード一覧

| 分類 | HTTPステータス | コード例 | ユーザー向けメッセージ例 | ログレベル |
|------|--------------|---------|----------------------|----------|
| バリデーション | 400 | `VALIDATION_ERROR` | 入力内容を確認してください | INFO |
| CSV形式不正 | 400 | `CSV_INVALID_FORMAT` | CSVの{n}行目に誤りがあります: {理由} | INFO |
| 未認証 | 401 | `UNAUTHORIZED` | ログインしてください | INFO |
| 権限なし | 403 | `FORBIDDEN` | この操作を行う権限がありません | WARN |
| 無効な共有トークン | 403 | `INVALID_SHARE_TOKEN` | このURLは無効になっています。運営者に確認してください | INFO |
| リソース未存在 | 404 | `TOURNAMENT_NOT_FOUND` 等 | 大会が見つかりません | INFO |
| 状態遷移違反 | 409 | `INVALID_STATE` | 大会開始前にはこの操作はできません | INFO |
| 更新競合 | 409 | `CONFLICT` | ほかの端末で更新されました。画面を更新して再度お試しください | INFO |
| ラウンド二重生成 | 409 | `ROUND_ALREADY_EXISTS` | このラウンドは既に生成されています | INFO |
| マッチング不能 | 422 | `PAIRING_CONSTRAINT_RELAXED` | 制約を緩和して組み合わせました(再戦が含まれます) | WARN |
| レート制限 | 429 | `RATE_LIMITED` | しばらく時間をおいて再度お試しください | WARN |
| システムエラー | 500 | `INTERNAL_ERROR` | 予期しないエラーが発生しました | ERROR |

新しいエラーコードを追加する場合は必ずこの表に追記すること。

---

## 3. バックエンド実装

### 例外クラス階層

```java
AppException (abstract, RuntimeException)
├── ValidationException      // 400
├── UnauthorizedException    // 401
├── ForbiddenException       // 403
├── NotFoundException        // 404
├── InvalidStateException    // 409(状態遷移違反)
├── ConflictException        // 409(楽観ロック競合)
└── PairingException         // 422(マッチング関連)
```

- `AppException` は `errorCode`(enum `ErrorCode`)と `userMessage` を持つ
- domain層は Spring に依存しないため、domain層の例外(`DomainException`)を application 層で `AppException` に変換する

### グローバルハンドラー

- `@RestControllerAdvice` で一元処理(`presentation/GlobalExceptionHandler`)
- `AppException` → 対応するHTTPステータス + 統一エラーレスポンス
- `MethodArgumentNotValidException` → 400 + `details` にフィールドエラー
- その他の `Exception` → 500 + `INTERNAL_ERROR`(詳細はログのみ)

### ログ出力ルール

- 構造化ログ(JSON)で出力: `timestamp`, `level`, `errorCode`, `tournamentId`, `userSub`, `requestId`, `message`
- **氏名・所属などの個人情報はログに出力しない**(IDのみ)
- ERROR はスタックトレース付き、INFO/WARN はメッセージのみ
- `requestId` はフィルターで生成しMDCに設定、レスポンスヘッダー `X-Request-Id` にも付与

---

## 4. フロントエンド実装

### APIクライアント層での処理

- `services/apiClient.ts` で fetch をラップし、`success: false` のとき `ApiError`(code, message, details)を throw
- ネットワークエラー(オフライン等)は `NETWORK_ERROR` として統一

### 表示方法の使い分け

| ケース | 表示方法 |
|-------|---------|
| フォームバリデーション(400) | フィールド下にインラインエラー表示 |
| 操作失敗(403/404/409) | Snackbar(トースト)で `error.message` をそのまま表示 |
| 競合(409 CONFLICT) | Snackbar + 「再読み込み」アクションボタン |
| 結果送信のネットワークエラー | ダイアログ + 「再送信」ボタン(会場の電波不良を想定) |
| 致命的エラー(500, 予期しない例外) | ErrorBoundary でエラー画面表示 + 再読み込み誘導 |

### ErrorBoundary

- ルートに1つ + 大会管理レイアウト(`TournamentLayout`)に1つ配置
- 画面全体が白画面になることを防ぐ

---

## 5. 判断に迷ったときの指針

- 「ユーザーの操作で直せるか?」→ YES なら 4xx + 直し方がわかるメッセージ
- 「見せてよい情報か?」→ 迷ったら見せない(汎用メッセージ + ログに詳細)
- 「大会進行が止まるか?」→ YES ならリトライ手段を必ずUIに用意する
