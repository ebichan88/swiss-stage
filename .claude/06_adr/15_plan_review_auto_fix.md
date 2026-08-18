# 15. plan-reviewer/design-reviewerの自動修正(plan-fixer)を追加し、MAX_FIX_ATTEMPTSを4に統一する

- Status: Accepted
- Issue: #187
- Date: 2026-08-18

## 1. 文脈

既存3系統(Reviewer→`fixer`、CI→`ci-fixer`、QA→`qa-fixer`)はいずれも指摘への自動修正ループを
持つが、Plan PR(`.claude/07_plans/**`・`.claude/06_adr/**`)を検証する`ai-plan-review.yml`
(plan-reviewer)・`ai-design-review.yml`(design-reviewer)は非ゲート・レポートのみで、対応する
自動修正の経路が最初から無い(`.claude/01_development_docs/11_cicd_design.md` §2.11)。

この欠落は実際にコストとして表面化した。本Issueの起点になったPR #186
(`.claude/07_plans/14_tournament_collaboration.md`、大会の共同管理・招待リンク機能のPlan PR)
では、Plan PRの内容が固まるまでにAI Plan Review・AI Design Reviewの指摘に**13ラウンド**
(`git log main..<PR #186のHEAD>` で確認できる `fix: AI Plan Review...` 系コミット)、
Claude Codeのセッション内で人間が手動で往復対応する結果になった。個々の修正内容自体は妥当
だったが、ラウンド数が多くなった主因は「指摘された箇所だけを狭く直し、同じパターンの他の箇所を
毎回スイープしていなかった」ことにある。例:

- `04_development_process.md` の節番号を `§5.1` → `§5.1.1`/`§5.1.2` に再構成した際、参照箇所を
  一部だけ直して次のラウンドで残り(`11_cicd_design.md`・`01_review_checklist.md`・
  `09_test_strategy.md`・`10_frontend_design.md`・`CLAUDE.md`)を指摘された
- OpenAPIスキーマ(`schema/openapi.yaml`)のlintエラー、招待の人数枠の動的上限化など、
  API契約・受け入れケース台帳そのものへの指摘も複数ラウンドにわたって発生した

一方で、今回の指摘には「共同管理者を取り消した後も同じ招待リンクで復帰できてしまう」という
実質的な認可の穴の指摘のように、相応の設計判断を要するものも混ざっていた。機械的なパターン
スイープで閉じられる指摘と、人間の判断が必要な指摘を区別する必要がある。

あわせて、既存3系統の `MAX_FIX_ATTEMPTS`(`ai-review.yml`=3、`ci.yml`/`ai-qa.yml`=2)もバラバラで、
統一の要否が積み残っていた。

## 2. 決定

- `.claude/agents/plan-fixer.md` を新設する。`fixer.md`/`qa-fixer.md` と同じ4分類
  (FIXED/DISPUTED/SKIPPED/FAILED)を、`ai-plan-review.yml`(`## 抜け` の `### 要対応`)・
  `ai-design-review.yml`(`## 指摘` の `### 要対応`)双方のsticky commentに対して適用する
  **1エージェントを両ワークフローが共用する**。指示には「指摘箇所だけを直すのではなく、
  原因になっているパターンを関連ファイル全体から`grep`等で洗い出してから直す」ことを明示する
- plan-fixerの起動は **`feature/plan-*` ブランチに限定する**。design-reviewは実装PRでも起動する
  (`.claude/**`広域トリガー)ため、そちらでは既存の非ゲート・自動修正なしの運用を変えない
- **聖域の再定義**(fixer/qa-fixerの聖域をそのまま踏襲しない):
  - `schema/openapi.yaml`・`.claude/05_acceptance/01_acceptance_scope.md`(このPlan PRが
    新規追加・調整した行に限る)は**自動修正を許可する**。Plan PRはそもそもこの2つを起草する
    ことが目的のファイルであり、AIが起草し人間がPRマージで承認する既存の運用の延長とみなせる。
    **既存原則との関係**: `04_development_process.md` §2・`00_acceptance_policy.md` §7-3は
    「受け入れケースの追加・変更・廃止は人間のみが判断し、AIエージェントは指摘を閉じる目的で
    台帳を書き換えてはならない」と無条件に定めており、qa-fixerもこの原則に従い
    `close: ledger-side`(台帳自体の書き換え)には一切触れない(`00_acceptance_policy.md` §7.5)。
    この決定はplan-fixerに限り、qa-fixerが避けている操作を許可する**明示的な例外**であり、
    根拠は「Plan PR自体がまだ人間のマージ承認を得ていない draft であり、plan-fixerが触るのは
    `Status: todo` のままこのPlan PRが新規に導入した行に限られる(＝実装PRで台帳が指す
    `close: ledger-side` の対象、既にマージ済みの他機能の既存行には一切触れない)」という
    構造的な違いにある。この例外を無条件原則のまま黙って踏み越えないよう、`00_acceptance_policy.md`
    §7に qa-fixerの §7.5 と同じ形式でplan-fixerの例外条項を追記する(§6)
  - `.claude/01_development_docs/**`(`05_swiss_pairing_algorithm.md` を除く)・`CLAUDE.md` は
    原則聖域。ただし**このPlan PR内に `Status: Accepted` のADR(`04_development_process.md` §4
    「Plan PR内でAcceptedにする場合」)が存在し、そのADRの決定に関する用語・節番号などの
    機械的な同期に限る場合のみ**許可する(新しい仕様判断を伴う編集は常にSKIPPED)
  - `backend/src/main/java/com/swiss_stage/domain/service/`・
    `.claude/01_development_docs/05_swiss_pairing_algorithm.md`・`.github/workflows/**`は
    既存3系統と同じ理由で常に聖域のまま
- `ai-plan-review.yml` / `ai-design-review.yml` はそれぞれ独立に、`ai-review.yml` と同じ
  「レビュー → ゲート(bash) → plan-fixer → 再レビュー」を自己完結させる。**2ワークフロー間の
  明示的な調整は行わない**(後述の却下案を参照)
- 自動修正コミットのprefixとして `[plan-fix]` を新設し、**両ワークフローで共用する**。
  `git log` ベースの試行回数カウント([plan-fix]コミット数)は両ワークフローが同じ計算式で
  同じ結果を得るため、追加の調整インフラなしに「Plan PR全体で合計4回まで」という単一予算になる
- **`MAX_FIX_ATTEMPTS` を4系統(`ai-review.yml`・`ci.yml`・`ai-qa.yml`・plan-fixer)すべて
  `4` に統一する**。カウント方法(コミット数、`ci.yml` は決定論的修正+AI修正を合算)は変更しない
- `ai-plan-review.yml`/`ai-design-review.yml` の非ゲート運用(`VERDICT: FAIL` でもCIジョブを
  失敗させない・マージ判断は常に人間)は維持する。既存3系統と同じfail-closedの原則
  (レポート欠落=`UNKNOWN` の場合はneeds-human + CI失敗)のみ新たに追加する
- `.github/workflows/guard.yml` の対象コミットprefixに `[plan-fix]` を追加する(`qa-fixer` と
  同じ、念のための二重防御。plan-fixerはテストファイルを触らないため実質的に発火しない想定)

詳細な設計・DoDは同じPRのプラン `.claude/07_plans/15_plan_review_auto_fix.md` を参照する。

## 3. 却下した案

- **案: 3ワークフローを1本の新しい `plan-fixer.yml` に統合し、plan-reviewer・design-reviewerの
  実行もそこに集約する。**
  却下理由: `ai-design-review.yml` は非Plan-PRブランチ(実装PR)でも起動する広いトリガー条件を
  持ち、統合すると責務が混ざる。加えて、統合するとsticky commentへの同時書き込みレースを新たに
  作り込む必要が生じ、既存の自己完結ワークフロー(`ai-review.yml`)パターンから外れる
- **案: 両ワークフローが共有concurrency groupを持ち、片方が完了してからもう片方が実行される
  よう直列化する。**
  却下理由: 異なるワークフローファイル間でconcurrency groupを共有すると、GitHub Actionsの
  concurrency groupはワークフローファイルをまたいでリポジトリ全体でユニークなため、
  `cancel-in-progress` が意図せず互いの実行中ランをキャンセルしてしまう。既存の3系統
  (Fixer/qa-fixer/ci-fixer)も相互に無調整で並行動作しており、稀なpush競合(非fast-forward)は
  次のpushイベントで自然に再試行される運用上許容されたリスクとして扱われている。plan-fixerも
  同じ扱いにする
- **案: `.claude/05_acceptance/01_acceptance_scope.md`・`schema/openapi.yaml` を fixer/qa-fixer
  と同じく常に聖域にする(qa-fixerに完全に揃え、`00_acceptance_policy.md` §7-3の無条件原則を
  一切緩めない)。**
  却下理由: Plan PRはこの2つを起草すること自体が目的のファイルであり、聖域にすると往復削減効果が
  実際の往復履歴(スキーマlintエラー・招待の人数枠の動的上限化)の相当割合をカバーできなくなる。
  ただし §7-3の原則を無条件のまま残すことはできないため、上記「決定」に書いたとおり
  `00_acceptance_policy.md` に明示的な例外条項を追記する対応(qa-fixerの §7.5 と同型)を
  セットで採用する。例外条項を書かずに黙って踏み越える設計は採らない
- **案: `.claude/01_development_docs/**`・`CLAUDE.md` への修正を無条件で許可する。**
  却下理由: Plan PRの「設計ドキュメント本体は実装PRで更新する」という核となる原則
  (`04_development_process.md` §2)を緩めすぎる。ADR Accepted-in-PR例外の範囲に限定することで、
  実例(節番号の波及漏れ)の再発防止と原則維持を両立させる
- **案: `MAX_FIX_ATTEMPTS` をPlan PR系(plan-fixer)とコード系(`ai-review.yml`/`ci.yml`/
  `ai-qa.yml`)で異なる値に保つ(plan-fixerのみ4、他は現状維持)。**
  却下理由(人間判断): 無人で加えられる変更量への懸念より、4系統の運用を単純化する利点を優先した

## 4. 結果

- **得るもの**: Plan PRの往復ラウンドが機械的なパターンスイープで閉じられる分だけ削減される。
  4系統の `MAX_FIX_ATTEMPTS` の数え方・運用が揃い、開発プロセス全体の一貫性が増す
- **引き受けるトレードオフ**: `schema/openapi.yaml`・受け入れケース台帳をplan-fixerが無人で
  書き換える範囲が既存3系統より広がる。ADR Accepted-in-PR例外時のみとはいえ、設計ドキュメント
  (`.claude/01_development_docs/**`)への無人書き込み経路が初めて生まれる
- **撤回条件**: 実運用で誤検知・意図しない書き換えが頻発したら、該当範囲(`schema/openapi.yaml`
  全体の許可、または `01_development_docs/**` への条件付き許可)を聖域に戻す新しいADRを起こす
