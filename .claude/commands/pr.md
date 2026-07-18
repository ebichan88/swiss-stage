---
description: 現在の変更をコミットしてPRを作成する(コミット前チェック込み)
argument-hint: "[PRタイトルや補足(省略可)]"
allowed-tools: Bash(git *), Bash(gh *), Bash(./gradlew *), Bash(npm *), Read, Grep, Glob
---

現在の作業ツリーの変更から Pull Request を作成してください。ユーザーからの補足: $ARGUMENTS

以下の手順に従うこと:

## 1. 変更内容の把握

- `git status` と `git diff` で変更内容を確認する
- 変更が何もない場合はその旨を伝えて終了する
- 意図しないファイル(ビルド成果物、一時ファイル等)が混ざっていないか確認する

## 2. コミット前チェック(CLAUDE.md 落とし穴 #12)

変更されたディレクトリに応じて必ず実行し、失敗したら修正してから進む:

- `backend/` に変更がある場合: `cd backend && ./gradlew check`
- `frontend/` に変更がある場合: `cd frontend && npm run check`
- ドキュメントのみの変更ならチェック不要

## 3. ドキュメント整合の確認(CLAUDE.md ドキュメント運用ルール)

- 実装と設計ドキュメント(`.claude/01_development_docs/` 等)が乖離する変更なら、同じPRでドキュメントも更新されているか確認する
- 特にマッチング・順位計算の変更は `05_swiss_pairing_algorithm.md` の更新が先行しているべき

## 4. ブランチ作成

- `main` にいる場合は必ずブランチを切る: `git checkout -b <prefix>/<英語の短いスラッグ>`
- prefix は変更内容に応じて選ぶ: `feat/` `fix/` `chore/` `docs/` `refactor/` `test/`
- 既に作業ブランチにいる場合はそのまま使う

## 5. コミット

- メッセージ形式: `<prefix>: <日本語の要約>`(1行目)+ 空行 + 必要なら本文(日本語)
- 末尾に必ず付ける: `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`
- 関連する変更は1コミットにまとめる。無関係な変更が混ざっていたらユーザーに確認する

## 6. プッシュとPR作成

- `git push -u origin <ブランチ名>`
- `gh pr create` で作成。タイトルはコミットメッセージ1行目と揃える
- PR本文は以下のテンプレート(日本語):

```markdown
## 概要

(何を・なぜ。バグ修正なら再現条件とエラー内容も)

## 原因

(バグ修正の場合のみ。根本原因の説明)

## 変更内容

- (箇条書き)

## テスト

- (実行したチェック・テストと結果)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

## 7. 報告

- PR の URL を報告する
- CI で AIレビュー(`.github/workflows/ai-review.yml`)が自動実行されることを添え、レビュー結果を確認してからマージするよう伝える
