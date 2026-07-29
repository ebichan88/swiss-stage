# CI/CD設計書

## 1. 基本方針

- CI: GitHub Actions(PR時に必ず実行、mainへの直接pushは禁止)
- CD: MVP期は手動デプロイ(スクリプト化)。安定後に AWS CodeDeploy 化を検討
- **「mainは常にデプロイ可能」**を守る

---

## 1.5 ワークフロー一覧

| ワークフロー | トリガー | 目的 |
|---|---|---|
| `ci.yml` | `pull_request` / `push`(main) | 単体・統合テスト、ビルド(PRごと必須。§2)+ 失敗時の決定論的な自動修正(§2.7) |
| `e2e.yml` | `workflow_dispatch` | クリティカルパスのE2E(Playwright)。リリース前・大会前に手動実行(`12_e2e_test_design.md`) |
| `vrt.yml` | `workflow_dispatch` | StorybookページのVisual Regression Test。大きめのUI変更をしたPRで手動実行(`09_test_strategy.md`) |
| `ai-review.yml` | `pull_request` | AIコードレビュー・自動修正(Critical/Majorのみ。§2.5) |
| `ai-qa.yml` | `pull_request` | 受け入れケース台帳との突合(レポートのみ・非ゲート。`.claude/agents/qa.md`) |
| `guard.yml` | `pull_request` | テスト弱体化ガード。AI自動修正の安全装置(§2.6) |

`ci.yml` / `guard.yml` 以外はPRごとに自動実行しない(重い、または人間の追加判断を要するため)。

### `vrt.yml` の運用

- `workflow_dispatch` の入力 `update_snapshots`(boolean)で「比較のみ」と「ベースライン更新」を切り替える
- **ベースライン更新は必ずこのワークフロー経由で行う**(ローカルではPRに含めるベースラインを生成しない。`09_test_strategy.md`)。更新時はコンテナ内でコミット・pushする(`permissions: contents: write`)
- 実行対象はローカル確認用の `frontend/scripts/vrt.sh` と同じPlaywright公式コンテナイメージを使う(フォントレンダリング差分を避けるため)

---

## 2. CIパイプライン(PR時)

`.github/workflows/ci.yml`

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main]

permissions:
  contents: read
  pull-requests: write # カバレッジレポートのPRコメント投稿用

jobs:
  frontend:
    runs-on: ubuntu-latest
    defaults: { run: { working-directory: frontend } }
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with: { package_json_file: frontend/package.json }
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: pnpm, cache-dependency-path: frontend/pnpm-lock.yaml }
      - run: pnpm install --frozen-lockfile
      - run: pnpm run lint
      - run: pnpm run type-check
      - run: pnpm run test:coverage
      - uses: davelosert/vitest-coverage-report-action@v2
        if: always() && github.event_name == 'pull_request'
        with: { working-directory: frontend }
      - run: pnpm run build

  backend:
    runs-on: ubuntu-latest
    defaults: { run: { working-directory: backend } }
    services:
      dynamodb-local:
        image: amazon/dynamodb-local:latest
        ports: ["8000:8000"]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21, cache: gradle }
      - run: ./scripts/create-table.sh
        env: { DYNAMODB_ENDPOINT: "http://localhost:8000" }
      - run: ./gradlew check build
      - uses: madrapps/jacoco-report@v1.8.0
        if: always() && github.event_name == 'pull_request'
        with:
          paths: ${{ github.workspace }}/backend/build/reports/jacoco/test/jacocoTestReport.xml
          token: ${{ secrets.GITHUB_TOKEN }}
          title: Backend Code Coverage
          update-comment: true
          min-coverage-overall: 0
          min-coverage-changed-lines: 0
```

### ルール

- どちらかのジョブが失敗したPRはマージ禁止(ブランチ保護設定)
- E2E(Playwright)はPRごとには実行しない(遅いため)。`.github/workflows/e2e.yml`(workflow_dispatch)をリリース前に手動実行
- 依存更新は Dependabot(週次、`gradle` / `pnpm` / `github-actions`)
- backendの `./gradlew check` にはSpotless(`googleJavaFormat`によるフォーマット・未使用import削除・import順序)のチェックが含まれる。frontendの `pnpm run lint` / `pnpm run format` に相当する役割(`backend/build.gradle` の `spotless { java { ... } }` 参照、`08_development_setup.md` §7)

### カバレッジ可視化(PRコメント)

- `madrapps/jacoco-report`(backend)・`davelosert/vitest-coverage-report-action`(frontend)がPRごとに「全体カバレッジ」と「PRで変更した行のカバレッジ」をsticky commentで投稿する(外部SaaSは使わずGitHub Actions内で完結)
- `min-coverage-*` は `0` にしており、CIを落とす閾値としては使わない。domain層90%の強制は既存の `jacocoTestCoverageVerification`(`backend/build.gradle`)が担う。役割は「強制はdomain層のみ・それ以外は可視化のみ」で分離している(`09_test_strategy.md` §2)

---

## 2.5 AIレビュー・自動修正(`.github/workflows/ai-review.yml`)

PRごとに `anthropics/claude-code-action@v1` でAIレビューを実行し、1ラン内で「レビュー → ゲート → 修正 → 再レビュー」を完結させる。

```text
PR(open/push) → Reviewer(sticky comment更新, VERDICT: PASS/FAIL)
  PASS    → 終了(マージ判断は人間)
  FAIL    → ゲート判定(bash・決定的)
              ├ 起動可 → Fixer(Critical/Majorのみ修正 → 検証 → レポート投稿 → push)→ 再レビュー
              └ 起動不可 → needs-humanラベル + 理由コメント
  UNKNOWN → ├ ワークフロー変更PR → needs-humanラベル + 説明コメント(CIは落とさない)
            └ それ以外           → needs-humanラベル + **CI失敗**(レビュー未実施を素通りさせない)
```

- **役割定義**: Reviewer = `.claude/agents/reviewer.md` / Fixer = `.claude/agents/fixer.md`。品質基準は `.claude/04_quality/`
- **ゲート(Fixer起動条件)**: 以下のいずれかに該当したらFixerを起動せず `needs-human` ラベルを付ける
  - 聖域への指摘: `backend/**/domain/service/**`(マッチング・順位計算)、`05_swiss_pairing_algorithm.md`(`SANCTUARY_PATTERN`)
  - 自動修正回数が上限(`MAX_FIX_ATTEMPTS`=3、`[ai-fix]` コミット数で計測)
  - 過去に `Fixed: <slug>` 済みの指摘が再指摘された(修正が無効)
  - レポートの形式崩れ(指摘を抽出できない)
  - レポートが存在しない(`UNKNOWN`)。この場合はCIも失敗させる
- **needs-human ラベル**: 付いている間は自動ループ停止。人間が対応してラベルを外すと再開
- **Fixerのpush**: pushがpull_requestイベントを発火しない環境向けに、同一ラン内でCI手動起動(`workflow_dispatch`)と再レビューを行う。発火する環境ではconcurrencyで新しいランに引き継がれる
- **マージ判断は常に人間**。PASSは「人間レビューの前処理完了」の意味

### fail-closed の原則

ゲートはレポート本文から `VERDICT:` 行を読む。レポートが見つからない場合は `UNKNOWN` となり、
**PASSではなく異常として扱う**。ここをPASS相当にすると「レビューが一度も行われていないのに
AIレビュー済みとしてマージできる」状態になり、ゲートとして意味を失う。

> **実際にこの事故が起きていた。** 当時のゲートは `VERDICT != FAIL` で素通りしていたため、
> AI Reviewは緑のまま長期間まったく機能していなかった。原因は独立に2つあった:
>
> 1. **ワークフロー検証によるスキップ** — `claude-code-action` は、実行中のワークフローファイルが
>    既定ブランチの内容と一致しない場合、実行を拒否する(PRからレビュー用ワークフローを書き換えて
>    シークレットを持ち出す攻撃を防ぐためのセキュリティ機構)。つまり
>    **`.github/workflows/**` を変更するPRはAIレビューされない**
> 2. **サブエージェントへの委譲** — Reviewerがレビュー作業をバックグラウンドのサブエージェントに
>    委譲して即座にターンを終えており、ジョブ終了と同時にそのエージェントが強制終了されるため、
>    レポートが永久に投稿されなかった

対策は2層:

1. **fail-closed**(構造): `UNKNOWN` を `needs-human` にする。原因が何であれ素通りを防ぐ。
   原因が(1)のワークフロー検証スキップである場合は**回避不能**なのでCIは落とさず、
   「人間がレビューすること」を明示するコメントに留める。それ以外はCIを失敗させる
2. **委譲の禁止**(原因2への直接対処): 各エージェントのpromptで委譲を禁止し、
   `--disallowedTools "Task,Agent"` を指定する。`ai-qa.yml` にも同じ対策を入れている

### 運用上の帰結: ワークフローを変更するPRはAIレビューされない

上記(1)により、`.github/workflows/**` に触れるPRは構造的にAIレビューの対象外になる。
**このようなPRは人間が必ずレビューする**。ゲートが `needs-human` ラベルと説明コメントで明示する。

また、ワークフロー自体の変更は**マージ後に初めて実際の動作を確認できる**。変更をマージしたら、
次のPRで意図どおり動いているかをログで確認すること。


---

## 2.6 テスト弱体化ガード(`.github/workflows/guard.yml`)

AIの自動修正は「指摘や失敗を閉じること」が目的であるため、**テストを弱めて通す**動機が構造的に働く。これを機械的に塞ぐ安全装置であり、**このガードが機能していることを前提に自動修正の範囲を広げる**。

検査本体は `.github/scripts/check-test-weakening.sh`(検証は同ディレクトリの `test-check-test-weakening.sh`)。

### 適用の強度

| 対象 | モード | 検査項目 | 違反時 |
|---|---|---|---|
| `[ai-fix]` / `[ci-fix]` コミットの差分(1コミットずつ) | strict | 全項目 | BLOCK → **CI失敗** + `needs-human` / ESCALATE → `needs-human` のみ |
| PR全体の差分 | light | BLOCK相当のみ | PRコメントで警告(**ゲートしない**) |

人間のPRをゲートしないのは、テストの整理・削除には正当な理由がありうるため。

### 判定

| 検査項目 | 判定 |
|---|---|
| テストの無効化(`@Disabled` / `@Ignore` / `.skip()` / `xit()` / `.only()` の追加) | BLOCK |
| 受け入れケースID(`XXX-AC-nnn`)を含むテストの削除 | BLOCK(台帳のStatusと連動するため人間の判断が要る) |
| `src/main/` の削除を伴わないテストの削除 | ESCALATE |
| アサーションの純減 | ESCALATE(正当なリファクタでも起きうるためブロックしない) |

**実装クラスの廃止に伴うテスト削除は正当**として許容する(同一差分に `src/main/` の削除があり、かつ受け入れケースIDを含まない場合)。

規約側の二重化として `.claude/agents/fixer.md` にも「テストを弱める修正は禁止」を明記している。

---

## 2.7 CI失敗の自動修正(`ci.yml` の `autofix` ジョブ)

CI失敗のうち**コマンド一発で決定論的に直るもの**だけを、AIを使わずに自動修正してpushする。
エージェント課金ゼロ・数十秒で終わり、「普段の開発の煩わしさ」の大半を占める失敗を人間の目に触れさせない。

| 失敗の種類 | 自動修正 | 担当 |
|---|---|---|
| prettier(`format:check`) | `pnpm run format` | **autofix** |
| 生成型の鮮度チェック | `pnpm run generate:api` | **autofix** |
| Spotless | `./gradlew spotlessApply` | **autofix** |
| 型エラー・テスト失敗 | 判断が必要 | 人間 / ci-fixer(将来) |
| カバレッジ不足 | **意図的に自動化しない** | 人間 |

> カバレッジ不足をAIに埋めさせると「アサーションの薄いテストを量産して閾値を通す」= 基準hackそのものになるため、
> 将来ci-fixerを入れる際も**この項目だけは常に人間**に回す。

### 設計上の要点

- **起動条件**: `needs: [frontend, backend]` + `if: failure() && github.event_name == 'pull_request'`。
  失敗したジョブに対応する修正だけを実行する(`needs.frontend.result == 'failure'` 等で分岐)
- **ループ防止**: `[ci-fix]` コミットが既にあれば実行しない。決定論的な修正は1回で収束するため、
  それでも失敗しているなら自動修正で直る種類の失敗ではない
- **差分がなければ何もしない**: 判断を要する失敗(型エラー・テスト失敗)では差分が出ないので、
  コミットもコメントも発生しない
- **`[ci-fix]` コミットの判定は subject のみを対象にする**(`git log --grep` は本文も検索してしまうため。
  §2.5 の自動修正回数カウントと同じ理由)

### GITHUB_TOKEN の push はワークフローを再発火しない

GitHubの仕様上、`GITHUB_TOKEN` によるpushは新しいワークフロー実行を作らない。そのため
push後に `gh workflow run ci.yml --ref <branch>` で明示的にCIを起動する(`actions: write` 権限が必要)。
`ai-review.yml` が持つ保険と同じパターン。

---

## 3. デプロイ(MVP期: 手動スクリプト)

`infra/scripts/deploy.sh`(概略):

```bash
# 1. フロントエンドをビルドし、Spring Bootのstatic配下へ配置
cd frontend && pnpm install --frozen-lockfile && pnpm run build
cp -r dist/* ../backend/src/main/resources/static/

# 2. バックエンドをビルド
cd ../backend && ./gradlew clean build

# 3. EC2へ転送し、サービス再起動
scp build/libs/swiss-stage.jar ec2:/opt/swiss-stage/
ssh ec2 'sudo systemctl restart swiss-stage'
```

### デプロイ運用ルール

- **大会前日・当日のデプロイ禁止**(検証時間が取れないため)
- デプロイ後は必ずスモークチェック: ログイン → 大会一覧表示 → 共有ページ表示
- EC2上は systemd でプロセス管理(`swiss-stage.service`)。JVMオプションは t3.micro(1GB)に合わせ `-Xmx512m`

---

## 4. 環境

| 環境 | 用途 | インフラ |
|------|------|---------|
| local | 開発 | DynamoDB Local + bootRun |
| production | 本番 | EC2 + DynamoDB |

- MVP期はステージング環境を持たない(コスト優先)。代わりに本番DynamoDBに `TEST#` プレフィックスの大会を作って検証し、検証後削除する
- 本番の秘密情報は EC2 の環境変数ファイル(`/opt/swiss-stage/env`、パーミッション600)で管理

---

## 5. 将来(MVP後)

- CodeDeploy によるゼロダウンタイムデプロイ
- mainマージで自動デプロイ(大会カレンダーと連動したデプロイ凍結期間の仕組み)
- CloudFront + S3 でフロントエンドを分離配信
