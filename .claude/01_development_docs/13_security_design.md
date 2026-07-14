# セキュリティ設計書

## 1. 基本方針

- 個人情報(氏名・所属企業)を扱うため、**個人情報保護をプロダクト品質の一部**として扱う
- 保持する個人情報は必要最小限(氏名・所属・段級位のみ。メールアドレスは運営者のGoogleアカウントのみ)
- 「見せる範囲」は常に運営者がコントロールできる

---

## 2. 認証

### 運営者: Google OAuth2 + JWT

- Spring Security `oauth2-client` で Google OAuth2 認証
- 認証成功後、自前のJWT(HS256)を発行し **HttpOnly + Secure + SameSite=Lax の Cookie** に格納
  - localStorage には保存しない(XSS耐性)
- JWTクレーム: `sub`(Google sub), `name`, `exp`(24時間)
- `JWT_SECRET` は32文字以上、環境変数で注入

### 参加者: 共有トークン

- 大会ごとに1つの共有トークン(`ShareToken`)を発行。URLに含める(`/s/{token}`)
- トークンは `SecureRandom` で32文字以上(URL-safe Base64)。**推測可能な連番・短いトークン禁止**
- 運営者はいつでも再発行(旧トークン即時無効化)できる
- トークンでできる操作は「閲覧」+「結果入力(大会設定 `resultInputEnabled` で許可時のみ)」に限定
- トークン経由のAPIは `/api/v1/shared/{token}` 系のみ。無効・不明・非公開(PRIVATE)はすべて同一の403 `INVALID_SHARE_TOKEN` で返す(存在を漏らさない)

---

## 3. 認可

| 操作 | 運営者(本人) | 運営者(他人) | 共有トークン | 未認証 |
|------|:---:|:---:|:---:|:---:|
| 大会の作成 | ○ | - | × | × |
| 大会の閲覧 | ○ | × | ○ | ×(PUBLIC時のみ○) |
| 大会の編集・削除・進行 | ○ | × | × | × |
| 結果入力 | ○ | × | △(設定による) | × |

- **すべてのAPIで「大会のownerSubとJWTのsubの一致」を検証する**(application層で必ずチェック。presentation層のチェックだけに頼らない)
- 認可失敗はエラーメッセージで存在の有無を漏らさない(他人の大会IDへのアクセスは404を返す)

---

## 4. 入力検証

- Bean Validation(JSR 380)を全DTOに適用: 文字数上限(大会名100文字、氏名50文字等)、必須、形式
- CSVインポート: 行数上限(500行)、ファイルサイズ上限(1MB)、拡張子・Content-Type検証
- フロントでも同等のバリデーションを行うが、**信頼するのはバックエンドの検証のみ**

---

## 5. Webセキュリティ対策

| 脅威 | 対策 |
|------|------|
| XSS | Reactのデフォルトエスケープに依存。`dangerouslySetInnerHTML` 禁止。CSPヘッダー設定 |
| CSRF | 認証CookieがSameSite=Lax + 更新系はJSON API(CORSで防御)。Spring SecurityのCSRF設定はAPI構成に合わせ調整 |
| CORS | 本番は自ドメインのみ許可。`*` 禁止 |
| インジェクション | DynamoDBのため SQLインジェクションなし。ただしキー組み立て時のユーザー入力混入を禁止(IDはULID検証必須) |
| レート制限 | 結果入力・トークンアクセスにIPベースの簡易レート制限(bucket4j)。ブルートフォースによるトークン探索対策 |
| 依存脆弱性 | Dependabot自動更新 + `npm audit` / `gradlew dependencyCheckAnalyze` を月次実行 |
| セキュリティヘッダー | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin` |

---

## 6. 個人情報保護の実装ルール

1. **ログに個人名・所属を出力しない**(IDのみ。`06_error_handling_design.md` 参照)
2. **大会削除は物理削除**: 主催者が削除したら参加者情報も含め完全に消す
3. 公開範囲 `PRIVATE`(運営者のみ)/ `TOKEN`(URL知っている人)/ `PUBLIC`(誰でも)をAPIレベルで強制
4. 共有トークン経由のレスポンスに `shareToken`・`ownerSub` を含めない(DTO出し分け)
5. 利用規約・プライバシーポリシーページを公開前に用意する(保持情報・目的・削除方法を明記)

---

## 7. インフラセキュリティ

- ALBでSSL/TLS終端(ACM証明書)。HTTP→HTTPSリダイレクト必須
- EC2セキュリティグループ: ALBからの8080のみ許可。SSHは自宅IPのみ(将来はSSM Session Manager化)
- IAM: EC2ロールはDynamoDBの対象テーブルのみ許可(最小権限)。アクセスキーをコードに書かない
- 秘密情報: 環境変数ファイル(600)で管理、リポジトリにコミットしない。`.gitignore` に `.env*` を必ず含める
