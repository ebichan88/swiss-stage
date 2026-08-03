---
description: PRのAIレビュー/QA/Plan Reviewの指摘のうち妥当だと判断したものを、このセッションで直接修正する
argument-hint: "[PR番号] [指摘ID...]"
allowed-tools: Bash(gh pr view:*), Bash(gh pr list:*), Bash(gh api:*), Bash(gh repo view:*), Bash(git *), Bash(./gradlew *), Bash(pnpm *), Bash(python3 .github/scripts/docs-lint.py), Read, Grep, Glob, Edit, Write
---

PRに投稿されたAIレビュー(`ai-review.yml`)・AI QA(`ai-qa.yml`)・AI Plan Review(`ai-plan-review.yml`)
の指摘のうち、人間が読んで妥当だと判断したものを、このセッションが直接修正します。
ユーザーからの入力: $ARGUMENTS

**位置づけ**: CIの `fixer`/`qa-fixer`(`.claude/agents/fixer.md`/`qa-fixer.md`)は無人実行のため
対象を機械的な条件で絞っている。このコマンドは「人間が指摘を読んで妥当だと判断した後」に
呼ばれる前提なので、その判断範囲までは踏み込んで修正してよい。ただし正確性・仕様に関わる
聖域はCI版と同じ理由で自動修正しない(5節)。

## 0. 引数の解析

`$ARGUMENTS` を空白区切りで読む。

- **空**: 「PR一覧モード」(1節)
- **数字1つのみ**: `<PR番号>` として「一括適用モード」(2節以降、指摘ID指定なし)
- **数字 + 続くトークン**: 先頭を `<PR番号>`、残りを対象の指摘ID(`C1` `Q2` `PL1` 等。角括弧は付けない)として「指定モード」

## 1. PR一覧モード(PR番号省略時)

`gh pr list --label needs-human --json number,title,url,updatedAt` で一覧を表示して終了する
(ここでは何も修正しない。どのPRを対象にするかは人間が選ぶ)。0件なら「対応待ちのPRはありません」
と伝えて終了する。

## 2. 対象PRのブランチを確認

- `gh pr view <PR番号> --json headRefName -q .headRefName` でPRのブランチ名を取得する
- `git branch --show-current` と一致しない場合、チェックアウトしてよいか確認してから
  `git fetch origin <ブランチ> && git checkout <ブランチ>` する(勝手に切り替えない)

## 3. レポートの取得

対象リポジトリは `gh repo view --json nameWithOwner -q .nameWithOwner` で取得する。
以下3種のsticky commentを、存在するものだけ取得する(例: Plan PRでなければ
`swiss-stage-ai-plan-review` のコメントは無いので無視してよい):

```bash
gh api "repos/<owner/repo>/issues/<PR番号>/comments" --paginate \
  --jq '[.[] | select(.body | startswith("<!-- swiss-stage-ai-review -->"))] | (last // {}) | .body // ""'
```

同様に `swiss-stage-ai-qa`、`swiss-stage-ai-plan-review` のマーカーでも取得する。

## 4. 対象指摘の抽出

各レポートの形式は `.claude/agents/reviewer.md` / `qa.md` / `plan-reviewer.md` の出力形式を参照。

- **指定モード**: 3レポートを横断して、指定された指摘IDを持つ指摘だけを抜き出す。見つからない
  IDはその旨を報告する
- **一括モード**: 以下のみを対象にする(絞る理由は5節と同じく、判断が要らない範囲を安全に
  広げるため)
  - AI Review: `## Critical` `## Major` の全指摘(`## Minor` は対象外。直したい場合はIDを指定する)
  - AI QA: 「## 台帳整合・対応検証・基準hack」のうち **`close: test-side` の指摘のみ**
  - AI Plan Review: 「## 抜け」の全指摘

## 5. 聖域(自動修正しない。指定モードでIDを明示されても同じ)

`.claude/agents/fixer.md` / `qa-fixer.md` の聖域定義と同じ理由・同じ範囲。「指摘の存在を人間が
妥当と判断したか」とは独立に、正確性・仕様の書き換えに関わるため踏み込まない:

- `backend/src/main/java/com/swiss_stage/domain/service/` 配下(マッチング・順位計算)
- `.claude/01_development_docs/05_swiss_pairing_algorithm.md` の変更を伴う修正
- `schema/` 配下(API契約のSSoT)
- `.claude/05_acceptance/**`(受け入れケース台帳)への書き込み。QAの `close: ledger-side` は
  常にこれに該当する
- QAの「基準hack検出」に分類される指摘(テストの実質を弱めた疑いをAI自身が直すと隠蔽になる)
- テストの削除・`@Disabled`/`.skip()`等による無効化・アサーションの弱体化を伴う修正

該当する指摘は常に **SKIPPED** とし、理由を添えて報告する。

## 6. 指摘ごとの判定

`.claude/agents/fixer.md` と同じ4分類を、対象とした指摘1件ずつに適用する:

1. **FIXED**: 指摘が正しく、修正できた
2. **DISPUTED**: 確認した結果、指摘が誤りだと確信した → 修正せず根拠を報告する
3. **SKIPPED**: 聖域(5節)に該当する
4. **FAILED**: 修正を試みたが検証を通せなかった

**このコマンド特有のルール**: 修正方針が一意に決まらない指摘(複数の妥当な直し方がある、新しい
判断が要る内容 ─ 例: ADR新設で「却下した案」を書く必要があるが本文からは分からない、UI文言など
実装者が決めるべき内容が未確定)は、**推測でFIXEDにせず、その場でユーザーに質問してから進める**。
CI版のfixer/qa-fixerは無人実行のため質問できず一律SKIPPEDやDISPUTEDに倒すしかないが、この
コマンドは対話セッションなのでその制約がない。

## 7. 検証

変更したファイルに応じて実行し、失敗したら修正するかFAILEDに倒す:

- `frontend/` を変更した場合: `cd frontend && pnpm run check`
- `backend/` を変更した場合: `cd backend && ./gradlew check`(DynamoDB Localが必要)
- `.claude/07_plans/**` `.claude/06_adr/**` `.claude/05_acceptance/**` を変更した場合:
  `python3 .github/scripts/docs-lint.py`
- 上記以外のドキュメントのみの変更なら検証不要

## 8. コミット

- 対象とした指摘をまとめて1コミットでよい(CI版と違い自動修正ループの回数管理が無いため
  `[ai-fix]`/`[qa-fix]` プレフィックスは不要)
- subject: `<prefix>: <日本語の要約>`(prefixは `/pr` と同じ規約: `fix:` `docs:` `chore:` 等)
- body に対応した指摘を `Applied: <slug>` 形式で1行ずつ列挙する
- 末尾に `Co-Authored-By: Claude <noreply@anthropic.com>`
- **pushはしない**。9節で報告した後、必要ならユーザーに確認してから `git push` する
  (pushするとこのPRのCIが再起動し、AIレビュー/QAが再度回る)

## 9. 報告

`.claude/agents/fixer.md` の出力形式に準じた表をチャット上に出す(PRコメントへの投稿はしない。
投稿は既存のreviewer/qa/plan-reviewer/fixer/qa-fixerの役割のまま変えない):

```markdown
| 指摘 | 結果 | 補足 |
|------|------|------|
| [C1] match-rematch-guard | FIXED | |
| [Q2] orphaned-id-missing-tag | SKIPPED | close: ledger-side のため人間対応 |
```

- DISPUTED / SKIPPED / FAILED が1件でもあれば最終行にその旨を明記する
- 全てFIXEDなら「全指摘を修正しました。pushしてよいか確認してください。」と締める
- 最後に、コミットをpushしてよいか確認する
