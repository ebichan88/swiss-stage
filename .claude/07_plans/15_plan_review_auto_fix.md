# 15. plan-reviewer/design-reviewerの自動修正エージェント(plan-fixer)を追加し、MAX_FIX_ATTEMPTSを4に統一する

- Status: in_progress
- Issue: #187
- PR: #189

## 1. 背景・目的

`ai-plan-review.yml`(plan-reviewer)・`ai-design-review.yml`(design-reviewer)はPlan PR
(`.claude/07_plans/**`・`.claude/06_adr/**`)に対してレポートのみを出す非ゲート運用で、対応する
自動修正の仕組みが無い(`.claude/01_development_docs/11_cicd_design.md` §2.11)。既存の3系統
(Reviewer→`fixer`、CI→`ci-fixer`、QA→`qa-fixer`)はいずれも指摘への自動対応ループを持つが、
Plan PR側だけこれが欠けている。

この欠落は本Issueの起点になったPR #186(`.claude/07_plans/14_tournament_collaboration.md`、
大会の共同管理・招待リンク機能のPlan PR)で実際にコストとして表面化した。Plan PRの内容が固まる
までにAI Plan Review・AI Design Reviewの指摘に**13ラウンド**、Claude Codeのセッション内で
人間が手動で往復対応する結果になった。個々の修正内容自体は妥当で、ユーザーが並走して確認して
いても違和感はなかったが、ラウンド数が多くなった主因は「指摘された箇所だけを狭く直し、同じ
パターンの他の箇所を毎回スイープしていなかった」ことにある(詳細は
`.claude/06_adr/15_plan_review_auto_fix.md` §1)。

完了時に得られる状態:

- `.claude/agents/plan-fixer.md` が新設され、plan-reviewer/design-reviewerの指摘のうち機械的な
  パターンスイープで閉じられるものを自動修正・コミットできる
- plan-fixerの指示に「指摘箇所だけでなく原因パターンを関連ファイル全体から洗い出してから直す」
  ことが明示され、往復ラウンド数が今回のような二桁に達しにくくなる
- 既存3系統とplan-fixerの `MAX_FIX_ATTEMPTS` が4に統一される

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**。plan-fixerは開発プロセス自体の
自動化であり、大会運営プラットフォームの機能(マッチング・順位計算)には一切触れない。

## 2. 画面シナリオ

対象外。このプランは開発プロセス(CI・自動化)自体の変更であり、エンドユーザー(大会運営者・
参加者)向けの画面変更を伴わない(`03_feature_plan_template.md` 冒頭の対象外規定)。

## 3. UI仕様

該当なし(§2と同じ理由)。

## 4. 技術設計

このプラン(`07_plans/15_plan_review_auto_fix.md`)・ADR(`06_adr/15_plan_review_auto_fix.md`)
に技術設計を書く。`.claude/01_development_docs/11_cicd_design.md` 等の設計ドキュメント本体は
実装PRで、実装(ワークフローファイル・エージェント定義)と同じPRで更新する(§6)。

### 4.1 `.claude/agents/plan-fixer.md`(新規)

`fixer.md`/`qa-fixer.md` と同じ4分類(FIXED/DISPUTED/SKIPPED/FAILED)を採用する。1エージェントを
`ai-plan-review.yml`(呼び出し元が「Plan Reviewレポートの `### 要対応`」を渡す)・
`ai-design-review.yml`(同様に「Design Reviewレポートの `### 要対応`」を渡す)の両方が共用する。
どちらの呼び出しでも、対応する差分を作る際は同じPR内のもう一方のレポートも参照し、同じ原因の
パターンが両方に指摘されていないか確認してよい(ただし「自分の担当分」として結果表に載せるのは
呼び出し元が渡した対象レポートの指摘のみ)。

**核となる指示**(往復削減の主目的): 指摘された箇所だけを直すのではなく、指摘の原因になっている
パターン(用語・節番号・エラーコード名等)を `grep` 等で関連ファイル全体から洗い出してから、
同じパターンの他の箇所もまとめて直す。

**聖域(自動修正禁止領域)**。既存3系統の聖域をそのまま踏襲せず、Plan PRの実態に合わせて
再定義する(判断の詳細・却下案は `06_adr/15_plan_review_auto_fix.md` §2/§3):

| 対象 | 扱い |
|---|---|
| `backend/src/main/java/com/swiss_stage/domain/service/` | 常に聖域(Plan PRでは通常触れないが念のため) |
| `.claude/01_development_docs/05_swiss_pairing_algorithm.md` | 常に聖域 |
| `.github/workflows/**` | 常に聖域(`fixer.md` と同じ技術的制約。Claude GitHub Appの個別トークンがworkflowsへのpush権限を持たない) |
| `schema/openapi.yaml` | **許可**(Plan PRの正規スコープ。API契約の起草そのものが目的のファイル) |
| `.claude/05_acceptance/01_acceptance_scope.md` | **許可、ただしこのPlan PRが新規追加・調整した行に限る**。他機能の既存行のStatus・内容変更は禁止。この許可は `04_development_process.md` §2・`00_acceptance_policy.md` §7-3「AIエージェントは指摘を閉じる目的で台帳を書き換えてはならない」の無条件原則に対する**明示的な例外**であり、qa-fixerの `close: test-side` 限定(§7.5)と同じ形式で `00_acceptance_policy.md` に例外条項を追記する(§6、理由は `06_adr/15_plan_review_auto_fix.md` §2) |
| `.claude/01_development_docs/**`(`05_swiss_pairing_algorithm.md`を除く)・`CLAUDE.md` | **原則聖域**。例外: このPlan PR内に `Status: Accepted` のADR(`04_development_process.md` §4「Plan PR内でAcceptedにする場合」)が存在し、そのADRの決定に関する**用語・節番号などの機械的な同期に限る**場合のみ許可(新しい仕様判断を伴う編集は常にSKIPPED) |
| `frontend/src/types/generated/api.d.ts` | `schema/openapi.yaml` を修正した場合、`pnpm run generate:api` の出力のみ許可(手編集禁止。`06_adr/14_plan_pr_generated_types_exception.md` と同じ制約) |
| `src/pages/XxxPage.stories.tsx` | このPlan PRが対象とする画面のストーリーに限り許可 |

**検証**: 変更内容に応じて `python3 .github/scripts/docs-lint.py`(常時)・
`pnpm --package=@redocly/cli@1 dlx redocly lint schema/openapi.yaml` + `pnpm run generate:api`
(schema変更時)・`pnpm run lint` / `pnpm run type-check`(`.stories.tsx` 変更時)を実行する。
`domain/service` を触らないため `./gradlew check` は不要(バックエンドコードは対象外)。

**コミット規約**: subject prefix `[plan-fix]`(新設。試行回数カウントに使う)。

### 4.2 ワークフロー変更

`ai-plan-review.yml`・`ai-design-review.yml` はそれぞれ独立に、`ai-review.yml` と同じ
「レビュー → ゲート(bash・決定的) → plan-fixer → 再レビュー」を自己完結させる(2ワークフロー間の
明示的な調整は行わない。却下案は ADR §3)。

- **起動条件**: plan-fixerは **`feature/plan-*` ブランチに限定**して起動する
  (`startsWith(github.head_ref, 'feature/plan-')`)。`ai-design-review.yml` は実装PRでも
  起動する広いトリガーを持つため、Plan PR以外のブランチでは既存の非ゲート・自動修正なしの運用を
  変えない
- **`needs-human` 解除時の再開**: `ai-review.yml`/`ai-qa.yml` と同じく、`on.pull_request.types`
  に `opened, synchronize, ready_for_review` に加えて **`unlabeled`** を追加する。
  `needs-human` ラベルを人間が外した際に自動ループ(plan-fixer含む)を再開するためで、
  `unlabeled` イベントは `needs-human` の除去以外では発火させないガード条件
  (`github.event.action != 'unlabeled' || github.event.label.name == 'needs-human'`)も
  既存2系統と同じ形で適用する。これを入れないと、`needs-human` を人間が外しても新しいpushが
  無い限りワークフローが再起動せず既存2系統との挙動パリティが崩れる
- **ゲート判定**(`ai-review.yml`と同じ思想。bashで決定的に判定): 対象レポートの
  `VERDICT: FAIL` かつ `### 要対応` を抽出できる かつ 聖域に該当しない かつ
  `[plan-fix]` コミット数が `MAX_FIX_ATTEMPTS` 未満 かつ 過去に修正済みの指摘が再指摘されていない
  → plan-fixerを起動。いずれか満たさなければ `needs-human` ラベル + 理由コメント
- **試行回数の共有**: `[plan-fix]` コミットは両ワークフローが同じprefixを使うため、
  `git log` ベースのカウントは自然に合算される。追加の調整インフラなしに
  「このPlan PR全体で `[plan-fix]` は合計4回まで」という単一予算になる
- **fail-closed**: レポートが見つからない(`UNKNOWN`)場合、既存3系統と同じ原則で
  `needs-human` + CIジョブ失敗にする(該当ワークフローファイル自体を変更したPRのみ回避不能な
  ワークフロー検証スキップとして扱い、CIは落とさない)
- **非ゲート運用は維持**: `VERDICT: FAIL` そのものはCIジョブを失敗させない(Plan PRのマージ
  判断は常に人間が行う、`04_development_process.md` §1)。fail-closedはあくまで「レビューが
  実施されたか」の検証であり、「FAILがマージをブロックするか」とは別の話
- **権限**: 現行の `contents: read` から `contents: write`(plan-fixerのpush)・
  `issues: write`(needs-humanラベル)を追加する(`pull-requests: write`・`id-token: write`は
  現行のまま)
- **セットアップ**: Node/pnpm(redocly lint・generate:api・lint/type-check用)のみ追加する。
  `domain/service` を触らないため、Java/DynamoDB Localのセットアップ(fixer/qa-fixer/ci-fixerが
  持つ)は不要

### 4.3 `MAX_FIX_ATTEMPTS` の統一

4系統すべてを `4` に統一する。カウント方法(コミット数)は変更しない:

| ワークフロー | 現状 | 変更後 | 数え方 |
|---|---|---|---|
| `ai-review.yml`(Fixer) | 3 | 4 | `[ai-fix]` コミット数 |
| `ci.yml`(autofix決定論的修正 + ci-fixer) | 2 | 4 | `[ci-fix]` コミット数(決定論的修正+ci-fixer合算、既存の数え方のまま) |
| `ai-qa.yml`(qa-fixer) | 2 | 4 | `[qa-fix]` コミット数 |
| `ai-plan-review.yml` / `ai-design-review.yml`(plan-fixer、新設) | — | 4 | `[plan-fix]` コミット数(両ワークフロー合算) |

`ci.yml` の「決定論的修正1回→まだ失敗→ci-fixer1回の最大2回で打ち切り」という説明文言は、
4回まで許容するよう実装PRで書き換える(数え方自体は変えない。ユーザー確認済み: 「ci.ymlは
決定論的修正を含めて4回でいい」)。

### 4.4 テスト弱体化ガードへの追加

`.github/workflows/guard.yml` の対象コミットprefix正規表現に `[plan-fix]` を追加する
(`qa-fixer` と同じ、念のための二重防御。plan-fixerはテストファイルを触らない設計のため
実質的に発火しない想定だが、他の3系統と同じ扱いに揃える)。

### 4.5 `/apply-review` との役割分担

既存のfixer/qa-fixerと `/apply-review` の関係をそのまま踏襲する: plan-fixerはCIのゲート判定
による機械的な範囲のみを無人で対応し、それ以外(SKIPPED/DISPUTEDに倒された指摘、聖域越えの
指摘、設計判断を伴う指摘)は人間が読んで妥当と判断した後に `/apply-review` で対応する。
`/apply-review` は既にPlan Review(`PL*`)・Design Review(`D*`)の指摘IDを読み取り・対応する
経路を持つため(`.claude/commands/apply-review.md` §3/§4)、この読み取り経路自体の変更は不要。

ただし `/apply-review` §5の聖域定義は `schema/` 配下・`.claude/05_acceptance/**` をPR種別を
問わず一律で自動修正禁止としており、§4.1で定めたplan-fixerの聖域再定義(Plan PRの正規スコープ
内は許可)と非対称になる。この非対称を放置すると、CIのplan-fixerが自動修正できる指摘を、
人間が明示的に妥当と判断して `/apply-review` で対応しようとしても規約上SKIPPEDになる逆転が
起きる。実装PRで `/apply-review` §5に「`feature/plan-*` ブランチに限り、`schema/`・
`.claude/05_acceptance/01_acceptance_scope.md`(このPR導入行に限る)はplan-fixerと同じ条件で
聖域から除外する」旨を追記し、対称性を揃える。

### 4.6 設計判断を伴う指摘の扱い(停止条件)

PR #186の「共同管理者を取り消した後も同じ招待リンクで復帰できてしまう」という認可の穴の指摘の
ように、機械的なパターンスイープでは閉じられない指摘が混ざることがある。plan-fixerは、対応する
差分の書き方が一意に決まらない・新しい設計判断が必要だと気づいた指摘は **DISPUTEDではなく
SKIPPED** に分類し、補足に「設計判断を要するため人間対応」と明記する(DISPUTEDは「指摘が誤り
だと確信した」場合専用のまま変えない)。これにより、needs-humanラベルが付き、人間が
`/apply-review` で対応する経路に自然に流れる。

## 5. 受け入れケース

該当なし。このプランは開発プロセスの自動化であり、既存の受け入れケース体系
(`.claude/05_acceptance/00_acceptance_policy.md`)が対象とする「大会運営プラットフォームの機能」
ではない(`07_plans/06_scheduled_plan_drafting.md` §5と同じ扱い)。プロセスの実効性は
§7 DoDの検証手順で確認する。

## 6. 更新する資料

### Plan PRで更新するもの

- [x] `.claude/06_adr/15_plan_review_auto_fix.md`(このPRで新規作成)
- [x] `.claude/07_plans/15_plan_review_auto_fix.md`(このファイル)

### 実装PRで更新が必要な設計ドキュメント(今は更新しない・実装時の申し送り)

- [ ] `.claude/agents/plan-fixer.md`(新規作成、§4.1)
- [ ] `.claude/agents/plan-reviewer.md`・`.claude/agents/design-reviewer.md`(末尾「重要な原則」の
      「自動修正との連携もない」という記述を、plan-fixer接続後の実態(非ゲートである点は変わらないが
      自動修正は接続される)に合わせて修正、§4.2)
- [ ] `.github/workflows/ai-plan-review.yml`(gate + plan-fixer + 再レビュー追加、§4.2)
- [ ] `.github/workflows/ai-design-review.yml`(同上、§4.2)
- [ ] `.github/workflows/ai-review.yml`(`MAX_FIX_ATTEMPTS` 3→4、§4.3)
- [ ] `.github/workflows/ci.yml`(`MAX_FIX_ATTEMPTS` 2→4、§4.3)
- [ ] `.github/workflows/ai-qa.yml`(`MAX_FIX_ATTEMPTS` 2→4、§4.3)
- [ ] `.github/workflows/guard.yml`(対象コミットprefixに `[plan-fix]` 追加、§4.4)
- [ ] `.claude/01_development_docs/11_cicd_design.md`(§2.5/§2.7/§2.9の `MAX_FIX_ATTEMPTS` 値・
      説明文言を更新、§2.10/§2.11にplan-fixerの追加を反映、新設小節で§4.1〜§4.6の設計を記述)
- [ ] `.claude/commands/apply-review.md`(§5の聖域定義に、`feature/plan-*` ブランチに限り
      `schema/`・受け入れケース台帳(このPR導入行)をplan-fixerと同じ条件で除外する旨を追記、§4.5)
- [ ] `.claude/05_acceptance/00_acceptance_policy.md`(§7に、qa-fixerの §7.5 と同じ形式で
      plan-fixerの例外条項を追記する: `feature/plan-*` ブランチ上で、そのPlan PRが新規追加・
      調整した行に限り自動修正してよい。他機能の既存行・`ledger-side` 相当の判断には一切触れない、
      §4.1。`04_development_process.md` §2「受け入れケースの追加・変更・廃止は人間のみが判断する」
      の無条件原則との関係を明文化する)
- [ ] `CLAUDE.md`(「AIエージェント連携」節に、**plan-reviewer自体の既存エントリが現状無い分と
      plan-fixerの新規分の両方**を追記する。現状は reviewer/fixer・qa/qa-fixer・ci-fixer・
      design-reviewerの4行のみで、`ai-plan-review.yml` で既にCI連携済みのplan-reviewerが
      未記載のため、他の行と同じ形式でplan-reviewer行を追加した上で、plan-fixer行(CI連携先
      `ai-plan-review.yml`/`ai-design-review.yml`)も追加する、§4.1)

## 7. DoD(完了の定義)

- [ ] `pnpm run check`(frontend)/ `./gradlew check`(backend)が通る(plan-fixerはコードを
      触らないため、既存の検証が壊れていないことの確認)
- [ ] `python3 .github/scripts/docs-lint.py` が通る
- [ ] Plan PRでplan-reviewerの `### 要対応` を意図的に発生させ、plan-fixerが起動し
      `[plan-fix]` コミットを作成、再レビューでPASSになることを確認する
- [ ] 同様にdesign-reviewerの `### 要対応` でも起動・修正・再レビューPASSを確認する
- [ ] `.claude/01_development_docs/05_swiss_pairing_algorithm.md` を対象とする指摘で
      SKIPPED + `needs-human` になることを確認する(聖域の直接検証)
- [ ] `Status: Accepted` なADRを含まないPlan PRで、`.claude/01_development_docs/**` への
      指摘がSKIPPED + `needs-human` になることを確認する(条件付き許可の境界の裏側)
- [ ] `Status: Accepted` なADRを含むPlan PRで、そのADRの用語・節番号の同期を目的とした
      `.claude/01_development_docs/**` への指摘がFIXEDになることを確認する
      (条件付き許可の境界の表側。PR #186の実例の再発防止を直接検証する)
- [ ] `schema/openapi.yaml`・`.claude/05_acceptance/01_acceptance_scope.md`(このPR導入行)
      への指摘がFIXEDになることを確認する(聖域再定義の直接検証)
- [ ] `[plan-fix]` コミット数が `MAX_FIX_ATTEMPTS`(4)に達すると `needs-human` になることを
      確認する。plan-review側とdesign-review側の双方から `[plan-fix]` コミットを作らせ、
      合算でカウントされることも確認する(共有予算の直接検証)
- [ ] 非Plan-PRブランチ(実装PR相当)で `.claude/**` を変更した場合、design-reviewが
      `VERDICT: FAIL` でもplan-fixerが起動しないこと(既存の非ゲート運用が壊れていないこと)を
      確認する
- [ ] `ai-review.yml`・`ci.yml`・`ai-qa.yml` の `MAX_FIX_ATTEMPTS` が4に更新され、`ci.yml` の
      決定論的修正+ci-fixerの数え方(合算)が変わっていないことを確認する
- [ ] `guard.yml` の対象コミットprefixに `[plan-fix]` が追加されていることを確認する
- [ ] `needs-human` が付いた状態で人間がラベルを外すと、新しいpushが無くても
      `unlabeled` イベントでplan-fixerのループが再開することを確認する
      (`ai-review.yml`/`ai-qa.yml` と同じ挙動パリティの直接検証)
- [ ] §6「実装PRで更新が必要な設計ドキュメント」がすべて実装PRで更新されている

## 8. リスク・未確定事項

- **2ワークフロー間のpush競合**: 決定済み(許容する)。`ai-plan-review.yml` と
  `ai-design-review.yml` は互いに無調整で並行動作するため、稀に同時pushによる非fast-forward
  エラーが起きうる。既存のFixer/qa-fixer/ci-fixerも同じリスクを抱えたまま運用されており、
  次のpushイベントで自然に再試行される。追加の調整インフラは設けない(`06_adr/15_plan_review_auto_fix.md` §3)
- **受け入れケース台帳への書き込み範囲の判定はソフトルール**: 「このPlan PRが新規追加・調整した
  行に限る」という制約は、`fixer.md` の `SANCTUARY_PATTERN` のようなパスベースの機械的ゲートでは
  なく、plan-fixer自身の指示への準拠に依存する。実運用で逸脱(他機能の既存行への誤った書き込み)
  が確認されたら、パスベースの機械的ゲート(例: 差分の行番号が既存行と重ならないことを
  ワークフロー側で検査する)への強化を検討する
- **ADR Accepted-in-PR例外の判定精度**: 「このPR内に `Status: Accepted` のADRが存在するか」は
  `git diff` でADRファイルのヘッダを機械的に読み取れるため誤検知のリスクは低いが、「その決定に
  関する機械的な同期に限る」かどうかの判断はplan-fixer自身の指示への準拠に依存する。DoDの
  該当項目で重点的に確認する
