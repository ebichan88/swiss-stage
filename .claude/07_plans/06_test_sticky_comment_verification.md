# 06. [検証用・マージしない] Issue #105 sticky commentの修正確認

- Status: planned
- Issue: #105
- PR: -

## 目的

このファイルは実装計画ではない。PR #132(`ai-plan-review.yml` / `ai-design-review.yml`の
sticky comment更新不具合の修正)が実際に2回目以降のpushでコメントを更新するかを、
このリポジトリのCI上で確認するための使い捨てPR専用ファイルである。

検証完了後、このPRはマージせずクローズし、このファイルも残さない。

## 検証手順

1. このファイルを追加してPRを作成する(1回目のpush)
2. `ai-plan-review.yml` / `ai-design-review.yml` が起動し、sticky commentが新規投稿される
3. 追記コミットをpushする(2回目のpush)
4. 同じsticky commentの `updated_at` が更新されていることを確認する
