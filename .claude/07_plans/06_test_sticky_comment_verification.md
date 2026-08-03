# 06. [検証用・マージしない] Issue #105 sticky commentの修正確認(v2)

- Status: planned
- Issue: #105
- PR: -

## 目的

このファイルは実装計画ではない。PR #135(`.claude/settings.json`のask設定が原因で
sticky commentのPATCH更新がCIで無言拒否されていた不具合の修正、main反映済み)が
実際に2回目以降のpushでコメントを更新するかを確認するための使い捨てPR専用ファイル。

検証完了後、このPRはマージせずクローズし、このファイルも残さない。

## 検証手順

1. このファイルを追加してPRを作成する(1回目のpush)
2. `ai-plan-review.yml` / `ai-design-review.yml` / `ai-review.yml` が起動し、sticky commentが新規投稿される
3. 追記コミットをpushする(2回目のpush)
4. 同じsticky commentの `updated_at` が更新されていることを確認する
