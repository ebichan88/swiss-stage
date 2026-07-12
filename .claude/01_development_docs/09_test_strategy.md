# テスト戦略

## 1. 基本方針

- **TDD推奨**: 特に domain 層(マッチング・順位計算)はテストファーストで実装する
- **ゼロ警告ポリシー**: テスト実行中のコンソール警告・エラーをゼロに保つ
- **テストピラミッド**: 単体テスト(多) > 統合テスト(中) > E2E(少・クリティカルパスのみ)
- 「正確性 > すべて」のプロダクト原則に基づき、**domain層のテストに最も投資する**

---

## 2. カバレッジ目標

| 層 | フレームワーク | 目標 | 理由 |
|----|--------------|------|------|
| backend domain | JUnit 5 (+ jqwik) | **90%以上** | マッチング・順位計算のバグは大会当日に致命傷 |
| backend application | Spring Boot Test + Mockito | 80%以上 | ユースケースの流れと例外変換 |
| backend infrastructure | DynamoDB Local統合テスト | 主要リポジトリ全メソッド | キー設計ミスの検出 |
| backend presentation | MockMvc | 70%以上 | ステータスコード・レスポンス形式・認可 |
| frontend | Jest + Testing Library | 主要コンポーネント・hooks | 表示ロジックと状態管理 |
| E2E | Playwright | クリティカルパスのみ | `12_e2e_test_design.md` 参照 |

---

## 3. バックエンドテスト構成

```
backend/src/test/java/com/swiss_stage/
├── unit/
│   ├── domain/          # 純粋な単体テスト(Spring起動なし・最速)
│   └── application/     # Mockitoでリポジトリをモック
├── integration/         # DynamoDB Local使用(@SpringBootTest)
└── contract/            # MockMvcによるAPIコントラクトテスト
```

### domain層テストの必須項目

`05_swiss_pairing_algorithm.md` のテスト要件を全て実装する。特に:

- **プロパティベーステスト(jqwik)**: ランダムな人数(16〜300)・ラウンド数で
  「再戦なし」「BYE重複なし」「全員が毎ラウンド1回だけ登場」を機械的に検証する
- **ゴールデンテスト**: 実大会(GAS版)の実データから期待順位表を固定し、回帰を検知する

### 統合テストのルール

- DynamoDB Local(Docker)を使用。モックでDynamoDBを再現しない(条件付き書き込み・GSIの挙動が再現できないため)
- テストごとにテーブルを作り直すのではなく、**テストごとに異なるTournamentIdを使って分離**する(高速化)
- 詳細なセットアップは `.claude/03_library_docs/03_dynamodb_local_testing.md` を参照

### presentation層テストのルール

- 認可テストを必ず含める: 「他人の大会を操作できないこと」「無効トークンで403」
- レスポンスが統一フォーマット(`success`/`data`/`error`)に従うことを検証

---

## 4. フロントエンドテスト構成

```
frontend/tests/
├── unit/            # Jest + Testing Library
└── e2e/             # Playwright
```

### 方針

- **テスト対象の優先順位**: services(APIクライアント) > hooks > 複雑な表示ロジックを持つコンポーネント
- ユーザー視点でテストする: `getByRole` / `getByLabelText` を優先(`data-testid` は最終手段)
- APIは MSW(Mock Service Worker)でモックする(fetchの手モック禁止)
- スナップショットテストは原則使わない(壊れやすく意味が薄い)

---

## 5. テストデータ

- `fixtures/` にテストデータビルダーを置く(例: `TournamentFixture.of(16人, 5回戦)`)
- 個人名は架空の名前のみ使用(実在の参加者データをテストに含めない)

---

## 6. 実行タイミング

| タイミング | 実行するもの |
|-----------|-------------|
| コード保存時(任意) | 対象ファイルの単体テスト |
| コミット前(必須) | frontend: `npm run check` / backend: `./gradlew check` |
| PR時(CI) | 全単体+統合テスト+ビルド(`11_cicd_design.md`) |
| リリース前 | E2E含む全テスト |
