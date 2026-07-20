# レビューチェックリスト

AIレビュアー(`.claude/agents/reviewer.md`)がPRレビュー時に参照する品質基準。
**機械で検査できない項目だけ**を載せる。指摘の際は項目ID(例: `CORR-1`)を根拠として引用する。

## 運用ルール

- 誤検知(不当な指摘)や見逃し(人間が後から発見したバグ)が出たら、このファイルを同じ要領でPR更新して育てる
- 機械検査可能な項目はここに追加せず、lint / ArchUnit / テストに落とす

## 機械検査済みの項目(レビューで指摘しない)

以下はCIが検出するため、AIレビューでは**指摘禁止**:

| 項目 | 検査手段 |
|------|---------|
| domain層のSpring/AWS SDK依存、レイヤー間の不正依存 | ArchUnit(`ArchitectureTest.java`) |
| `alert`/`confirm`/`prompt` の使用 | oxlint `no-alert` |
| `services/` 外での `fetch` 直呼び | oxlint `no-restricted-globals` |
| フォーマット・型エラー・テスト失敗 | `npm run check` / `./gradlew check` |
| API契約(DTO/enum)とスキーマの一致 | contractテストのOpenAPI検証(`schema/openapi.yaml`)+ 生成型の鮮度チェック(CI) |

## QAエージェント管轄(レビューで指摘しない)

受け入れケース台帳(`.claude/05_acceptance/`)との整合 — 台帳の更新漏れ・ケースIDとテストの対応・受け入れテストの基準hack — はQAエージェント(`.claude/agents/qa.md`、CIは `ai-qa.yml`)が検証する。Reviewerは**テストとコード・設計ドキュメントの関係**(TEST-1〜4)を見る。

---

## CORR: 正確性(マッチング・順位計算)— 最重要

- **CORR-1**: 再戦禁止・BYE重複禁止の絶対制約を弱めるロジック変更がないか(緩和は「解が存在しない場合のみ+UI警告」に限る。`05_swiss_pairing_algorithm.md` §2.2)
- **CORR-2**: 順位決定基準の適用順序(同 §3.1)を変えていないか
- **CORR-3**: SOS等の計算ルールの変更が、先に `05_swiss_pairing_algorithm.md` の更新を伴っているか
- **CORR-4**: 浮動小数点の比較・丸めで順位が不安定にならないか(同点判定は誤差を持ち込まない実装か)
- **CORR-5**: 途中棄権(WITHDRAWN)参加者の扱いが仕様(同 §2.3)どおりか
- **CORR-6**: フロントエンドで順位計算・マッチング相当の計算をしていないか(フロントは表示専用)
- **CORR-7**: 順位(Standing)を保存していないか(都度計算の原則)

## API: 仕様・API整合

- **API-1**: レスポンスが統一フォーマット(`success`/`data`/`error`)か。エラーコードが `06_error_handling_design.md` の表に存在するか(新規コードは表への追記が先)
- **API-2**: 更新系エンドポイントに `version` による楽観ロックがあるか。競合時に409を返すか
- **API-3**: DTO・エンドポイント変更に `schema/openapi.yaml` の更新が伴っているか(スキーマとの一致自体は機械検査されるが、「スキーマを先に直したか・意味論のdescriptionが更新されているか」はレビューで見る)
- **API-4**: ID=ULID文字列、日時=ISO8601文字列、JSON camelCase の規約を守っているか
- **API-5**: 順序・優先度に意味のあるenumが `ordinal()`(宣言順)に依存していないか。明示的な数値フィールドで比較し、整合テストがあるか

## SEC: セキュリティ

- **SEC-1**: ログ・エラーレスポンス・例外メッセージに個人情報(氏名・所属)や `shareToken` が漏れていないか
- **SEC-2**: 運営者向けAPIが対象大会の所有者確認をしているか。共有トークンでのアクセスが読み取り専用に制限されているか(`13_security_design.md`)
- **SEC-3**: 外部入力のバリデーションが適切な層(presentation=形式、domain=業務ルール)にあるか

## DB: DynamoDB / infrastructure

- **DB-1**: キー設計が規約(`PK=TOURNAMENT#{id}`, `SK=METADATA|PARTICIPANT#…|ROUND#nn#MATCH#…`)どおりか。新しいアクセスパターンが `02_database_design.md` に文書化されているか
- **DB-2**: 楽観ロックがEnhanced Client経由で機能する実装か(低レベルAPIや手書き条件式で迂回していないか)
- **DB-3**: `@DynamoDbBean` のアノテーションがgetter側に付いているか

## FE: フロントエンド

- **FE-1**: `window.location.href` への代入で画面遷移していないか(React Routerを使う)
- **FE-2**: 認証状態のロード完了前にリダイレクトしていないか(`RequireAuth` の `isLoading` 待ち)
- **FE-3**: 色・余白・フォントサイズのハードコードがないか(テーマのデザイントークンを使う)
- **FE-4**: 更新系のmutation後に関連クエリの無効化(`invalidateQueries`)があるか(画面が古いデータのままにならないか)

## TEST: テスト

- **TEST-1**: domain層の変更に対応するユニットテストが追加・更新されているか(`05_swiss_pairing_algorithm.md` §4 の必須ケースを壊していないか)
- **TEST-2**: マッチング・順位計算のロジック変更に、jqwikプロパティテストの観点(不変条件)が反映されているか
- **TEST-3**: リポジトリ実装のテストがDynamoDB Local実機で行われているか(モックにすり替わっていないか)
- **TEST-4**: テストが実装詳細ではなく仕様(振る舞い)を検証しているか。テストの削除・スキップ・アサーション弱体化で「通した」形跡がないか

## DOC: ドキュメント同期

- **DOC-1**: 実装と設計ドキュメントが乖離する変更に、同じPRでのドキュメント更新が含まれているか
- **DOC-2**: 新しいエラーコード・デザイントークン・UIパターンが対応ドキュメントに追記されているか
