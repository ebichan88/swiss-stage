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
| フロントエンド | React 19 + TypeScript + Vite + Material-UI + React Router v7 + TanStack Query |
| バックエンド | Java 21 + Spring Boot 3.x + Gradle 8 |
| データベース | DynamoDB(シングルテーブル設計、AWS SDK v2 Enhanced Client) |
| インフラ | AWS(EC2 t3.micro + ALB + Route53 + CloudWatch)、予算 ~$17/月 |
| 認証 | Google OAuth2 + JWT Cookie(運営者)/ 共有トークン(参加者) |

⚠️ `spring-data-dynamodb`(boostchicken)は Spring Boot 3 非対応のため**使用しない**。
詳細: `.claude/03_library_docs/02_dynamodb_enhanced_client.md`

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
npm run dev            # 開発サーバー(:5173、/api→:8080プロキシ)
npm run check          # lint + format + type-check + test(コミット前必須)
npm run test:e2e       # Playwright(backend起動が前提)

# バックエンド(backend/)
docker compose up -d dynamodb-local   # DynamoDB Local(:8000)
./scripts/create-table.sh             # ローカルテーブル作成
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew check        # test + 静的解析(コミット前必須)
```

## API・データ規約(要点)

- ベースパス `/api/v1`、レスポンスは統一フォーマット(`success`/`data`/`error`)。エラーコードは `06_error_handling_design.md` の表に従う
- ID は ULID 文字列。日時は ISO8601 文字列。JSONは camelCase
- 更新系は `version` による楽観ロック(競合は409)
- DynamoDBキー: `PK=TOURNAMENT#{id}`, `SK=METADATA|PARTICIPANT#…|ROUND#nn#MATCH#…`
- 順位(Standing)は保存せず都度計算(domain/service/StandingCalculator)

## テスト方針(要点)

- domain層(マッチング・順位計算)はTDD、カバレッジ90%以上 + jqwikプロパティテスト
- リポジトリ実装のテストは DynamoDB Local(モック禁止)。テストごとに一意ULIDで分離
- フロントは Vitest + Testing Library + MSW。`getByRole` 優先
- E2Eはクリティカルパスのみ(`12_e2e_test_design.md`)

## 避けるべき落とし穴

1. **マッチングの絶対制約を破らない**: 再戦禁止・BYE重複禁止(`05_swiss_pairing_algorithm.md`)
2. **フロントで順位計算・マッチングをしない**(表示専用。計算はバックエンドのみ)
3. domain層に Spring/AWS SDK を import しない
4. `spring-data-dynamodb` を追加しない(Spring Boot 3非対応)
5. DynamoDB Local は `-sharedDb` 必須(ないと認証情報ごとにDBが分かれて「テーブルがない」事故)
6. `@DynamoDbBean` はgetterにアノテーション。楽観ロックはEnhanced Client経由のみ有効
7. MUI: `textTransform: 'none'` をテーマ設定、アイコンは個別import、色・余白のハードコード禁止
8. ログ・レスポンスに個人情報(氏名・所属)やshareTokenを漏らさない
9. `window.alert/confirm`・`window.location.href` 禁止(ConfirmDialog / React Router を使う)
10. 認証状態のロード完了前にリダイレクトしない(`RequireAuth` の isLoading 待ち)
11. 大会前日・当日はデプロイしない
12. コミット前に `npm run check` / `./gradlew check` を必ず実行
13. 順序・優先度に意味のあるenumはordinal(宣言順)に依存しない。明示的な数値フィールド(`sortOrder` 等)で比較し、宣言順との整合をテストで検証する(例: `Rank`)
14. `package-lock.json` の更新はCIと同じ `npx -y npm@10 install --package-lock-only` で行う(ローカルのnpm 11はpeerDependenciesをlockに含めず、CIの `npm ci` が「Missing from lock file」で落ちる)
15. コントローラーの `@PathVariable`/`@RequestParam` 等は名前を必ず明示する(省略すると `-parameters` フラグ依存になり、VSCode(Eclipse JDT)ビルドで起動したときだけ実行時エラー。ArchUnitで強制済み)

## プロジェクトドキュメントガイド

### 📋 プロジェクト要件
- `.claude/00_project/01_appcadia_concept_requirements.md` — コンセプト・要求定義(最上位の判断基準)
- `.claude/00_project/02_inception_deck.md` — ビジョン・やらないこと・トレードオフ

### 🏗️ 技術設計(.claude/01_development_docs/)
- `01_architecture_design.md` — DDDレイヤー構造と責任、ロジック配置基準
- `02_database_design.md` — DynamoDBシングルテーブル設計、アクセスパターン
- `03_api_design.md` — エンドポイント一覧、レスポンス形式、ステータスコード
- `04_screen_transition_design.md` — 画面一覧・遷移図・UXルール
- `05_swiss_pairing_algorithm.md` — **スイス方式マッチング・順位計算の仕様(心臓部)**
- `06_error_handling_design.md` — エラーコード表、例外階層、表示方法
- `07_type_definitions.md` — DTO型定義(TS/Java同期)、enum一覧
- `08_development_setup.md` — 環境構築、コマンド、Git運用
- `09_test_strategy.md` — カバレッジ目標、層別テスト方針
- `10_frontend_design.md` — コンポーネント設計、状態管理(TanStack Query)
- `11_cicd_design.md` — GitHub Actions、デプロイ手順
- `12_e2e_test_design.md` — クリティカルパス定義、Playwrightルール
- `13_security_design.md` — 認証認可、共有トークン、個人情報保護
- `14_performance_optimization.md` — キャッシュ戦略、負荷特性、目標値
- `15_performance_monitoring.md` — CloudWatch監視、大会当日の運用手順

### 🎨 デザインシステム(.claude/02_design_system/)
- `00_basic_design.md` — 概要・クイックスタート(**UI実装前にまず読む**)
- `01_design_principles.md` — カラー・タイポグラフィ・スペーシングのトークン
- `02_component_design.md` — ボタン・表・フォーム等の使い分け
- `03_animation_system.md` — 動きのルール(控えめ・確実)
- `04_layout_system.md` — ブレークポイント・ページ構造

### 📚 ライブラリ対策(.claude/03_library_docs/)
- `01_mui_patterns.md` — MUIテーマ・sx・Dialog・RHF接続の標準形
- `02_dynamodb_enhanced_client.md` — Enhanced Client実装パターン・落とし穴
- `03_dynamodb_local_testing.md` — DynamoDB Localテスト・CI設定
- `04_react_router_patterns.md` — ルート定義・認証ガード・SPAフォールバック

### 🔍 品質基準・AIレビュー(.claude/04_quality/)
- `01_review_checklist.md` — AIレビューの観点(機械検査できない項目のみ。lint/ArchUnitで検査可能なものは載せない)
- `02_severity.md` — Critical/Major/Minorの定義とPASS/FAIL判定基準
- Reviewer本体は `.claude/agents/reviewer.md`、Fixer本体は `.claude/agents/fixer.md`、CI連携は `.github/workflows/ai-review.yml`(PRごとに自動レビュー。FAIL時はCritical/MajorのみFixerが自動修正 → 再レビュー。上限3回・domain/serviceは聖域・行き詰まったら `needs-human` ラベルで人間へ)

### クイックリファレンスマップ

| タスク | 参照ドキュメント |
|-------|----------------|
| 新機能の追加 | アーキテクチャ → DB設計 → API設計 → フロントエンド設計 |
| マッチング・順位計算の実装/修正 | **05_swiss_pairing_algorithm** → テスト戦略 |
| APIエンドポイント追加 | API設計 → エラーハンドリング → 型定義 |
| DynamoDBの操作追加 | DB設計 → Enhanced Client → DynamoDB Localテスト |
| 画面の新規作成 | 画面遷移設計 → フロントエンド設計 → デザインシステム(00から) |
| UIコンポーネント実装 | コンポーネント設計 → MUIパターン → デザイン原則 |
| フォーム実装 | コンポーネント設計(§4) → MUIパターン(§5) → エラーハンドリング |
| テスト作成 | テスト戦略 → (層に応じて)DynamoDB Localテスト |
| E2Eテスト | E2Eテスト設計 → 画面遷移設計 |
| 認証・認可・共有トークン | セキュリティ設計 → API設計 |
| エラー処理 | エラーハンドリング → 型定義 |
| パフォーマンス改善 | パフォーマンス最適化 → 監視 |
| デプロイ・CI | CI/CD設計 → 開発セットアップ |

## ドキュメント運用ルール

- 実装と設計ドキュメントが乖離したら、**同じPRでドキュメントを更新する**
- 仕様変更(特にマッチング・順位計算)は先に `05_swiss_pairing_algorithm.md` を更新してから実装する
- 新しいエラーコード・デザイントークン・UIパターンは対応ドキュメントに追記してから使う
- AIレビューの誤検知・見逃しに気づいたら `04_quality/01_review_checklist.md` を更新して育てる。機械検査可能な規約はチェックリストではなく lint / ArchUnit に追加する
