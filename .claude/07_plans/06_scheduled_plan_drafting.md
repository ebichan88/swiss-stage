# 06. backlogの定期的なPlan PRドラフト自動作成

- Status: in_progress
- Issue: #145
- PR: #146(Plan PR) / #150(実装PR)

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

- **トリガー**: `schedule`(週次。`cron: '0 21 * * 5'` = 毎週金曜21:00 UTC/日本時間土曜6:00。
  平日より利用状況に余裕がある期間であり、かつ土曜であれば人間がPlan PRをレビューする時間を
  確保しやすいことから週末の頭を選んだ。`mutation.yml`(月曜3:00 UTC)とも日・時刻ともに
  重ならない)+ `workflow_dispatch`(手動テスト用)+ `issue_comment`(即時再開用。後述)
- **権限**: `contents: write`(ブランチ作成・push)/ `pull-requests: write`(Plan PR作成)/
  `issues: write`(コメント・ラベル操作)/ `id-token: write`(claude-code-actionのOIDCトークン
  交換に必須。既存の`ai-review.yml`/`ai-qa.yml`/`ai-design-review.yml`/`ai-plan-review.yml`
  すべてに存在する権限で、本ワークフローも同じ認証方式を使うため必要)
- **concurrency**: `group: scheduled-planner` / `cancel-in-progress: false`
  (手動実行とscheduleが重なっても後続を待たせる。実行中のものを取り消して二重選定しない)
- **選定ステップ(bash・決定的)**: `ai-review.yml` の「Fixerゲート判定」と同じ思想で、
  対象Issue番号の決定はAIに委ねず、シェルスクリプトで機械的に行う。

  ```text
  for P in P0 P1 P2:
    for issue in (auto-plan:P<P> ラベル付きのopen issue、createdAt昇順):
      - 既にそのIssueを参照するPlan PR(本文に "Refs #<N>"、open・merged・closed(未マージ)の
        いずれでも)があれば skip。**closed(未マージ)の場合は追加で**、その旨をIssueに
        コメントして needs-human を付ける(却下された可能性が高いため可視化する。再指摘は
        冪等でよい。再挑戦には人間が auto-plan:P* を外して付け直す必要がある)
      - needs-human が付いており、かつ最新コメントが
        <!-- swiss-stage-ai-planner-question --> 自身(＝未回答)であれば skip
      - リモートに feature/plan-auto-<N>-* ブランチが既に存在すれば
        (前回実行が完走せず残った痕跡とみなす)、その旨をIssueにコメントして
        needs-human を付け skip(§4.3「途中失敗時の扱い」参照)
      - 上記のいずれにも該当しなければ TARGET=<N> として選定を終了
  対象なしなら何もせず正常終了(no-op)
  ```

  `"Refs #<N>"` の一致判定は大文字小文字を区別しない(`refs #12` 等の表記ゆれも許容する)
  正規表現でPR本文を検索する。Plan PR本文は `/plan` のテンプレート(`.claude/commands/plan.md`
  手順8)により常に `Refs #<N>` で始まるため通常は表記ゆれが起きないが、判定を厳密な完全一致に
  すると人間が手で作った例外的なPlan PRを誤検知(見逃し)しうるため、緩めに倒す。
  **ただし番号側は完全一致にする**: `Refs #(\d+)` で数値を抽出し、対象Issue番号と文字列として
  完全一致するものだけをヒットとする(単純な部分一致にすると、Issue #14 の選定時に無関係な
  Issue #145 向けのPlan PR本文「Refs #145」に部分一致し、Issue #14 が永久に選定対象から
  外れる事故になりうるため)。

- **`issue_comment`トリガーによる即時再開**: BLOCKEDへの人間の返信を週次scheduleまで待たずに
  反映するため、`issue_comment: types: [created]` もトリガーに加える。job条件(`if:`)で、
  コメントされたIssueに `needs-human` と `auto-plan:P0/P1/P2` のいずれかが**同時に**付いている
  場合のみこの即時再開パスを有効にする(それ以外のコメント・Issueでは何もしない)。この場合、
  優先度スキャンによる選定は行わず、コメントされたIssue番号をそのまま `TARGET` とし、上記の
  除外チェック(既存Plan PRの有無・ブランチ存在)だけを行ってから `planner` を起動する。
  - **自己トリガーにはならない**: `planner` が質問を更新する操作は既存sticky commentの
    PATCH(編集)であり、これは `issue_comment.edited` を発火させるが `.created` は発火しない。
    `types: [created]` に絞っているため、`planner`自身の更新が再度自分を呼び出すことはない
  - **週次scheduleとの並行実行はない**: 同じ `concurrency: group: scheduled-planner` を使う
    ため、コメント起点の実行とschedule起点の実行が同時に走ることはなく、後着は待たされるだけ
  - **頻度を上げる案は採らない**: schedule自体を毎日実行にする代替案も考えられるが、
    バッチ選定(週次でレビューキューを溢れさせない供給ペース)とBLOCKED返信への反応速度は
    別の要求であり、頻度を上げても後者の体感速度改善には限界がある(最悪ほぼ1日待つ)上、
    前者の目的を弱めてしまう。両立できる `issue_comment` トリガーの追加を選んだ
- **ブランチ命名規約**: `feature/plan-auto-<issue番号>-<slug>`。人間の `/plan` が使う
  `feature/plan-<slug>` と衝突しないよう `auto` を挟み、かつIssue番号を含めることで
  選定ステップが「同じIssueに対する前回の残骸」を機械的に検出できるようにする
- **plannerエージェント起動**: 選定された `TARGET` を渡し、`.claude/agents/planner.md`
  (実装PRで新規作成)の指示に従って処理させる。他の全エージェントと同じく
  `--disallowedTools "Task,Agent"` でサブエージェントへの委譲を禁止する
  (`11_cicd_design.md` §2.5「委譲の禁止」と同じ理由。ジョブ終了と同時に委譲先が強制終了され
  何も完了しない事故を防ぐ)
- **permission-mode**: `planner` はブランチ作成・push・`gh pr create` を無人実行で行うため、
  `ai-review.yml` のFixerステップと同じ理由(`main` の `.claude/settings.json` は
  git push が ask 設定だが、CIは無人実行のため ask は常に拒否扱いになり push が黙って失敗する)
  で `--allowedTools` による許可範囲の限定とセットで `--permission-mode bypassPermissions`
  を付ける。実装PRでこれを付け忘れると、選定・BLOCKED判定までは動くのにPlan PR作成の
  直前(ブランチpush)だけが無言で失敗し、§4.2の「ブランチ存在チェックによる途中失敗検知」
  経由でしか気づけない事故になる

### 4.3 `planner` エージェントの分類(`.claude/agents/planner.md`、実装PRで新規作成)

`fixer`/`ci-fixer` と同じく、必ず次のいずれかに分類してから終了する:

| 分類 | 内容 | 事後処理 |
|---|---|---|
| **PLANNED** | Issue本文・コメント全体から計画作成に十分な情報が揃っている(最初から、または過去の質問への回答により) | `/plan` と同じ手順(分類判定→ADR要否→計画作成→受け入れケース→docs-lint→ブランチ→Plan PR作成→Issueコメント)を実行し、`auto-plan:P*`(・付いていれば`needs-human`)を外す。**Issue本文の進捗チェックリスト「Plan PR」項目も自動でチェックする**(`/plan`手順9は人間に確認するが、`planner`は無人実行のため`auto-plan:P*`の付与自体を事前同意とみなし自動編集する。`06_adr/09_scheduled_plan_drafting.md`§2参照) |
| **BLOCKED** | 不明点があり、AskUserQuestion相当の質問(最大3問)が必要 | `<!-- swiss-stage-ai-planner-question -->` sticky commentで質問を投稿し `needs-human` を付けて終了 |
| **NOT_APPLICABLE** | 選定されたIssueの種別(`type:bug`、またはアーキテクチャ・技術選定を含まない`type:chore`)が`04_development_process.md` §2のトリガー表でそもそもPlan PR不要と判定される(誤ってラベルが付いた場合の安全弁) | その旨をIssueにコメントし `auto-plan:P*` を外す(Plan PRは作らない) |
| **FAILED** | docs-lint等の検証を通せなかった | 変更を破棄し、その旨をIssueにコメントして `needs-human` を付ける。**`auto-plan:P*` も外す**(PLANNED/NOT_APPLICABLEと同じくラベルを外して選定対象から除く。外さないと選定ステップの除外条件(4.2)に当たらず、同じIssueが次回も最優先で選ばれ続け、他のbacklog Issueが永久に処理されなくなるため) |

`/plan`の手順(6. 検証 → 7. ブランチ作成とコミット → 8. プッシュとPR作成)のとおり、**docs-lint等の
検証はブランチ作成・pushより前**に行う。したがってFAILED(検証失敗)に分類される時点では
`feature/plan-auto-<N>-*` ブランチはまだpushされておらず、§4.2の途中失敗検知(ブランチ存在
チェック)とは経路が独立している。「FAILEDなのにブランチが残っていて次回needs-humanの原因表示が
実態とずれる」という事態は、この手順の順序が守られている限り起こらない。

4分類のうち **BLOCKED以外(PLANNED/NOT_APPLICABLE/FAILED)は必ず `auto-plan:P*` を外して終了する**。
BLOCKEDだけは人間の回答を待つ必要があるため付けたままにする。この対称性が崩れると、選定ステップ
(4.2)の除外条件(「`needs-human` 付きかつ最新コメントが質問自身」)だけでは弾けない状態が生まれ、
特定のIssueが毎回選定され続けて他のIssueの着手を止めてしまう。

`/plan` コマンド自体の判断ロジック(ADR要否判定・分類判定・受け入れケースの洗い出し等)は
変更しない。`planner` は「不明点の確認手段をAskUserQuestionからIssueコメントの往復に置き換えた、
無人実行版の `/plan`」と位置づける。

**途中失敗時(4分類のいずれにも到達できない場合)の扱い**: 上表の4分類は `planner` エージェント
が最後まで動いて自己申告する結果であり、ジョブタイムアウト・インフラ障害等でエージェント自体が
途中で強制終了された場合はどの分類にも到達せず、ラベル操作もコメント投稿も行われない。この場合
`auto-plan:P*` は外れないまま残るが、§4.2の選定ステップに追加した
「`feature/plan-auto-<N>-*` ブランチが既に存在すれば skip + `needs-human`」というチェックが
安全弁になる。ブランチはPlan PR作成前(コミット・push後)に作られるため、途中失敗時は
このブランチだけが残った状態になり、次回実行時にこのチェックで確実に検出できる。**ブランチの
自動削除・自動再作成はしない**(人間が原因(タイムアウトの再発か、単発の障害か)を確認してから
判断すべきため。誤って自動削除すると、実際にはまだ実行中の並行ジョブのブランチを壊す危険もある)。

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
- [x] `.claude/agents/planner.md`(このPRで新規作成)
- [x] `.github/workflows/scheduled-planner.yml`(このPRで新規作成)

## 7. DoD(完了の定義)

- [ ] `python3 .github/scripts/docs-lint.py` が通る
- [ ] `auto-plan:P0/P1/P2` を持つopen issueが0件の状態で実行し、no-opで正常終了することを確認する
- [ ] `workflow_dispatch` で手動実行し、`auto-plan:P2` を付けたテスト用Issueに対して
      選定 → Plan PR作成 までが実際に動くことを確認する。あわせてPLANNED完了後、対象Issue
      本文の進捗チェックリスト「Plan PR」項目が自動でチェックされていることを確認する
- [ ] 不明点があるテスト用Issueで BLOCKED(質問コメント + `needs-human`)になることを確認し、
      人間が返信した後の次回実行で再開して PLANNED になることを確認する
- [ ] BLOCKED(`needs-human`+`auto-plan:P*`が付いた)テスト用Issueに人間が返信すると、
      週次scheduleを待たずに`issue_comment`トリガーで即時に再処理され、PLANNEDになることを
      確認する(即時再開の直接検証)。あわせて、これらのラベルが付いていないIssueへの
      コメントでは何も起きないことも確認する
- [ ] BLOCKEDのまま人間からの返信がない状態で次回実行すると、最新コメントがsticky comment
      自身のままなので再度skipされ(`needs-human`は付いたまま)、次点の優先度のIssueが
      選定されることを確認する(返信ありと対になる境界の確認)
- [ ] BLOCKED→人間の返信→再選定されたテスト用Issueで、再び別の不明点が生じた場合、
      新規コメントではなく同一の `<!-- swiss-stage-ai-planner-question -->` sticky comment
      がPATCH更新されることを確認する(2巡目のBLOCKED)
- [ ] `auto-plan:P*` を持つ有効な対象Issueが2件以上ある状態で1回実行すると、Plan PRが
      1本だけ作成され、2件目のIssueは(ラベルが付いたまま)次回実行まで未処理で残ることを
      確認する(1回1件のみ処理の直接検証)
- [ ] 同一Issueに `auto-plan:P0` と `auto-plan:P2` が同時に付いている場合、`P0` として
      選定されることを確認する(複数ラベル付与時の組み合わせの検証)
- [ ] `type:bug` のIssueに誤って `auto-plan:P2` を付けても NOT_APPLICABLE でPlan PRが
      作られないことを確認する
- [ ] アーキテクチャ・技術選定を含まない `type:chore` のIssueに誤って `auto-plan:P2` を
      付けても NOT_APPLICABLE でPlan PRが作られないことを確認する
- [ ] BLOCKEDで一度スキップされた(`needs-human`が付いた)テスト用Issueが、人間の返信後の
      再選定でNOT_APPLICABLEと判定された場合、`auto-plan:P*` に加えて `needs-human` も
      外れることを確認する
- [ ] アーキテクチャ・技術選定を**含む** `type:chore` のIssueに `auto-plan:P*` を付けると
      PLANNEDとなり、ADR(`.claude/06_adr/`)を含むPlan PRが作成されることを確認する
      (NOT_APPLICABLE境界の反対側)
- [ ] Issue #14 のような番号に対し、無関係な Issue #145 向けのPlan PR(本文「Refs #145」)が
      存在していても誤って「既にPlan PRあり」と判定されない(番号の完全一致)ことを確認する
- [ ] `refs #12`(小文字)のような表記ゆれのPlan PR本文でも重複起票防止が機能することを確認する
- [ ] 既にPlan PR(open)が存在するIssueに `auto-plan:P*` が付いていても、選定ステップで
      スキップされ次点のIssueが選定されることを確認する(重複起票防止)
- [ ] 既にPlan PR(**merged**)が存在するIssueに `auto-plan:P*` が付いていても、選定ステップで
      スキップされ次点のIssueが選定されることを確認する(重複起票防止・merged側)
- [ ] 既にPlan PR(**closed・未マージ**)が存在するIssueに `auto-plan:P*` が付いている場合、
      選定ステップでスキップされ次点のIssueが選定されること、かつその旨のコメントと
      `needs-human` が付くことを確認する(重複起票防止・closed側)
- [ ] `auto-plan:P1` の方が `auto-plan:P0` より古いIssueであっても、`auto-plan:P0` のIssueが
      先に選定されることを確認する(優先度順の検証)
- [ ] 同じ `auto-plan:P<N>` を持つ複数Issueがある場合、作成日時が最も古いものが選定される
      ことを確認する(同一優先度内のタイブレークの検証)
- [ ] FAILEDになったIssueの `auto-plan:P*` が外れ、`needs-human` が付与される(既に付いて
      いれば維持される)ことを確認する。あわせて次回実行で再選定されない(＝他のIssueの
      着手を妨げない)ことを確認する
- [ ] `feature/plan-auto-<N>-*` ブランチが残った状態(途中失敗を模擬)で実行し、選定ステップが
      そのIssueをskipして `needs-human` を付け、次点のIssueへ進むことを確認する
- [ ] 実装PRで `.claude/agents/planner.md` / `.github/workflows/scheduled-planner.yml` が
      追加され、この計画の §6 チェックボックスがすべて埋まる

## 8. リスク・未確定事項

- **sticky commentの往復ロジックの精度**: 「最新コメントが質問自身か」の判定を誤ると、
  未回答なのに計画作成を試みる/回答済みなのに永久にスキップする、のどちらかの事故になりうる。
  実装PRのレビュー・手動テストで重点的に確認する。**先行スパイクは置かない**: このロジック
  自体は「sticky commentを1本更新し続け、直近のコメントの発信元で状態を判定する」という、
  reviewer/qa/plan-reviewer/design-reviewer/`/apply-review` が既に本番運用している
  パターンの単純な流用であり、技術的な新規性は低い。新規性があるのは「BLOCKEDでの再開判定」
  という利用のされ方のみのため、独立したスパイクではなく実装PR本体の中で(§7 DoDの手動テスト
  として)重点確認すれば十分と判断した
- **週次cronの具体的な時刻**: 決定済み。毎週金曜21:00 UTC(日本時間土曜6:00)。平日より
  利用状況に余裕がある期間であり、かつ土曜であれば人間がPlan PRをレビューする時間を
  確保しやすいことから選んだ(`mutation.yml` の月曜3:00 UTCとも重ならない)
- **人手による誤ラベル**: `auto-plan:P*` を人間が誤って複数個(P0とP2など)同時に付けた場合の
  優先度は「最も高いもの」を採用する(実装PR側でこの解釈を明記する)
- **途中失敗(ジョブタイムアウト・インフラ障害)からの復旧**: 決定済み。§4.2の選定ステップに
  `feature/plan-auto-<N>-*` ブランチの存在チェックを追加し、残っていれば「前回実行が完走
  しなかった痕跡」とみなして `needs-human` を付けてskipする(自動削除・自動再作成はしない。
  §4.3「途中失敗時の扱い」参照)。**選定ステップの再選定条件は「ブランチが存在しないこと」のみ**
  であり `needs-human` の有無は条件に含めない(`needs-human` を外し忘れてもブランチさえ
  削除されていれば次回実行時に再選定される)。`needs-human` を外す操作は人間向けの運用上の
  推奨(「対応済み」を可視化する)であり、選定ロジック上の必須条件ではない。人間がブランチを
  削除すれば、`needs-human` の付け外しに関わらず次回実行時に再度候補になる
- **人間の手動`/plan`との競合**: 決定済み(許容する)。scheduled plannerが処理中のIssueに
  人間が同じタイミングで手動 `/plan` を実行する競合は、重複起票防止の仕組みが「実行完了後の
  痕跡」しか検出できないため理論上は防げない。発生確率が低く、最悪でもPlan PRが2本並行して
  開くだけでデータ破壊等の実害はないため、追加のロック機構は設けない
  (`06_adr/09_scheduled_plan_drafting.md` §2)
- **BLOCKED再発時のコメント運用**: 決定済み。1回目・2回目以降の質問いずれも新規コメントは
  作らず、同一の `<!-- swiss-stage-ai-planner-question -->` sticky commentを更新(PATCH)し
  続ける(他の全エージェントと同じsticky comment運用パターン)
- **git push時のpermission-mode「ask」**: 決定済み(§4.2に追記)。`main` の
  `.claude/settings.json` はgit pushがask設定のため、無人実行では常に拒否扱いになり
  黙って失敗する(`ai-review.yml` のFixerステップで実際に発生・対処済みの事故と同種)。
  `scheduled-planner.yml` でも `--permission-mode bypassPermissions` を付けることで対処する。
  AskUserQuestion相当の不明点確認(BLOCKED分類)とは別レイヤーの問題であり、混同しないこと
- **`needs-human`ラベルをIssue単位で使う初のケース**: これまで `needs-human` は
  reviewer/fixer/qa/qa-fixer/ci-fixer(`11_cicd_design.md` §2.5等)によりPR単位でのみ運用
  されてきたが、本プランはIssue単位で同じラベルを使う。現時点でIssueに `needs-human` を
  付ける自動化は `scheduled-planner.yml` のみのため、PLANNED完了時に「付いていれば外す」
  としても他要因のラベルを誤って解除する実害はない。将来、他の自動化がIssueに
  `needs-human` を付けるようになった場合は、`scheduled-planner.yml` 側で無条件に外して
  よいか再検討が必要(現時点ではスコープ外の留意事項として記録するに留める)
- **ADR/プランの連番衝突**: 決定済み(許容する)。ADR・プランのファイル名は「既存の最大値+1」で
  採番する(`04_development_process.md` §4・§5)ため、scheduled plannerが週次でPlan PR
  (場合によりADRも)を作成している最中に、別のIssueへ人間が並行して手動 `/plan` を実行すると、
  両者が同じ「次の連番」を計算し同一番号のファイルを異なるPRで作ってしまう可能性がある。この
  場合 `ci.yml` の docs-lint(連番規約検査・必須ゲート)が重複をCI失敗として機械的に検出する
  ため、後からマージする側は連番を振り直してpushし直すだけで済み、正確性上の実害(異なる決定が
  同じ番号で共存してしまう等)はない。発生頻度も低いため、採番方式の見直し(例:
  Issue番号を連番の一部に含める等)は行わず、docs-lintによる検出とリベースで十分と判断する
- **`backlog`ラベルの扱い**: PLANNED完了時に外すのは `auto-plan:P*`(・付いていれば
  `needs-human`)のみで、`backlog`(対応時期未定)には触れない。人間向けの既存`/plan`も
  `backlog`を操作しないため挙動を合わせている。Plan PR作成後も対応時期の見直し(backlogのまま
  でよいか)は人間がApprove時に判断する
- **BLOCKED再開判定が拾うコメントの範囲**: 「最新コメントがsticky comment自身か」でのみ
  判定するため、理論上は人間以外(将来の別の自動化)がsticky comment更新以外の形でIssueに
  新規コメントを追加した場合も「回答あり」と誤検出しうる。現時点でIssueにコメントを投稿する
  自動化は `scheduled-planner.yml`(sticky commentの更新のみ)しかなく、実質的にコメントの
  追加主体は人間のみのため許容する。将来Issueにコメントする別の自動化(bot等)を追加する場合は、
  この誤検出リスクを再検討すること
