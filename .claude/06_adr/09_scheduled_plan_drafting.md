# 09. backlog Issueの定期Plan PRドラフト化

- Status: Proposed
- Issue: #145
- Date: 2026-08-04

## 1. 文脈

`04_development_process.md` は「人間がやること」を要件を書く / 質問に答える / Plan PR・実装PRを
Approveする、の3つに絞る方針を掲げている。しかし実際には **Issue作成後、`/plan <issue番号>` を
人間が能動的に実行しないとPlan PRが作られない**。特に `backlog` ラベルの付いたIssue(着手時期
未定)は、着手のタイミングが人間が定期的にIssue一覧を見返すことに依存しており、Issueを書いた
時点では明示的な仕事にならないはずの「着手を思い出す」という暗黙の作業が人間側に残っていた。

この隙間をAIに任せ、人間はPlan PR・実装PRのApproveという判断のゲートに専念できるようにしたい
(詳細な経緯・技術的な選択肢の比較は Issue #145 のやり取りを参照)。

## 2. 決定

週次で起動する GitHub Actions ワークフロー(`.github/workflows/scheduled-planner.yml`、実装PRで
追加)が、`auto-plan:P0`/`auto-plan:P1`/`auto-plan:P2` ラベル(このADRで新設)のいずれかが付いた
Issueのうち、優先度が最も高く・同じ優先度なら最も古いものを1件選び、既存の `/plan` の手順
(分類判定・ADR要否判定・計画作成・受け入れケース追加)に従ってPlan PRのドラフトを自動作成する。

- **認証・実行基盤**: 既存の `ai-review.yml` 等と同じ `anthropics/claude-code-action@v1` +
  `CLAUDE_CODE_OAUTH_TOKEN` を使う(§3案Aとの比較を参照)
- **無人実行時のAskUserQuestion代替**: 対象Issue本文とコメント全体を毎回読み込む。不明点が
  あれば `<!-- swiss-stage-ai-planner-question -->` sticky commentで質問を投稿し、
  `needs-human` ラベルを付けて処理を打ち切る(そのIssueは今回スキップし、次に優先度の高い
  対象へ進む)。次回実行時、最新コメントがこのsticky comment自身のままなら「未回答」として
  再スキップする。人間が返信すればIssueスレッド全体の再読み込みにより自動的に「回答あり」を
  検出し、計画作成を再開する。**質問への回答は人間がIssueに通常どおりコメントするだけでよく、
  何かコマンドを叩く必要はない**
- **対象Issueの選定基準**: `auto-plan:P0/P1/P2` のいずれかを持つことを一次条件とする。
  `backlog` ラベルの有無は問わない(対応時期と定期実装への投入は別軸のため。§3案Cを参照)。
  ただし選定後、`04_development_process.md` §2 のトリガー表で該当Issueの種別が
  (`type:bug`、またはアーキテクチャ・技術選定を含まない`type:chore`)であり、
  そもそもPlan PR不要と判定される場合は、Plan PRを作らずその旨をコメントして
  `auto-plan:P*` ラベルを外す(誤ってラベルが付いた場合の安全弁。`type:feature`は
  トリガー表上つねにPlan PR必須のため該当しない)
- **重複起票の防止**: 既にそのIssueを参照する Plan PR(`Refs #N` を本文に含む、open または
  merged)が存在する場合は選定対象から除外する
- **処理件数**: 1回の実行につき1件のみ(§3案Dを参照)
- **完了後のラベル操作**: 4分類(`.claude/07_plans/06_scheduled_plan_drafting.md` §4.3)ごとに
  `auto-plan:P*` は**必ず**外す(再選定を防ぐための状態遷移。外さないと選定ステップの除外条件
  だけでは弾けず、同じIssueが次回も最優先で選ばれ続け、他のbacklog Issueの着手を妨げてしまう)。
  一方 `needs-human` の扱いは分類によって異なる:
  - **PLANNED**: `auto-plan:P*` を外す。`needs-human` が付いていれば(過去にBLOCKEDだった場合)
    それも外す
  - **NOT_APPLICABLE**: `auto-plan:P*` を外す。`needs-human` は通常付いていないが、念のため
    付いていれば外す
  - **FAILED**: `auto-plan:P*` を外す。ただし **`needs-human` は付ける**(または維持する)。
    検証に失敗した原因は人間が確認すべきであり、`needs-human` まで外すと「何も起きなかった」
    ように見えてしまうため
  - **BLOCKED**: `auto-plan:P*` は外さない(人間の回答を待って再選定させる必要があるため)。
    `needs-human` を付ける
- **スコープ**: Plan PRドラフトの自動作成までであり、実装PRの自動作成・Plan PR/実装PRの
  自動マージは対象外(承認ゲートは人間に残す)

## 3. 却下した案

### 案A: claude.ai側のスケジュール機能(routine/`RemoteTrigger`)を使う

Anthropic側でホストされる永続的なcronで、GitHub Actionsとは独立して動く。技術的には可能だが、
このリポジトリの reviewer/fixer/qa/qa-fixer/ci-fixer/design-reviewer/plan-reviewer は
すべて GitHub Actions + `claude-code-action` + `CLAUDE_CODE_OAUTH_TOKEN` という一貫した基盤の
上にある。claude.ai側のroutineを使うと、設定がリポジトリのバージョン管理外に置かれ、PRで
レビューできず、既存の認証・権限モデル(Appトークンのスコープ制約、§2.5・§2.7の教訓)とも
別建てになる。一貫性を優先し却下した。

(Claude Code内蔵の `CronCreate` は対話セッションに紐づく一時的な仕組み(セッション終了で消滅・
7日で自動失効)であり、リポジトリの自動化基盤としてはそもそも要件を満たさないため比較対象にすら
ならない)

### 案B: 不明点があれば常にスキップしてneeds-humanで止め、人間が手動で`/plan`を叩くまで待つ

質問の再読み込み・sticky comment更新のロジックを実装せず、一律「わからなければ止める。再開は
人間が能動的に `/plan` を実行する」とする案。実装は単純だが、「週次でAIが拾って人間は返信する
だけでよい」という本来の目的(Issue #145)に対して、結局人間が能動的にコマンドを叩く負担が
残ってしまい、自動化の効果が薄い。Issueへの通常のコメント返信だけで次回自動的に再開する設計の
方が、追加実装コストに見合うと判断し採用しなかった。

### 案C: 既存の `backlog` ラベル + `priority:P0/P1/P2`(重大度)をそのまま対象選定に使う

新しいラベル体系を増やさずシンプルに保てる利点があるが、「重大度(本番で顕在化したときの
影響度)」と「定期実装に回してよいか(着手意欲)」は別軸の判断である。重大度は低いが早くAIに
着手させたいIssueや、重大度は高いが人間が直接見たいIssueが両方ありうる。既存の優先度ラベルを
援用すると、通常のIssue優先度判定(`02_severity.md`)と意味がずれるケースが生じるため、
`auto-plan:P0/P1/P2` を独立したラベル体系として新設することにした(ユーザーの判断)。

### 案D: 1回の実行で対象issue全件を処理する

着手が早まる利点があるが、Plan PRの承認・マージは人間の作業であり律速はそこにある。週次で
複数のPlan PRが一気に開くと、レビューキューが溢れて「読み飛ばし」が起きやすくなる
(`ai-qa.yml` をゲート化した経緯 §2.9 と同種の教訓)。1回の実行で最優先の1件だけを処理し、
既存のPlan PRが残っている(未Approve)場合は次点をスキップして待つ設計とした。

## 4. 結果

- 得られるもの: `backlog` を含むIssueの着手判断のうち、実装計画のドラフト作成までを人間の
  「思い出す」作業なしに進められる。既存の自動化基盤(GitHub Actions・claude-code-action)と
  同じ認証・権限モデル・ラベル運用パターン(`needs-human`・sticky comment)を再利用できる
- 引き受けたトレードオフ: `auto-plan:P0/P1/P2` という新しいラベル体系が増える
  (`04_development_process.md` §7 に作成コマンドを追加)。質問駆動の再開ロジックは
  `/plan` 本体より状態管理が複雑になる
- 撤回条件: sticky commentでの質問・再開ロジックの誤検知(未回答なのに再開してしまう、
  回答があるのに検出できない等)が頻発する場合、案Bへ切り替えるか、本ADRを Superseded にして
  別方式へ移行する
