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
| frontend | Vitest + Testing Library | 主要コンポーネント・hooks | 表示ロジックと状態管理 |
| E2E | Playwright | クリティカルパスのみ | `12_e2e_test_design.md` 参照 |

domain層の90%はCI(`./gradlew check`)で機械的に強制する(`jacocoTestCoverageVerification`)。それ以外の層は目標値のみで、CIでは強制せずPRコメントで可視化する(`11_cicd_design.md` §2)。

---

## 2.5 テストケース設計技法

**カバレッジは「そのコードを実行したか」しか測らない。** アサーションが空でも90%は達成できる。
「何をテストケースにするか」を技法で決めることで、網羅の根拠を属人的な勘から切り離す。

この節は**実装をAIに委譲するときの基準**でもある。ケースの洗い出しを指示する際、
「同値分割と境界値分析で洗い出して」と技法名で伝えられる状態を目指す。

### 適用する技法

| 技法 | 何をするか | 主な適用先 |
|---|---|---|
| 同値分割 | 入力を「同じ振る舞いをするグループ」に分け、各グループから1件選ぶ | バリデーション全般 |
| 境界値分析 | グループの境目の**両側**(on/off point)を突く | 数値・文字数の制約 |
| 2因子間網羅(ペアワイズ) | 全組み合わせが爆発する場合に、任意の2因子の組み合わせだけを網羅する | 大会の構成条件 |
| デシジョンテーブル | 複数条件の組み合わせと結果を表にして漏れを見つける | 順位決定基準 |
| 状態遷移テスト | 正当な遷移に加え、**不正な遷移が拒否されること**を確認する | 大会・ラウンドのライフサイクル |

**全数テストは行わない。** 2因子間網羅で妥協するのは、実際の欠陥の多くが1〜2因子の組み合わせで再現するため。

### このプロジェクトの境界値

新しいバリデーションを追加したらこの表に追記する。

| 対象 | 制約 | on/off point |
|---|---|---|
| 大会名 | 必須(空白のみ不可) | `""` / `" "` / 1文字 |
| チーム名・グループ名・メンバー氏名 | 50文字以内(`NAME_MAX_LENGTH`) | 0 / 1 / 50 / 51 |
| ラウンド数(`totalRounds`) | 1以上(APIは8以下) | 0 / 1 / 8 / 9 |
| 現在ラウンド(`currentRound`) | 0〜`totalRounds` | -1 / 0 / `totalRounds` / `totalRounds`+1 |
| チーム制(`teamSize`) | 団体戦は3または5、個人戦は指定不可 | 2 / 3 / 4 / 5 / 6 / `null`(**飛び値なので4も必須**) |
| エントリー順(`entryOrder`)・ボード位置(`boardPosition`)・ラウンド番号 | 1以上 | 0 / 1 |
| グループあたりのACTIVE参加者数 | 大会開始には2名以上(団体戦は2チーム以上) | 1 / 2 |
| 参加者数の偶奇 | BYEの発生条件 | 奇数 / 偶数 |
| 棋力(`Rank` の `sortOrder`) | 段が負・級が正(小さいほど強い) | 初段 / 1級 の隣接ペア |

> **参加者数の上限300名は境界値ではない。** これは機能上の制約ではなく、
> ファーストリリース時点でインフラ側が想定するキャパシティであり、バリデーションで弾く性質のものではない。
> 規模の観点は負荷テスト(`perf/`)で扱う。

### ペアワイズ網羅の因子

大会の構成条件は組み合わせが爆発するため、2因子間網羅で妥協する:

| 因子 | 水準 |
|---|---|
| 競技種別 | 個人戦 / 団体戦(3人制) / 団体戦(5人制) |
| グループ分け | 単一グループ / 複数グループ |
| 参加者数の偶奇 | 偶数(BYEなし) / 奇数(BYEあり) |
| ラウンド位置 | 初回 / 中間 / 最終 |
| 参加者状態 | 全員ACTIVE / WITHDRAWNを含む |
| 結果入力元 | 運営者(OWNER) / 共有トークン(SHARE_TOKEN) |

**どの因子を選んだかをテストに明記する**(`@DisplayName` またはコメント)。
選定の根拠が残っていないと、後から網羅の妥当性を検証できない。

### デシジョンテーブルの対象

順位決定基準の適用順序(`05_swiss_pairing_algorithm.md` §3.1)のように、
**複数条件を順に適用して結果が決まる**ロジックは表にしてケースを起こす。
特に「上位基準が同値で下位基準に落ちる」経路は、各段で1件ずつ必要。

### 状態遷移テストの対象

| 状態 | 遷移 |
|---|---|
| `TournamentStatus` | PREPARING → IN_PROGRESS → FINISHED |
| `RoundStatus` | PAIRING → PLAYING → CONFIRMED |
| 対局結果の確定 | 両者の自己申告の突き合わせ(一致 / 不一致 / 片方のみ) |
| `ParticipantStatus` | ACTIVE → WITHDRAWN |

**不正な遷移を試すケースを必ず含める**(例: 準備中でない大会を開始する、最終ラウンドを超えて進める)。
`Tournament` は不正遷移を `DomainException` で弾いており、これはテストで固定すべき仕様である。

### 層別の技法対応

| 層 | 主に使う技法 |
|---|---|
| domain(マッチング・順位) | 境界値 + ペアワイズ + デシジョンテーブル + プロパティテスト(§3) |
| domain(モデル・バリデーション) | 同値分割 + 境界値 |
| application | 状態遷移 + 異常系(例外変換) |
| presentation / contract | 同値分割(正常/異常) + 認可の網羅 |
| frontend | 4状態(通常 / 空 / ローディング / エラー)← 同値分割の一種(§4) |
| E2E | **技法を適用せずクリティカルパスのみ**(意図的に絞る。`12_e2e_test_design.md`) |

### ツールは導入しない

因子が3〜5個ならペアワイズの組み合わせ表は手で作れる。PICT等の外部ツールや生成ライブラリは
導入せず、既存の jqwik(プロパティテスト)と `@ParameterizedTest` で表現する。
必要性が実証された時点で再検討する。

---

## 2.6 Mutation Testing(PITest)

**カバレッジ(LINE/BRANCH)は「そのコードを実行したか」しか測らない。** アサーションが空でも
高い値を達成できる。PITestは「コードを壊したらテストが気づくか」を測り、アサーションの薄い
箇所を名指しで炙り出す。domain層(マッチング・順位計算・モデル)に対象を絞る。

- **実行**: `./gradlew pitest`(ローカル)/ `.github/workflows/mutation.yml`(`workflow_dispatch` + 週次)。
  **`check` には紐付けない**(PR CIを遅くしないため。e2e.yml/vrt.ymlと同じ「重いものは別ワークフロー」方針)
- **対象**: `targetClasses = ['com.swiss_stage.domain.*']`
- **`targetTests` は明示指定が必須**: 既定値は `targetClasses` と同じパターンになるが、
  domainのテストは `com.swiss_stage.unit.domain.*` パッケージにあり `com.swiss_stage.domain.*`
  直下ではないため、指定しないと対象テストが0件と判定され `NO_COVERAGE`(0%)になる(実際に踏んだ)
- **jqwikのプロパティテストはJUnit Platform経由**のため `pitest-junit5-plugin` が必須
- **`mutationThreshold` は設定しない**(2026-07-30時点の実測: Mutation Coverage 86% / Test Strength 88%、
  397 mutations・実行59秒。閾値はこの先の実績を見てから固定する)
- HTMLレポートはArtifactsに30日保存。job summaryにスコアのサマリを出す

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

### contract/ はコンポーネント単位の受け入れテスト

`contract/` はAPIを外から叩いてレスポンス・DB状態・認可を検証する「コンポーネント単位の受け入れテスト」であり、受け入れケース台帳(`.claude/05_acceptance/01_acceptance_scope.md`)と対応する。

- `@DisplayName` の先頭に受け入れケースID(例: `TRN-AC-003: ...`)を付ける(複数はカンマ区切り)
- ID体系・運用は `.claude/05_acceptance/00_acceptance_policy.md` を参照

### domain層テストの必須項目

`05_swiss_pairing_algorithm.md` のテスト要件を全て実装する。特に:

- **プロパティベーステスト(jqwik)**: ランダムな人数(16〜300)・ラウンド数で
  「再戦なし」「BYE重複なし」「全員が毎ラウンド1回だけ登場」を機械的に検証する
- **characterization test**(ゴールデンテストの代替): 実大会(GAS版)の実データが手元にないため、
  固定シードの参加者・決定論的な勝敗パターンを使い、現行実装の出力(ペアリング結果・順位表)を
  `unit/domain/CharacterizationTest.java` で固定値と比較する。**仕様の正しさ自体は保証しない**が、
  回帰は確実に検知できる(domain実装を1行壊すと実際に失敗することを確認済み)。実データ入手後に
  ゴールデンテストへ差し替える。固定値の更新は `-Dcharacterization.update=true` 経由のみ許可し、
  ファイルが無い場合の自動生成はしない(差分は必ず人間がレビューする)

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
├── unit/            # Vitest + Testing Library
├── e2e/             # Playwright(バックエンド・DynamoDB Local前提)
└── vrt/             # Playwright(Visual Regression Test。Storybookページに対して実行、バックエンド不要)
```

### 方針

- **テスト対象の優先順位**: services(APIクライアント) > hooks > 複雑な表示ロジックを持つコンポーネント
- ユーザー視点でテストする: `getByRole` / `getByLabelText` を優先(`data-testid` は最終手段)
- APIは MSW(Mock Service Worker)でモックする(fetchの手モック禁止)
- **スナップショットテスト(DOM)は原則使わない(壊れやすく意味が薄い)。**
  これはVitestのコンポーネントテストの話であり、Storybook(下記)やVisual Regression Test(画像比較)とは別物。
  画像によるデグレ検知は「壊れやすいから禁止」ではなく、対象をページレベルに絞ることで運用する

### Storybook(ページレベル)

新規画面・大きなレイアウト変更では、`src/pages/XxxPage.stories.tsx` を **Plan PRの一部として作成**し、
実機を起動せずにUIの4状態(通常/空/ローディング/エラー)を確認・合意する(Plan PRのレビュー・
マージをもって合意とする。`10_frontend_design.md` §7、`04_development_process.md` §5.1)。
`components/ui/` 単体のカタログ化はしない。詳細は同ドキュメントを参照。

Plan PRブランチに `.stories.tsx` が追加されると、既存の `ci.yml`(frontendジョブ: lint/type-check/
build)と `vrt.yml` がpathsフィルタ・イベント条件の変更なしに自動実行される。新規ストーリーは
対応するベースライン画像が存在しないため、下記VRTの初回実行では失敗するが、`vrt.yml` は
非ブロッキング運用のため実害はない。新規ストーリーのスクリーンショットは `vrt.yml` のartifactから
確認できる(下記VRT節参照)。

### Visual Regression Test(VRT)

Storybookのページストーリーをスクリーンショット比較し、UI変更による意図しないデグレを検知する。
**UI関連のファイルを変更したPRで自動実行する**(pathsフィルタで絞る)。ただし
`maxDiffPixelRatio: 0`(ピクセル完全一致)と厳しいため、当面は**非ブロッキング**で運用し、
安定実績を見てから required check への昇格を検討する(`11_cicd_design.md` §2.8)。

- 対象: `frontend/tests/vrt/stories.spec.ts` が `storybook-static/index.json` から
  Storybookの全ページストーリーを自動列挙する(ストーリーを追加すれば対象も増える)
- 実行環境: `frontend/scripts/vrt.sh`(ローカル)/ `.github/workflows/vrt.yml`(`pull_request` / `workflow_dispatch`)。
  **必ず同じPlaywright公式コンテナイメージ(`mcr.microsoft.com/playwright:v1.61.1-noble`)から実行する。**
  ローカルのネイティブ環境でベースラインを生成しない(OSごとにフォントレンダリングが異なり
  100%差分になるため)
- 決定論の制約(**これを崩すと運用が破綻する**): アニメーション・トランジション停止、
  外部フォント(Google Fonts)への依存禁止(`@fontsource/noto-sans-jp` を使用)、
  ストーリー内で `new Date()` 等の非決定的な値を使わない
- ベースライン更新: `./scripts/vrt.sh --update`(ローカル確認用)または
  `gh workflow run vrt.yml -f update_snapshots=true --ref <branch>`(CI経由。**ベースライン更新は必ずCI経由**とし、
  更新は意図したUI変更のときのみ・1PRにまとめる)。**PRトリガーではベースラインを更新しない**
  (更新されると差分を検知せず素通りするため)。差分を検出したPRには更新コマンドが自動でコメントされる
- **新規ストーリーのスクリーンショット共有**: ベースライン画像がまだ存在しない新規ストーリー
  (Plan PRで作成された場合を含む)は、`vrt` ジョブがスクリーンショットを `actions/upload-artifact@v4`
  でartifact化し、`notify` ジョブがPRコメントでダウンロード案内を追記する(既存ストーリーの差分検知
  失敗時のコメントとは別に案内する)。レビュアーはローカルで `pnpm run storybook` を起動しなくても
  PR上でUIを確認できる。この仕組みはレビューの利便性向上が目的で、マージ可否には影響しない
  (`04_development_process.md` §5.1)

---

## 5. テストデータ

- `fixtures/` にテストデータビルダーを置く(例: `TournamentFixture.of(16人, 5回戦)`)
- 個人名は架空の名前のみ使用(実在の参加者データをテストに含めない)

---

## 6. 実行タイミング

| タイミング | 実行するもの |
|-----------|-------------|
| コード保存時(任意) | 対象ファイルの単体テスト |
| コミット前(必須) | frontend: `pnpm run check` / backend: `./gradlew check` |
| PR時(CI) | 全単体+統合テスト+ビルド、**E2E**、**VRT**(UI変更時)(`11_cicd_design.md`) |
| リリース前 | 全テスト + 実機での動作確認(`.claude/skills/verify`) |
