# 01. 上流プロセスの自動化(Issue + Plan PR + ADR)

- Status: in_progress
- Issue: なし(このシリーズ自体が `04_development_process.md` の前身にあたるため、Issueなしで開始した)
- PR: #99(PR1) / このPR(PR2) / PR3(予定)

## 1. 背景・目的

下流工程(実装〜マージ)は reviewer → fixer → qa → qa-fixer → ci-fixer と guard.yml / docs-lint で
自律的に品質を維持できている。一方、上流(要件 → 仕様検討 → 実装計画)は Plan モードの中だけで
完結しており、成果物がリポジトリに残らない。

- 検討結果は `~/.claude/plans/` に溜まるが、リポジトリ外・PRから辿れない・grepできない。
- 「なぜその設計にしたか」「どの案を却下したか」が失われる。
- 上流の品質ゲートが「人間の承認」1段しかない。
- 課題を認識していても着手時期が未定なものを置く場所がない。

決定の経緯と却下案は `.claude/06_adr/01_upstream_process_artifacts.md` に記録している
(このプランはADRの「決定」を実行計画に落としたもの)。

**目指す状態**: 人間の関与を「要件を書く」「質問に答える」「Plan PR と実装PR を Approve する」の
3点に絞り、その過程で生まれた判断がすべて git と Issue に残る。

## 2. 画面シナリオ

このプランは開発プロセス自体の変更であり、エンドユーザー(大会運営者・参加者)向けの画面変更を
伴わない。対象外(`03_feature_plan_template.md` 冒頭の対象外規定)。

## 3. UI仕様

該当なし(§2と同じ理由)。

## 4. 技術設計

3PRのスタックで実施する。シリーズ名「上流プロセス自動化」。底からマージし、下流PRへの取り込みは
rebase でなく merge を使う(`.claude/commands/pr.md` §8 の既存ルールに従う)。

### PR1(#99・マージ待ち)— Issue基盤とプロセスの正典

- `.github/ISSUE_TEMPLATE/`: bug.yml / feature.yml / chore.yml / config.yml
- `.claude/00_project/04_development_process.md`(新規): 開発プロセスの正典
- `CLAUDE.md`: ドキュメントガイド・クイックリファレンス・運用ルールへの参照追加
- ラベル7件(`type:bug/feature/chore`, `priority:P0/P1/P2`, `backlog`)

### PR2(このPR)— ADR・プラン基盤と docs-lint 拡張

- `.claude/06_adr/01_upstream_process_artifacts.md`(ADR第1号。このプロセス変更自体)
- `.claude/07_plans/01_upstream_process_automation.md`(このファイル)
- `.claude/00_project/03_feature_plan_template.md` の更新(Plan PRでの利用を前提にした文言に改める)
- `.github/scripts/docs-lint.py` に ADR/プランのヘッダ・連番・命名規約の検査を追加

### PR3(予定)— plan-reviewer とコマンド

- `.claude/agents/plan-reviewer.md` + `.github/workflows/ai-plan-review.yml`(非ゲート)
- `.claude/commands/issue.md`(`/issue`)・`.claude/commands/plan.md`(`/plan`)
- `.claude/commands/pr.md` の3箇所追記(§3 台帳・プランStatus更新、§6 `Closes #N`、§7 報告)
- `.claude/01_development_docs/11_cicd_design.md` §1.5 と新設 §2.11
- `.claude/04_quality/01_review_checklist.md` の機械検査済み項目表を更新

## 5. 受け入れケース

該当なし。このプランはプロセス変更であり、既存の受け入れケース体系(`00_acceptance_policy.md`)が
対象とする「大会運営プラットフォームの機能」ではない。プロセスの実効性は各PRの検証手順(§7)で確認する。

## 6. 更新する設計資料

- [x] `.claude/00_project/04_development_process.md`(PR1で新規作成)
- [x] `CLAUDE.md`(PR1で参照追加)
- [x] `.claude/06_adr/01_upstream_process_artifacts.md`(このPRで新規作成)
- [x] `.claude/00_project/03_feature_plan_template.md`(このPRで更新)
- [ ] `.claude/agents/plan-reviewer.md`(PR3)
- [ ] `.claude/01_development_docs/11_cicd_design.md`(PR3)
- [ ] `.claude/04_quality/01_review_checklist.md`(PR3)

## 7. DoD(完了の定義)

- [x] 各PRで `python3 .github/scripts/docs-lint.py` が通る
- [ ] PR2: docs-lintの新規検査をわざと壊して効くことを確認済み(ADRのStatus欠落・プランのdone+PR空欄・NN_規約違反・Supersededの参照切れ)
- [ ] PR3: `/issue` → `/plan` → Approve & マージ → `/pr` を実案件で1周させ、Issueに Plan PR と実装PRが両方ぶら下がることを確認する
- [ ] 3PRすべてマージ後、このプランの Status を `done` に更新し PR番号を確定する
