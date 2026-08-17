# CLAUDE.md

このファイルは、Claude Code がこのリポジトリで作業する際のガイダンスを提供します。

## プロジェクト概要

**Swiss Stage** は囲碁・将棋大会向けの大会運営プラットフォームです。
コンセプトは「大会といえば Swiss Stage」— スイス方式マッチング・結果集計・順位表示を自動化し、紙と手作業による大会運営を置き換えます。

- 対象規模: 16〜300名の個人戦(MVP)
- 利用者: 運営者(PC・Googleログイン) / 参加者(スマホ・共有URL、ログイン不要)
- **最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**(マッチング・順位計算は絶対に間違えられない)

## 技術スタック

| 領域 | 技術 |
|------|------|
| データベース | DynamoDB(シングルテーブル設計、AWS SDK v2 Enhanced Client) |
| インフラ | AWS(EC2 t3.micro + ALB + Route53 + CloudWatch)、予算 ~$17/月 |
| 認証 | Google OAuth2 + JWT Cookie(運営者)/ 共有トークン(参加者) |

## アーキテクチャ

モノレポ構成: `frontend/`(SPA) + `backend/`(REST API)。バックエンドはDDDレイヤードアーキテクチャ厳守:

```
backend/src/main/java/com/swiss_stage/
├── presentation/   # controller, filter(HTTPのみ、ロジック禁止)
├── application/    # service(ユースケース), dto
├── domain/         # model, repository(IF), service ← Spring/AWS依存禁止・テスト最重点
└── infrastructure/ # repository(DynamoDB実装), config
```

**ロジックの置き場所**: 大会・対局・順位のルール → domain / 操作の流れ → application / 見せ方 → presentation / 保存 → infrastructure

```
frontend/src/
├── components/{ui,features,layouts}/
├── pages/          # ルーティング単位(ロジックなし)
├── services/       # API通信(fetch直呼び禁止)
├── hooks/          # TanStack Query接続
├── types/          # 型定義(バックエンドDTOと同期)
├── utils/
└── theme/          # MUIテーマ(デザイントークン集約)
```

## 開発コマンド

```bash
# フロントエンド(frontend/)
pnpm run dev            # 開発サーバー(:5173、/api→:8080プロキシ)
pnpm run check          # lint + format + type-check + test(コミット前必須)
pnpm run test:e2e       # Playwright(backend起動が前提)

# バックエンド(backend/)
docker compose up -d dynamodb-local   # DynamoDB Local(:8000)
./scripts/create-table.sh             # ローカルテーブル作成
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew check        # test + 静的解析(コミット前必須)
```

## API・データ規約(要点)

- **API契約(エンドポイント・DTO・enum)は `schema/openapi.yaml` が唯一の正**。変更はスキーマが先(`07_type_definitions.md`)
- ID は ULID 文字列。更新系は `version` による楽観ロック(競合は409)
- 順位(Standing)は保存せず都度計算する

## テスト方針(要点)

- domain層(マッチング・順位計算)はTDD、カバレッジ90%以上 + jqwikプロパティテスト
- リポジトリ実装のテストは DynamoDB Local(モック禁止)。テストごとに一意ULIDで分離
- フロントは Vitest + Testing Library + MSW。`getByRole` 優先
- E2Eはクリティカルパスのみ(`12_e2e_test_design.md`)

## 避けるべき落とし穴

1. **マッチングの絶対制約を破らない**: 再戦禁止・BYE重複禁止(`05_swiss_pairing_algorithm.md`)
2. **フロントで順位計算・マッチングをしない**(表示専用。計算はバックエンドのみ)
3. domain層に Spring/AWS SDK を import しない
4. `spring-data-dynamodb` を追加しない(boostchicken製、Spring Boot 3以降のいずれのバージョンにも対応していない)
5. DynamoDB Local は `-sharedDb` 必須(ないと認証情報ごとにDBが分かれて「テーブルがない」事故)
6. `@DynamoDbBean` はgetterにアノテーション。楽観ロックはEnhanced Client経由のみ有効
7. MUI: `textTransform: 'none'` をテーマ設定、アイコンは個別import、色・余白のハードコード禁止
8. ログ・レスポンスに個人情報(氏名・所属)やshareTokenを漏らさない
9. `window.alert/confirm`・`window.location.href` 禁止(ConfirmDialog / React Router を使う)。
   `window.print()` のみ例外(代替APIがないため許容。`utils/printPage.ts` 経由でのみ呼び、
   ユーザー操作起点に限る。`useEffect` 等での自動実行は禁止)
10. 認証状態のロード完了前にリダイレクトしない(`RequireAuth` の isLoading 待ち)
11. 大会前日・当日はデプロイしない
12. コミット前に `pnpm run check` / `./gradlew check` を必ず実行
13. 順序・優先度に意味のあるenumはordinal(宣言順)に依存しない。明示的な数値フィールド(`sortOrder` 等)で比較し、宣言順との整合をテストで検証する(例: `Rank`)
14. `package.json` の依存を変更したら `pnpm install` で `pnpm-lock.yaml` を再生成しコミットに含める(CIの `pnpm/action-setup` は `packageManager` フィールドからバージョンを読むため、ローカルと同じpnpmバージョンで解決される)
15. コントローラーの `@PathVariable`/`@RequestParam` 等は名前を必ず明示する(省略すると `-parameters` フラグ依存になり、VSCode(Eclipse JDT)ビルドで起動したときだけ実行時エラー。ArchUnitで強制済み)
16. `spring-boot-starter-test` 4系がバンドルする `junit-platform-launcher` のバージョンが `junit-jupiter` と噛み合わないことがある(実測でNoSuchMethodError)。`backend/build.gradle` の `resolutionStrategy.eachDependency` で明示的に揃えている設定を外さない(決定の経緯は `06_adr/05_junit_platform_launcher_pin.md`)
17. 外部のskillを `.claude/skills/` に入れる前に SKILL.md を通読し、前提スタックを確認する。**「どのルールを無視すべきか」を毎回確認しないと使えないものは採用しない**(逸脱が沈黙のうちに規約違反を生むため)。例: `baseline-ui` は Tailwind + Base UI/Radix + `motion/react` 前提で、`02_design_system/03_animation_system.md` のアニメーションライブラリ追加禁止と衝突するため不採用(`06_adr/06_design_system_direction.md`)
18. Plan PRは原則コード0行だが、次の2つを例外として許可する: (1) 新規画面・大きなレイアウト変更を含む場合は対象画面の `src/pages/XxxPage.stories.tsx`。新規画面(0→1)・既存画面の大きなレイアウト変更のいずれも、本物のページが対象の新レイアウトをまだ反映していない間はストーリー内にインラインのプレースホルダー実装を書いてよいが、実装PRで本物のページをこれに合わせて書き換え実importへ書き換えること(`04_development_process.md` §5.1.1、`06_adr/11_story_first_agreement.md`・`06_adr/12_story_first_existing_page_placeholder.md`)。(2) `schema/openapi.yaml` を変更する場合は `pnpm run generate:api` の出力(`frontend/src/types/generated/api.d.ts`)。CIの生成型鮮度チェックがブランチ種別を問わず必須ゲートのため、スキーマ変更と同じPRで追随させる必要がある(`06_adr/14_plan_pr_generated_types_exception.md`)

## プロジェクトドキュメントガイド

`.claude/` 配下は番号+内容が分かるファイル名(例: `05_swiss_pairing_algorithm.md`)。詳細は該当ディレクトリを直接見る。迷ったときの入口だけ挙げる:

- `00_project/` — 要件・開発プロセス。**03_feature_plan_template.md**(Planモードで従う必須形式)/ **04_development_process.md**(Issue→Plan PR→実装PRの正典)
- `01_development_docs/` — 技術設計。**05_swiss_pairing_algorithm.md**(マッチング・順位計算の仕様。心臓部)
- `02_design_system/` — デザインシステム。UI実装前に **00_basic_design.md** から
- `03_library_docs/` — MUI・DynamoDB Enhanced Client・React Routerの落とし穴対策
- `04_quality/` — AIレビュー観点(`01_review_checklist.md`)・Critical/Major/Minor基準(`02_severity.md`)
- `05_acceptance/` — 受け入れケース台帳(`01_acceptance_scope.md`)。contractテスト・PlaywrightとIDで紐づく
- `06_adr/` — ADR(決定記録)。書き換えず `Superseded by` で積む
- `07_plans/` — Plan PRの実装計画の実体。実装完了で `Status: done`

### AIエージェント連携
- AIレビュー(Reviewer→Fixer)は `.claude/agents/reviewer.md` / `fixer.md`、CI連携は `.github/workflows/ai-review.yml`
- QAは `.claude/agents/qa.md`(受け入れケース台帳と差分を突合)、CI連携は `.github/workflows/ai-qa.yml`。指摘の自動修正は `.claude/agents/qa-fixer.md`
- ci-fixerは `.claude/agents/ci-fixer.md`、CI連携は `.github/workflows/ci.yml` の `autofix` ジョブ
- design-reviewerは `.claude/agents/design-reviewer.md`、CI連携は `.github/workflows/ai-design-review.yml`
- 受け入れケース台帳のID整合・ファイル参照切れの機械検査は `.github/scripts/docs-lint.py`(CIの`frontend`ジョブ)

## ドキュメント運用ルール

- 開発プロセス全体(要件→Issue→Plan PR→実装PR)は `.claude/00_project/04_development_process.md` に従う。
  分類ごとに Plan PR・ADR・受け入れケースが必要かは同ファイル §2 のトリガー表で判断する。
- 機能追加・挙動変更のプランは `.claude/00_project/03_feature_plan_template.md` の形式に従う。
  特に **UI仕様(4状態・レスポンシブ・大量データ時)を実装前に文章で確定させる**(実機で見てからの手戻りを防ぐため)
- **プラン承認後、プランの「6. 更新する資料」§「Plan PRで更新するもの」に列挙したファイルの更新には
  追加の承認を求めない**(承認済みの範囲として扱う)。`.claude/01_development_docs/**` 等の設計
  ドキュメント本体はPlan PR承認の対象外で、実装PRで別途更新・レビューする(`04_development_process.md` §2)
- 実装と設計ドキュメントが乖離したら、**同じPRでドキュメントを更新する**
  - 受け入れケース台帳(`.claude/05_acceptance/01_acceptance_scope.md`)もこのルールの対象: 新機能・挙動変更は実装前にケースを追加(Status=todo)し、実装PRでdoneに更新する
- 仕様変更(特にマッチング・順位計算)は先に `05_swiss_pairing_algorithm.md` を更新してから実装する
- 新しいエラーコード・デザイントークン・UIパターンは対応ドキュメントに追記してから使う
- AIレビューの誤検知・見逃しに気づいたら `04_quality/01_review_checklist.md` を更新して育てる。機械検査可能な規約はチェックリストではなく lint / ArchUnit に追加する
