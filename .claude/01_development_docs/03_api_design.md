# API設計書

## 1. 基本方針

- RESTful API、ベースパス `/api/v1`
- リクエスト/レスポンスは JSON(UTF-8)
- 認証: 運営者は Google OAuth2 + セッション(JWT)、参加者(閲覧・結果入力)は共有トークン
- 命名: パスは複数形ケバブケース、JSONプロパティは camelCase

---

## 2. レスポンス統一フォーマット

### 成功時

```json
{
  "success": true,
  "data": { ... },
  "meta": { "timestamp": "2026-07-12T10:00:00+09:00" }
}
```

### エラー時

```json
{
  "success": false,
  "error": {
    "code": "TOURNAMENT_NOT_FOUND",
    "message": "大会が見つかりません",
    "details": [ { "field": "name", "reason": "必須です" } ]
  }
}
```

- `code` は `06_error_handling_design.md` のエラーコード一覧に従う
- `message` はそのままUIに表示できる日本語
- `details` はバリデーションエラー時のみ

---

## 3. エンドポイント一覧(MVP)

### 認証

| メソッド | パス | 説明 | 認証 |
|---------|------|------|------|
| GET | `/api/v1/auth/login` | Google OAuth2へリダイレクト(`/api/v1/auth/login/google` を経由) | - |
| GET | `/api/v1/auth/callback` | OAuth2コールバック(Spring Securityが処理し、JWT Cookie発行後にSPAへ戻す) | - |
| POST | `/api/v1/auth/logout` | ログアウト | 運営者 |
| GET | `/api/v1/auth/me` | ログイン中ユーザー情報 | 運営者 |
| POST | `/api/v1/auth/test-login` | 開発・テスト用の仮ログイン(**local/testプロファイル限定**。本番には存在しない) | - |

### 大会

| メソッド | パス | 説明 | 認証 |
|---------|------|------|------|
| GET | `/api/v1/tournaments` | 自分の大会一覧 | 運営者 |
| POST | `/api/v1/tournaments` | 大会作成 | 運営者 |
| GET | `/api/v1/tournaments/{id}` | 大会詳細 | 運営者 |
| PATCH | `/api/v1/tournaments/{id}` | 大会更新(名前・公開範囲・結果入力許可等) | 運営者 |
| DELETE | `/api/v1/tournaments/{id}` | 大会削除(物理削除) | 運営者 |
| POST | `/api/v1/tournaments/{id}/start` | 大会開始(PREPARING→IN_PROGRESS) | 運営者 |
| POST | `/api/v1/tournaments/{id}/finish` | 大会終了 | 運営者 |

### 参加者

| メソッド | パス | 説明 | 認証 |
|---------|------|------|------|
| GET | `/api/v1/tournaments/{id}/participants` | 参加者一覧 | 運営者 |
| POST | `/api/v1/tournaments/{id}/participants` | 参加者追加 | 運営者 |
| POST | `/api/v1/tournaments/{id}/participants/import` | CSVインポート(multipart) | 運営者 |
| PATCH | `/api/v1/tournaments/{id}/participants/{pid}` | 参加者更新・棄権処理・グループ割当変更 | 運営者 |
| DELETE | `/api/v1/tournaments/{id}/participants/{pid}` | 参加者削除(開始前のみ) | 運営者 |

### グループ(棋力帯クラス分け。すべて PREPARING 中のみ変更可)

| メソッド | パス | 説明 | 認証 |
|---------|------|------|------|
| GET | `/api/v1/tournaments/{id}/groups` | グループ一覧(作成順) | 運営者 |
| POST | `/api/v1/tournaments/{id}/groups` | グループ作成(最大10個。同名重複は400) | 運営者 |
| PATCH | `/api/v1/tournaments/{id}/groups/{gid}` | グループ改名 | 運営者 |
| DELETE | `/api/v1/tournaments/{id}/groups/{gid}` | グループ削除(割当済み参加者は未割当に戻す) | 運営者 |
| POST | `/api/v1/tournaments/{id}/groups/auto-assign` | 段級位で全ACTIVE参加者を一括振り分け(`05_swiss_pairing_algorithm.md` §2.4)。レスポンスは更新後の参加者一覧 | 運営者 |

### ラウンド・対局

| メソッド | パス | 説明 | 認証 |
|---------|------|------|------|
| GET | `/api/v1/tournaments/{id}/rounds` | ラウンド一覧(対局含む) | 運営者 |
| POST | `/api/v1/tournaments/{id}/rounds` | 次ラウンドの組み合わせ生成。レスポンスは `GeneratedRound`(round + relaxations) | 運営者 |
| POST | `/api/v1/tournaments/{id}/rounds/{n}/confirm` | ラウンド確定 | 運営者 |
| PUT | `/api/v1/tournaments/{id}/matches/{mid}/result` | 対局結果入力 | 運営者 |
| GET | `/api/v1/tournaments/{id}/standings` | 順位表取得。レスポンスは `GroupStandings[]`(グループなし大会は group=null の単一要素) | 運営者 |

### 共有(トークン保持者向け。IPレート制限あり)

| メソッド | パス | 説明 | 認証 |
|---------|------|------|------|
| GET | `/api/v1/shared/{token}` | 共有ページ用の大会集約(`SharedTournament` = 大会概要 + ラウンド + 順位表)。`shareToken`・`ownerSub` は含めない | トークン |
| PUT | `/api/v1/shared/{token}/matches/{mid}/result` | トークン経由の結果入力(大会設定 `resultInputEnabled` が有効な場合のみ) | トークン |
| POST | `/api/v1/tournaments/{id}/share-token/regenerate` | トークン発行・再発行(旧トークン即時無効) | 運営者 |

---

## 4. 設計ルール

1. **状態を変える操作は動詞サブリソース**(`/start`, `/confirm`, `/regenerate`)を許容する。ドメインの操作をそのまま表現するため。
2. **HTTPステータス**: 200(取得/更新), 201(作成), 204(削除), 400(バリデーション), 401(未認証), 403(権限なし), 404(未存在), 409(競合・状態遷移違反), 500(サーバーエラー)
3. **べき等性**: 結果入力(PUT)はべき等。ラウンド生成(POST)は Round status で二重生成を防ぐ(409を返す)。
4. **楽観ロック**: 更新系リクエストには `version` を含め、競合時は 409 + `CONFLICT` コードを返す。
5. **ページネーション**: MVPでは参加者最大300名のため不要。将来必要になったら `cursor` ベースで追加。
6. **CSVインポート**: ヘッダー行必須(`氏名,所属,段級位` の3列、または `氏名,所属,段級位,グループ` の4列。グループ列は任意)。グループ列の値は定義済みグループ名に完全一致させる(未知の名前は行エラー。自動作成はしない)。文字コードは UTF-8 / Shift_JIS を自動判定。エラー行は行番号付きで `details` に返す(1行でもエラーがあれば全行取り込まない)。行数上限500・ファイル1MB。
7. **共有トークンによる閲覧・結果入力は `/api/v1/shared/{token}` 系エンドポイントに集約する**(Phase 5で実装済み)。
   共有ページは1リクエストで全データ(大会・ラウンド・順位)を取得でき、`/api/v1/tournaments/**` は運営者認証のみに保てる。
   無効・不明・非公開(PRIVATE)トークンはすべて 403 `INVALID_SHARE_TOKEN` で統一し、大会の存在を漏らさない。

---

## 5. DTOの型定義

リクエスト/レスポンスDTOの型は `07_type_definitions.md` で一元管理する。
バックエンド(Java record)とフロントエンド(TypeScript interface)の対応を必ず同期させること。
