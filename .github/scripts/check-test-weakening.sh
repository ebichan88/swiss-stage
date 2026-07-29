#!/usr/bin/env bash
#
# テスト弱体化ガード。
#
# AIによる自動修正が「失敗を消す」最短経路(テストの削除・無効化・アサーション削除)を
# 選んでいないかを機械的に検査する。AIレビューのFixer・CI自動修正の安全装置であり、
# これが機能していることを前提に自動修正の範囲を広げる(11_cicd_design.md §2.5)。
#
# 使い方:
#   check-test-weakening.sh <mode> <before-ref> <after-ref>
#
#   mode        strict … 全項目を検査する(AI修正コミットに適用)
#               light  … BLOCK相当の項目だけ検査する(PR全体に適用)
#   before-ref  比較元(例: <sha>^ / merge-base)
#   after-ref   比較先(例: <sha> / HEAD)
#
# 終了コード:
#   0  違反なし
#   1  BLOCK   … テストの無効化 / 受け入れケースID付きテストの削除
#   2  ESCALATE… アサーションの純減 / 実装削除を伴わないテスト削除(strictのみ)
#
# BLOCKとESCALATEをどう扱うか(CIを落とすか needs-human に留めるか)は
# 呼び出し側(.github/workflows/guard.yml)が決める。
set -euo pipefail

MODE="${1:-}"
BEFORE="${2:-}"
AFTER="${3:-}"

if [ -z "$MODE" ] || [ -z "$BEFORE" ] || [ -z "$AFTER" ]; then
  echo "usage: $0 <strict|light> <before-ref> <after-ref>" >&2
  exit 64
fi
if [ "$MODE" != "strict" ] && [ "$MODE" != "light" ]; then
  echo "mode は strict または light を指定してください: $MODE" >&2
  exit 64
fi

# テストコードとみなすパス。backend/frontend の両方の規約に合わせる
TEST_PATH_PATTERN='(^backend/src/test/|^frontend/tests/|\.test\.tsx?$|\.spec\.tsx?$)'
# 受け入れケースID(00_acceptance_policy.md の体系)
AC_ID_PATTERN='[A-Z0-9]+-AC-[0-9]+'
# テストを無効化するマーカー。現在リポジトリ内に0件なので「1件でもあればNG」で運用する
DISABLE_MARKER_PATTERN='@Disabled|@Ignore|\.skip\(|\bxit\(|\.only\('
# アサーションとみなす呼び出し
ASSERTION_PATTERN='assertThat|assertEquals|assertTrue|assertFalse|assertNull|assertNotNull|assertThrows|assertAll|expect\('

blocks=()
escalations=()

# --- テストファイルの削除 -----------------------------------------------------
# 実装クラスの廃止に伴う削除は正当なので、削除の「文脈」で判定する:
#   受け入れケースIDを含む       → 常にBLOCK(台帳のStatusと連動するため人間の判断が要る)
#   src/main/ の削除が同一差分内 → 許容(クラス廃止に伴う正当な削除)
#   実装削除がないのにテストだけ → ESCALATE
deleted_files=$(git diff --diff-filter=D --name-only "$BEFORE" "$AFTER" || true)
deleted_tests=$(printf '%s\n' "$deleted_files" | grep -E "$TEST_PATH_PATTERN" || true)

if [ -n "$deleted_tests" ]; then
  impl_deleted=$(printf '%s\n' "$deleted_files" | grep -E '(^backend/src/main/|^frontend/src/)' || true)

  while IFS= read -r f; do
    [ -z "$f" ] && continue
    ac_ids=$(git show "$BEFORE:$f" 2>/dev/null | grep -oE "$AC_ID_PATTERN" | sort -u | tr '\n' ' ' || true)
    if [ -n "$ac_ids" ]; then
      blocks+=("受け入れケースID付きテストの削除: $f (${ac_ids% })
    → 台帳(.claude/05_acceptance/)のStatusと連動するため、削除は人間が判断する")
    elif [ -z "$impl_deleted" ] && [ "$MODE" = "strict" ]; then
      escalations+=("実装削除を伴わないテストの削除: $f
    → 対応する src/main/ の削除が差分に見当たらない")
    fi
  done <<< "$deleted_tests"
fi

# --- テスト無効化マーカーの追加 -----------------------------------------------
# 追加行(^+)のみを見る。既存行の移動で誤検知しないよう、テストパスに限定する
changed_tests=$(git diff --diff-filter=d --name-only "$BEFORE" "$AFTER" \
  | grep -E "$TEST_PATH_PATTERN" || true)

if [ -n "$changed_tests" ]; then
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    added=$(git diff -U0 "$BEFORE" "$AFTER" -- "$f" \
      | grep -E '^\+' | grep -vE '^\+\+\+' \
      | grep -E "$DISABLE_MARKER_PATTERN" || true)
    if [ -n "$added" ]; then
      blocks+=("テスト無効化マーカーの追加: $f
$(printf '%s\n' "$added" | sed 's/^/      /')")
    fi
  done <<< "$changed_tests"
fi

# --- アサーションの純減 -------------------------------------------------------
# 正当なリファクタでも起きうるため、BLOCKではなくESCALATE(人間へのエスカレーション)に留める
if [ "$MODE" = "strict" ] && [ -n "$changed_tests" ]; then
  diff_body=$(git diff -U0 "$BEFORE" "$AFTER" -- $(printf '%s ' $changed_tests) || true)
  added_assertions=$(printf '%s\n' "$diff_body" \
    | grep -E '^\+' | grep -vE '^\+\+\+' | grep -cE "$ASSERTION_PATTERN" || true)
  removed_assertions=$(printf '%s\n' "$diff_body" \
    | grep -E '^-' | grep -vE '^---' | grep -cE "$ASSERTION_PATTERN" || true)

  if [ "$removed_assertions" -gt "$added_assertions" ]; then
    escalations+=("アサーションの純減: 追加 ${added_assertions} 件 / 削除 ${removed_assertions} 件
    → 検証が弱まっていないか人間が確認する")
  fi
fi

# --- 結果出力 -----------------------------------------------------------------
echo "テスト弱体化ガード (mode=$MODE, $BEFORE..$AFTER)"
echo

if [ ${#blocks[@]} -eq 0 ] && [ ${#escalations[@]} -eq 0 ]; then
  echo "違反はありません。"
  exit 0
fi

for b in "${blocks[@]:-}"; do
  [ -z "$b" ] && continue
  echo "  [BLOCK] $b"
  echo
done
for e in "${escalations[@]:-}"; do
  [ -z "$e" ] && continue
  echo "  [ESCALATE] $e"
  echo
done

if [ ${#blocks[@]} -gt 0 ]; then
  exit 1
fi
exit 2
