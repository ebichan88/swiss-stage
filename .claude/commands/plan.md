---
description: Issueから実装計画を立て、原則コード0行のPlan PRを作成する(承認はPRのマージで行う)
argument-hint: "<Issue番号>"
allowed-tools: Bash(gh issue view:*), Bash(gh issue comment:*), Bash(git *), Bash(gh pr create:*), Bash(gh pr view:*), Bash(python3 .github/scripts/docs-lint.py), Read, Grep, Glob, Write, Edit
---

Issue #$ARGUMENTS から実装計画を立て、Plan PR を作成してください。

`.claude/00_project/04_development_process.md` の「/plan」段階です。**このコマンドでは原則コードを
1行も書かない**(新規画面・大きなレイアウト変更時は対象画面の `.stories.tsx` のみ例外。手順5、
`04_development_process.md` §5.1)。以下の手順に従うこと。

## 1. Issueを読む

`gh issue view $ARGUMENTS` で本文・ラベル・進捗チェックリストを確認する。
種別ラベル(`type:bug/feature/chore`)から、`04_development_process.md` §2 のトリガー表で
Plan PR・ADR・受け入れケースの要否を確認する(Plan PR不要の種別に `/plan` が呼ばれた場合は
その旨を伝えて中断してよい)。

## 2. 既存コードの調査

関連する既存の画面・API・ドメインロジックを調べる。すでにある実装・パターンを再利用できないか
確認する(新しい抽象を安易に作らない)。

## 3. 不明点の確認

AskUserQuestion で**最大3問**にまとめて確認する。優先すべき観点(`03_feature_plan_template.md` §2):

- 画面シナリオの異常系・境界(0件・上限)・競合・権限外アクセス
- 大量データ時の表示、レスポンシブでの違い
- 既存画面との一貫性(似た画面のボタン配置・用語・トーンに合わせるか)

## 4. 計画の作成

`.claude/00_project/03_feature_plan_template.md` の形式で計画を書く。

- 配置先: `.claude/07_plans/NN_<slug>.md`(連番は既存ファイルの最大値+1。2桁ゼロ埋め、
  `.claude/07_plans/` に既存が無ければ `01` から)
- 冒頭ヘッダ: `Status: planned` / `Issue: #$ARGUMENTS` / `PR: -`
- `04_development_process.md` §3 のADR条件に1つでも当てはまる場合は、`.claude/06_adr/NN_<slug>.md`
  を同じ連番規約で作成する(ADR用の連番は `.claude/06_adr/` 内で独立してカウントする)。
  ヘッダは `Status: Proposed`(Plan PR レビュー中のため。マージ後に `/pr` が `Accepted` へ更新する)
  / `Issue: #$ARGUMENTS` / `Date: <今日の日付>`。ADR §2「決定」は仕様を再記述せず「何を決めたか」を
  数行に留め、詳細は同じPRのプランを参照させる(`04_development_process.md` §4)
- API変更があれば `schema/openapi.yaml` を計画と同じPRで更新する。**`.claude/01_development_docs/**`
  配下の設計ドキュメント(`05_swiss_pairing_algorithm.md` 等)は Plan PR では更新しない**。
  マッチング・順位計算の仕様変更やデータモデルの変更方針は計画の「4. 技術設計」に文章で書き、
  実際のドキュメント更新は実装PRで、実装コードと同じPRで行う(`04_development_process.md` §2。
  決定のSSoTを1箇所に保ち、Plan PRと設計ドキュメントの二重記述によるズレを防ぐため)

## 5. 新規画面・大きなレイアウト変更のストーリー作成

UI仕様(§3の観点で計画済み)確定後、対象画面が新規画面、または既存画面の大きなレイアウト変更に
該当する場合は、`src/pages/XxxPage.stories.tsx` を作成する(`04_development_process.md` §5.1、
`03_feature_plan_template.md` §3 の対象範囲。Plan PRの「コード0行」原則の例外)。該当しない場合は
このステップを飛ばす。

- **新規画面(0→1)・既存画面の大きなレイアウト変更のいずれも**: 本物のページ
  (`src/pages/XxxPage.tsx`)が対象の新レイアウトをまだ反映していない(新規画面なら未存在、
  既存画面なら旧レイアウトのまま)ため、ストーリーファイル内にインラインのプレースホルダー実装を
  書く。実装PRで本物のページをこのプレースホルダーに合わせて書き換え、ストーリーを実importへ
  書き換える運用になる(`06_adr/12_story_first_existing_page_placeholder.md`)
- **合意のタイミング**: 新しい承認ステップは発明しない。ストーリー作成後、Plan PRのレビュー・
  マージそのものを「UI合意」のゲートとして扱う(`04_development_process.md` §5.1)

## 6. 受け入れケースの追加

`.claude/05_acceptance/01_acceptance_scope.md` に **Status=todo** でケースを追加する。

- 連番は該当プレフィックスの既存最大値+1(欠番の再利用禁止)
- 新しいプレフィックスが必要な場合は、先に `00_acceptance_policy.md` §2 の表に追記する
- 優先度は `02_severity.md` の判定フローに従う
- 洗い出しには `09_test_strategy.md` §2.5 の技法を使い、境界値の両側と組み合わせの抜けを確認する

## 7. 検証

`python3 .github/scripts/docs-lint.py` を実行し、違反があれば直す(0で終了するまで繰り返す)。

## 8. ブランチ作成とコミット

- `main` から `feature/plan-<slug>` ブランチを作成する
- メッセージ形式: `docs: <日本語の要約>(Plan PR)` + 空行 + 本文
- 末尾に `Co-Authored-By: Claude <noreply@anthropic.com>` を付ける
- **コミット前チェック(`pnpm run check` / `./gradlew check`)は、対象画面の `.stories.tsx` を
  含まない限り不要**(コード変更を含まないため)。手順5の例外で `.stories.tsx` を含む場合は、
  最低限 `pnpm run lint` / `pnpm run type-check` を実行する(フルの `pnpm run check` を実行
  してもよい)。`ci.yml` のfrontendジョブがPlan PRブランチでも自動実行される(`04_development_process.md`
  §5.1)ため必須ではないが、ローカルで先に検知した方が手戻りが小さい

## 9. プッシュとPR作成

- `git push -u origin feature/plan-<slug>`
- `gh pr create` で作成。PR本文の冒頭に `Refs #$ARGUMENTS`(**`Closes` ではない**。実装が
  終わるまで Issue はクローズしない)
- 本文テンプレート:

```markdown
Refs #$ARGUMENTS

## 概要

(この計画が何を決めるか)

## 計画

- `.claude/07_plans/NN_<slug>.md` を参照

## 変更内容

- (追加・更新したファイルの一覧)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

## 10. Issueへの反映

`gh issue comment $ARGUMENTS` で Plan PR のURLをコメントし、進捗チェックリストの
「Plan PR」項目にチェックが入る想定であることを伝える(チェックボックスの実際の更新は
Issue本文の編集操作なので、必要ならユーザーに確認する)。

## 11. 報告

- Plan PR の URL を報告する
- CI で docs-lint / ai-design-review / ai-plan-review(非ゲート)が自動実行されることを添え、
  レポートを確認してから Approve・マージするよう伝える
- マージされたら `/pr` で実装PRに進めることを伝える
