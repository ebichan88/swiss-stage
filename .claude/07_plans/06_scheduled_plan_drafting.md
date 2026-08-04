# 06. backlogの定期的なPlan PRドラフト自動作成

- Status: planned
- Issue: #145
- PR: -

## 1. 背景・目的

`.claude/00_project/04_development_process.md` は「人間がやること」を要件を書く / 質問に答える /
Plan PR・実装PRをApproveする、の3つに絞る方針を掲げているが、実際には **Issue作成後、
`/plan <issue番号>` を人間が能動的に実行しないとPlan PRが作られない**。特に `backlog`
ラベルの付いたIssueは、着手のタイミングが人間の記憶・手作業(定期的にIssue一覧を見返すこと)に
依存しており、Issueを書いた時点では明示的な仕事にならないはずの「着手を思い出す」という暗黙の
作業が人間側に残っている。

この隙間をAIに任せ、人間はPlan PR・実装PRのApproveという判断のゲートに専念できるようにする。
決定の経緯と却下案は `.claude/06_adr/09_scheduled_plan_drafting.md` を参照。

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**。このプランは自動化の**トリガー**を
足すものであり、Plan PR・実装PRの承認ゲート(人間の判断)には一切踏み込まない。

## 2. 画面シナリオ

対象外。このプランは開発プロセス自体の変更であり、エンドユーザー(大会運営者・参加者)向けの
画面変更を伴わない(`03_feature_plan_template.md` 冒頭の対象外規定)。

## 3. UI仕様

該当なし(§2と同じ理由)。

## 4. 技術設計

実装PRで追加するものは大きく3つ: (a) ラベル、(b) 選定ロジックを含むGitHub Actionsワークフロー、
(c) 無人実行用の `planner` エージェント定義。

### 4.1 ラベル(新設)

`auto-plan:P0` / `auto-plan:P1` / `auto-plan:P2` の3種。人間がIssueに手動で付与する
「このIssueは定期実装(自動Plan PRドラフト化)の対象にしてよい」というオプトインの意思表示。
既存の `priority:P0/P1/P2`(重大度。`02_severity.md`)とは別軸(却下案は ADR §3 案C)。
作成コマンドは `04_development_process.md` §7 に追記する。

### 4.2 ワークフロー(`.github/workflows/scheduled-planner.yml`、実装PRで新規作成)

既存の `ai-review.yml` 等と同じ `anthropics/claude-code-action@v1` +
`CLAUDE_CODE_OAUTH_TOKEN` を使う(却下案は ADR §3 案A)。

- **トリガー**: `schedule`(週次。案: `cron: '0 21 * * 1'` = 毎週月曜21:00 UTC/日本時間火曜6:00。
  `mutation.yml` の月曜3:00 UTCと時間帯をずらす)+ `workflow_dispatch`(手動テスト用)
- **権限**: `contents: write`(ブランチ作成・push)/ `pull-requests: write`(Plan PR作成)/
  `issues: write`(コメント・ラベル操作)
- **concurrency**: `group: scheduled-planner` / `cancel-in-progress: false`
  (手動実行とscheduleが重なっても後続を待たせる。実行中のものを取り消して二重選定しない)
- **選定ステップ(bash・決定的)**: `ai-review.yml` の「Fixerゲート判定」と同じ思想で、
  対象Issue番号の決定はAIに委ねずbashで行う。

  ```text
  for P in P0 P1 P2:
    for issue in (auto-plan:P<P> ラベル付きのopen issue、createdAt昇順):
      - 既にそのIssueを参照するPlan PR(本文に "Refs #<N>"、open または merged)があれば skip
      - needs-human が付いており、かつ最新コメントが
        <!-- swiss-stage-ai-planner-question --> 自身(＝未回答)であれば skip
      - 上記のいずれにも該当しなければ TARGET=<N> として選定を終了
  対象なしなら何もせず正常終了(no-op)
  ```

- **plannerエージェント起動**: 選定された `TARGET` を渡し、`.claude/agents/planner.md`
  (実装PRで新規作成)の指示に従って処理させる。他の全エージェントと同じく
  `--disallowedTools "Task,Agent"` でサブエージェントへの委譲を禁止する
  (`11_cicd_design.md` §2.5「委譲の禁止」と同じ理由。ジョブ終了と同時に委譲先が強制終了され
  何も完了しない事故を防ぐ)

### 4.3 `planner` エージェントの分類(`.claude/agents/planner.md`、実装PRで新規作成)

`fixer`/`ci-fixer` と同じく、必ず次のいずれかに分類してから終了する:

| 分類 | 内容 | 事後処理 |
|---|---|---|
| **PLANNED** | Issue本文・コメント全体から計画作成に十分な情報が揃っている(最初から、または過去の質問への回答により) | `/plan` と同じ手順(分類判定→ADR要否→計画作成→受け入れケース→docs-lint→ブランチ→Plan PR作成→Issueコメント)を実行し、`auto-plan:P*`(・付いていれば`needs-human`)を外す |
| **BLOCKED** | 不明点があり、AskUserQuestion相当の質問(最大3問)が必要 | `<!-- swiss-stage-ai-planner-question -->` sticky commentで質問を投稿し `needs-human` を付けて終了 |
| **NOT_APPLICABLE** | 選定されたIssueの種別(`type:bug`、またはアーキテクチャ・技術選定を含まない`type:chore`)が`04_development_process.md` §2のトリガー表でそもそもPlan PR不要と判定される(誤ってラベルが付いた場合の安全弁) | その旨をIssueにコメントし `auto-plan:P*` を外す(Plan PRは作らない) |
| **FAILED** | docs-lint等の検証を通せなかった | 変更を破棄し、その旨をIssueにコメントして `needs-human` を付ける。**`auto-plan:P*` も外す**(PLANNED/NOT_APPLICABLEと同じくラベルを外して選定対象から除く。外さないと選定ステップの除外条件(4.2)に当たらず、同じIssueが次回も最優先で選ばれ続け、他のbacklog Issueが永久に処理されなくなるため) |

4分類のうち **BLOCKED以外(PLANNED/NOT_APPLICABLE/FAILED)は必ず `auto-plan:P*` を外して終了する**。
BLOCKEDだけは人間の回答を待つ必要があるため付けたままにする。この対称性が崩れると、選定ステップ
(4.2)の除外条件(「`needs-human` 付きかつ最新コメントが質問自身」)だけでは弾けない状態が生まれ、
特定のIssueが毎回選定され続けて他のIssueの着手を止めてしまう。

`/plan` コマンド自体の判断ロジック(ADR要否判定・分類判定・受け入れケースの洗い出し等)は
変更しない。`planner` は「不明点の確認手段をAskUserQuestionからIssueコメントの往復に置き換えた、
無人実行版の `/plan`」と位置づける。

### 4.4 Plan PR本文

既存の `/plan` と同じテンプレート(`Refs #N` + 概要 + 計画へのリンク + 変更内容)を使う。
末尾に「このPRは週次の自動実行(`scheduled-planner.yml`)によるドラフトです」という一文を
追加し、人間が読んだときに出自を判別できるようにする。

## 5. 受け入れケース

該当なし。このプランは開発プロセスの自動化であり、既存の受け入れケース体系
(`00_acceptance_policy.md`)が対象とする「大会運営プラットフォームの機能」ではない
(`07_plans/01_upstream_process_automation.md` §5と同じ扱い)。プロセスの実効性は
§7 DoDの検証手順で確認する。

## 6. 更新する設計資料

- [x] `.claude/06_adr/09_scheduled_plan_drafting.md`(このPRで新規作成)
- [x] `.claude/07_plans/06_scheduled_plan_drafting.md`(このファイル)
- [x] `.claude/00_project/04_development_process.md`(§1 フローチャートに自動経路を追記・
      §7 に `auto-plan:P0/P1/P2` の作成コマンドを追記)
- [x] `.claude/01_development_docs/11_cicd_design.md`(§1.5 ワークフロー一覧に追記・
      新設 §2.13 で設計を記述)
- [ ] `.claude/agents/planner.md`(実装PRで新規作成。このPRには含めない)
- [ ] `.github/workflows/scheduled-planner.yml`(実装PRで新規作成。このPRには含めない)

## 7. DoD(完了の定義)

- [ ] `python3 .github/scripts/docs-lint.py` が通る
- [ ] `workflow_dispatch` で手動実行し、`auto-plan:P2` を付けたテスト用Issueに対して
      選定 → Plan PR作成 までが実際に動くことを確認する
- [ ] 不明点があるテスト用Issueで BLOCKED(質問コメント + `needs-human`)になることを確認し、
      人間が返信した後の次回実行で再開して PLANNED になることを確認する
- [ ] `type:bug` のIssueに誤って `auto-plan:P2` を付けても NOT_APPLICABLE でPlan PRが
      作られないことを確認する
- [ ] 既にPlan PR(open)が存在するIssueに `auto-plan:P*` が付いていても、選定ステップで
      スキップされ次点のIssueが選定されることを確認する(重複起票防止)
- [ ] `auto-plan:P1` の方が `auto-plan:P0` より古いIssueであっても、`auto-plan:P0` のIssueが
      先に選定されることを確認する(優先度順の検証)
- [ ] FAILEDになったIssueの `auto-plan:P*` が外れ、次回実行で再選定されない(＝他のIssueの
      着手を妨げない)ことを確認する
- [ ] 実装PRで `.claude/agents/planner.md` / `.github/workflows/scheduled-planner.yml` が
      追加され、この計画の §6 チェックボックスがすべて埋まる

## 8. リスク・未確定事項

- **sticky commentの往復ロジックの精度**: 「最新コメントが質問自身か」の判定を誤ると、
  未回答なのに計画作成を試みる/回答済みなのに永久にスキップする、のどちらかの事故になりうる。
  実装PRのレビュー・手動テストで重点的に確認する
- **週次cronの具体的な時刻**: 実装PR時点でCI混雑状況を見て調整してよい(この計画では
  「他のscheduleワークフローと時間帯をずらす」方針のみ固定する)
- **人手による誤ラベル**: `auto-plan:P*` を人間が誤って複数個(P0とP2など)同時に付けた場合の
  優先度は「最も高いもの」を採用する(実装PR側でこの解釈を明記する)
