---
description: 課題・要望からGitHub Issueを作成する(分類テンプレート・優先度ラベル込み)
argument-hint: "[課題や要望の説明]"
allowed-tools: Bash(gh issue create:*), Bash(gh issue edit:*), Bash(gh issue view:*), Bash(gh label list:*), Read, Grep, Glob
---

ユーザーからの課題・要望の説明を Issue にしてください。ユーザーからの入力: $ARGUMENTS

`.claude/00_project/04_development_process.md` に定義された開発プロセスの入口です。
以下の手順に従うこと。

## 1. 分類の判定

`.github/ISSUE_TEMPLATE/` の3種類から最も近いものを選ぶ:

- **bug.yml**: 動作が仕様と異なる、エラーが出る、結果が正しくない
- **feature.yml**: 新しい画面・ユースケースの追加、既存の挙動やレイアウトの変更
- **chore.yml**: 利用者から見た挙動を変えない改善(内部構造・依存更新・CI・ドキュメント)

判断に迷う場合(例: 挙動が変わるのかどうか曖昧)は、後述の AskUserQuestion で確認する。

## 2. 優先度の判定

`.claude/04_quality/02_severity.md` の判定フローに従い、P0/P1/P2を判定する。
根拠(「大会当日に運営が止まるか」等)を必ず述べる。

## 3. 対応時期の確認

ユーザーの説明から「今すぐ」「次の大会まで」「未定(backlog)」のどれかを判断する。
説明に含まれていなければ AskUserQuestion で確認する。

## 4. 不明点の確認

次のような情報が説明から読み取れない場合、AskUserQuestion で**最大3問**にまとめて確認する:

- (bug) 再現手順、期待/実際の挙動、影響範囲(運営者/参加者/両方)
- (feature) 背景・課題、主な利用者、完了時に得られる状態
- (chore) 対象領域、アーキテクチャ・技術選定の決定を含むか

## 5. Issue作成

選んだテンプレートに沿った本文で `gh issue create --template <bug|feature|chore>.yml` を実行する。
`--title` は `[bug]` `[feature]` `[chore]` のプレフィックスに続けて日本語の短い要約を付ける。

作成後、優先度・対応時期に応じてラベルを追加する(Issue Formsのdropdownはラベルを自動付与
しないため、`gh issue edit <番号> --add-label` で明示的に付ける):

- 優先度: `priority:P0` / `priority:P1` / `priority:P2` のいずれか1つ
- 対応時期が「未定(backlog)」の場合のみ: `backlog`

ラベルが存在しない場合は `04_development_process.md` §7 のコマンドで作成されているはずなので、
`gh label list` で確認し、無ければユーザーに知らせて中断する(勝手に作らない)。

## 6. 報告

- Issue の URL と、付与したラベルを報告する
- 対応時期が「未定(backlog)」なら、ここで作業は完了(Plan PRは作らない)
- それ以外(今すぐ・次の大会まで)で、種別が feature、または chore で
  「アーキテクチャ・技術選定の決定を含む」場合は、**続けて `/plan <issue番号>` を実行するか
  ユーザーに確認する**
- bug、または Plan PR が不要な chore の場合は、実装に進んでよいか確認する
