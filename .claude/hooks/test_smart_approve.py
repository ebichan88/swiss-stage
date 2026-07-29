#!/usr/bin/env python3
"""smart_approve.py の回帰テスト(標準ライブラリのみで実行可能)。

実行方法: python3 .claude/hooks/test_smart_approve.py
"""
import sys
import threading
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from smart_approve import decompose, matches, UNSAFE_SHELL_PATTERN  # noqa: E402


class DecomposeTests(unittest.TestCase):
    def test_splits_double_ampersand(self):
        self.assertEqual(
            decompose("git status && git diff"),
            ["git status", "git diff"],
        )

    def test_splits_pipe_and_semicolon(self):
        self.assertEqual(
            decompose("git log | head -5; echo done"),
            ["git log", "head -5", "echo done"],
        )

    def test_splits_single_ampersand_background(self):
        # C1: `&` 1つ(バックグラウンド実行)を放置すると、後続コマンドが
        # 1つのステージに混入し、許可パターンのstartswithマッチを
        # すり抜けて自動承認されてしまう。
        self.assertEqual(
            decompose("git log & rm -rf /path"),
            ["git log", "rm -rf /path"],
        )

    def test_splits_raw_newline(self):
        # C1: 生の改行もステージ区切りとして扱う必要がある。
        self.assertEqual(
            decompose("git log\nrm -rf /path"),
            ["git log", "rm -rf /path"],
        )

    def test_quoted_ampersand_is_not_split(self):
        # クォート内の & はシェル演算子ではないので分割しない。
        self.assertEqual(
            decompose('echo "a & b"'),
            ['echo "a & b"'],
        )

    def test_quoted_newline_is_not_split(self):
        self.assertEqual(
            decompose('echo "line1\nline2"'),
            ['echo "line1\nline2"'],
        )

    def test_trailing_backslash_does_not_hang(self):
        # M1: 末尾が \ 単独で終わる入力で i が進まなくなると
        # while ループが無限ループになる(PreToolUseフックが応答不能になる)。
        # 修正が壊れた場合にテストスイート自体がハングしないよう、
        # 別スレッド + join(timeout) で検証する。
        result = {}

        def run():
            result["value"] = decompose("git commit -m foo\\")

        t = threading.Thread(target=run, daemon=True)
        t.start()
        t.join(timeout=2)
        self.assertFalse(t.is_alive(), "decompose() が末尾の \\ で無限ループした")
        self.assertEqual(result["value"], ["git commit -m foo\\"])


class BypassRegressionTests(unittest.TestCase):
    """C1: `& rm -rf` / 埋め込み改行 での自動承認バイパスが
    再発しないことを確認する結合テスト。main() のステージ検証ループを再現する。
    """

    def _would_auto_approve(self, command, allow_patterns, deny_patterns=()):
        stages = decompose(command)
        for stage in stages:
            if UNSAFE_SHELL_PATTERN.search(stage):
                return False
            if any(matches(p, w, stage) for p, w in deny_patterns):
                return False
            if not any(matches(p, w, stage) for p, w in allow_patterns):
                return False
        return True

    def test_ampersand_bypass_is_blocked(self):
        allow = [("git log", True)]  # Bash(git log *) 相当
        self.assertFalse(
            self._would_auto_approve("git log & rm -rf /path", allow)
        )

    def test_newline_bypass_is_blocked(self):
        allow = [("git log", True)]
        self.assertFalse(
            self._would_auto_approve("git log\nrm -rf /path", allow)
        )

    def test_legitimate_double_ampersand_still_approved(self):
        allow = [("git status", True), ("git diff", True)]
        self.assertTrue(
            self._would_auto_approve("git status && git diff", allow)
        )

    def test_echo_append_redirect_is_never_auto_approved(self):
        # M2: `echo ... >> file` はUNSAFE_SHELL_PATTERN(`>`)により
        # 常にブロックされる設計。echo自体を許可リストに入れても
        # リダイレクトを伴う用途は自動承認されない(意図した動作)。
        allow = [("echo", False)]
        self.assertFalse(
            self._would_auto_approve('echo "log line" >> session_log.md', allow)
        )

    def test_pipe_tee_append_is_never_auto_approved(self):
        # M2: `... | tee -a file` も同様にリダイレクト相当のUNSAFE_SHELL_PATTERNには
        # 引っかからないが、decompose()で `tee -a file` ステージに分割された後、
        # `Bash(tee)`(完全一致・引数なし)にはマッチしないため自動承認されない。
        allow = [("git log", True), ("tee", False)]
        self.assertFalse(
            self._would_auto_approve("git log | tee -a session_log.md", allow)
        )

    def test_bare_echo_without_redirect_is_auto_approved(self):
        # `Bash(echo)`(完全一致・引数なし)が許可リストにある場合、
        # 引数もリダイレクトも伴わない bare `echo` のみが自動承認される。
        allow = [("echo", False)]
        self.assertTrue(self._would_auto_approve("echo", allow))


if __name__ == "__main__":
    unittest.main()
