# 10. 新規画面ストーリー先行作成をPlan PRの例外として実行可能にする

- Status: planned
- Issue: #170
- PR: -

## 1. 背景・目的

`.claude/01_development_docs/10_frontend_design.md` §7 と `.claude/00_project/03_feature_plan_template.md`
は、新規画面・大きなレイアウト変更ではストーリー(`src/pages/XxxPage.stories.tsx`)を実装前に作成し、
実機を起動せずにUIを確認・合意すると定めている。しかし実績を確認したところ、この規定は運用されて
いない。`.stories.tsx` が追加されたのは3回(#80, #121, #151)のみで、すべて `feat:` の実装PR内での
後付け作成であり、「新規画面をゼロから作る前にストーリーで合意した」事例は存在しない。ストーリー
作成と画面本体の変更が同一PRに同居しており、「実装前の合意ゲート」として機能していない。

放置すると、UI実装後に「これじゃない」と気づく手戻り(本Issue化の契機となった事象)を防げないまま、
`10_frontend_design.md` §7 の規定が名目だけの記述になり続ける。

本プランは、`.claude/00_project/04_development_process.md` が定める「Plan PRはコード0行」という
原則に、対象画面の `.stories.tsx` のみを例外として認める運用(ADR `.claude/06_adr/11_story_first_agreement.md`
の案A)を、実行可能な形でドキュメント・コマンドに落とし込む。

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**。本プランは開発プロセス自体の変更であり、
大会運営プラットフォームの機能(マッチング・順位計算等)には触れない。

## 2. 画面シナリオ

対象外。本プランは開発プロセス自体の変更であり、エンドユーザー(大会運営者・参加者)向けの画面変更を
伴わない(`03_feature_plan_template.md` 冒頭の対象外規定)。

## 3. UI仕様

該当なし(§2と同じ理由)。

## 4. 技術設計

以下の6文書を実装PRで更新する。決定の中身(何を・なぜ)はADR(`06_adr/11_story_first_agreement.md`)
と本プランに集約し、各文書には「現在の仕様」として反映するだけに留める。

### 4.1 `.claude/00_project/04_development_process.md`

- §1(全体フロー)の「Plan PR(コード0行)」という表現に、「新規画面・大きなレイアウト変更を含む
  場合は対象画面の `.stories.tsx` のみ例外」という趣旨の注記を追記する。
- §2(トリガー表)の直後にある「Plan PR が触るのは ADR・プラン・受け入れケース台帳・
  `schema/openapi.yaml` まで」という一文に、「(新規画面・大きなレイアウト変更の場合は対象画面の
  `.stories.tsx` を含む)」を追記する。
- §5(プランの運用ルール)近辺に、Plan PRへ `.stories.tsx` を含める場合の運用を1〜2段落で追記する:
  対象範囲(新規画面 + 既存画面の大きなレイアウト変更。`03_feature_plan_template.md` と同一基準)、
  新規画面ではストーリー内にインラインのプレースホルダー実装を許可すること、実装PRで本物のページに
  差し替えストーリーを実importへ書き換えること、順序の強制はPlan PR承認のみに委ね追加の機械的
  チェックは設けないこと。

### 4.2 `.claude/00_project/03_feature_plan_template.md`

- §3 UI仕様の末尾にある「新規画面・大きなレイアウト変更では、ここに挙げた画面のストーリーを実装前に
  作成し、実機を起動せずにUIを確認・合意する」という記述を、「Plan PRの一部として作成し、Plan PRの
  レビュー・マージをもって合意とする」という実行可能な表現に書き換える。新規画面の場合はストーリー
  内にプレースホルダー実装を書いてよいこと、既存画面の大きなレイアウト変更では本物のページを直
  importすることを併記する。
- §6「Plan PRで更新するもの」チェックリストに、該当する場合の項目として
  `src/pages/XxxPage.stories.tsx` を追加する。

### 4.3 `.claude/01_development_docs/10_frontend_design.md` §7

- 「対象範囲」節に、Plan PRの一部として作成する運用であることを明記する。
- 新規画面(0→1)の場合の例外として、「ページ本体(`src/pages/XxxPage.tsx`)が未実装の間は、
  ストーリーファイル内にインラインのプレースホルダー実装を置いてよい(本節の『ストーリー専用ダミー
  画面は作らない』原則のPlan PR時点での例外)。実装PRで本物のページに差し替え、ストーリーを実import
  へ書き換える」と追記する。
- 既存画面の大きなレイアウト変更の場合は、この例外を使わず引き続き本物のページを直importすることを
  明記する(プレースホルダーは新規画面のみの救済策)。

### 4.4 `.claude/01_development_docs/09_test_strategy.md`(Storybook節)

- 「新規画面・大きなレイアウト変更では、実装前に...作成し...合意する」の記述を、Plan PRの一部として
  作成される旨に揃えて更新する。
- Plan PRブランチでも既存の `ci.yml`(frontendジョブ: lint/type-check/build)と `vrt.yml` が
  pathsフィルタ・イベント条件の変更なしに自動実行される旨、新規ストーリーは初回VRTでベースライン
  欠如により失敗するが `vrt.yml` は非ブロッキング運用のため実害はない旨を注記する。

### 4.5 `.claude/commands/plan.md`

- 「4. 計画の作成」の後(または新設のステップ)に、UI仕様確定後、対象画面が新規画面・大きなレイアウト
  変更に該当する場合は `src/pages/XxxPage.stories.tsx` を作成する手順を追加する。
- 新規画面の場合はストーリー内にプレースホルダー実装を書く旨、既存画面のレイアウト変更の場合は本物の
  ページを直importする旨を分岐で示す。
- ストーリー作成後、AskUserQuestion等で人間にUIの見た目を確認してもらう、あるいはPlan PRのレビュー
  自体を合意のタイミングとする旨を明記する(新しい承認ステップは発明しない)。

### 4.6 `.claude/commands/pr.md`

- 「3. ドキュメント整合の確認」に、対応する `.stories.tsx` が存在する場合(Plan PRで作成された場合)、
  新規画面であればプレースホルダーを本物のページ実装に差し替え・ストーリーを実importへ更新したかを
  確認するステップを追加する。

### 4.7 CI・自動化への影響(コード変更は不要)

- 既存の `ci.yml`(frontendジョブ)は `pull_request` イベントで無条件に起動する
  (`# paths-ignoreを付けない` のコメントどおり、docs-lintのため常時起動)ため、Plan PRブランチに
  `.stories.tsx` が追加されれば自動的にlint/type-check/buildが実行される。追加のワークフロー変更は
  不要。
- 既存の `vrt.yml` は `frontend/src/**` 等のpathsフィルタで起動するため、Plan PRブランチの新規
  ストーリーに対しても自動実行される。新規ストーリーはベースライン画像が存在しないため初回は失敗
  するが、`vrt.yml` は非ブロッキング運用(`maxDiffPixelRatio: 0` の安定実績待ち)であり実害はない。
  ベースラインは実装PR側で意図的に確定させる(`./scripts/vrt.sh --update` または
  `gh workflow run vrt.yml -f update_snapshots=true`)。追加のワークフロー変更は不要。

## 5. 受け入れケース

該当なし。`04_development_process.md` §2 のトリガー表により、アーキテクチャ・技術選定の決定は
Plan PRとADRが必須だが受け入れケースは対象外(「—」)。本プランは開発プロセスの変更であり、既存の
受け入れケース体系(`00_acceptance_policy.md`)が対象とする「大会運営プラットフォームの機能」では
ない(`07_plans/06_scheduled_plan_drafting.md` §5と同じ扱い)。

## 6. 更新する資料

### Plan PRで更新するもの

- [x] `.claude/06_adr/11_story_first_agreement.md` — ADRを新規作成
      (`04_development_process.md` §3 の条件1「後から覆すのが高くつく決定」・条件2「複数案比較」・
      条件3「CLAUDE.mdの落とし穴に増えうる決定」すべてに該当)
- [x] `.claude/07_plans/10_story_first_agreement.md` — 本ファイル

### 実装PRで更新が必要な設計ドキュメント(今は更新しない・実装時の申し送り)

- [ ] `.claude/00_project/04_development_process.md` — §4.1
- [ ] `.claude/00_project/03_feature_plan_template.md` — §4.2
- [ ] `.claude/01_development_docs/10_frontend_design.md` §7 — §4.3
- [ ] `.claude/01_development_docs/09_test_strategy.md`(Storybook節) — §4.4
- [ ] `.claude/commands/plan.md` — §4.5
- [ ] `.claude/commands/pr.md` — §4.6

## 7. DoD(完了の定義)

- [ ] 上記6文書がすべて更新され、相互に矛盾がない(対象範囲・プレースホルダーの扱い・合意タイミングの
      記述が一致している)
- [ ] `python3 .github/scripts/docs-lint.py` が通る
- [ ] `.claude/06_adr/11_story_first_agreement.md` の `Status` が `Accepted` に更新されている
      (`/pr` が実装PRで更新)
- [ ] このプランで扱う変更はコード実装を伴わないため `pnpm run check` / `./gradlew check` は対象外
      (ドキュメント更新のみ)
- [ ] 実際に新規画面(または既存画面の大きなレイアウト変更)を伴う次のPlan PRで、`.stories.tsx` を
      含めた運用が実行可能であることを確認する(このプランのDoDとして即時のE2E確認は求めず、次の
      利用機会での検証を申し送りとする)

## 8. リスク・未確定事項

- プレースホルダー実装 → 実装PRでの実import置き換え、という二度書きの手間が実際の運用で許容できるかは
  未検証。負担が大きいと分かった場合はADRの撤回条件に従い案B・案Cへの切り替えを再検討する。
- 「Plan PRはコード0行」という説明のシンプルさが、例外規定の追加によりやや複雑化する。CLAUDE.mdの
  「避けるべき落とし穴」への追記が必要かは実装PR時点で判断する。
- 新規ストーリーがVRT初回実行で毎回失敗する(非ブロッキングだが)ノイズが、運用開始後に見過ごされ
  やすくなるリスクがある。実装PRの `09_test_strategy.md` への注記(§4.4)で軽減を図るが、実運用の
  様子を見て通知方法の見直しが必要になる可能性がある。
