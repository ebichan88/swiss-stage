#!/usr/bin/env python3
"""docs-lint: .claude/ 配下のドキュメントに対する機械的な整合性チェック。

Python標準ライブラリのみで動作する(依存追加なし)。Reviewer・QAエージェントが
毎回目視で見ている部分のうち、機械的に判定できるものをここに寄せる
(.claude/04_quality/01_review_checklist.md の運用ルールと同じ発想)。

検査項目:
  1. 受け入れケース台帳(01_acceptance_scope.md)のID整合
     - 形式違反(<PREFIX>-AC-<3桁の数字> から外れるもの)
     - 重複ID
     - 未登録プレフィックス(00_acceptance_policy.md §2の表にないprefix)
     - (informational・非エラー) プレフィックスごとの欠番。永久欠番は仕様上
       正当(00_acceptance_policy.md §3)なため、これは失敗要因にしない
  2. 台帳ID ↔ テストID の双方向突合(00_acceptance_policy.md §6と同じ抽出コマンド)
     - Status=doneのP0/P1ケースがテストに存在しない(宙に浮いた台帳ID)
     - テストに書かれたIDが台帳に存在しない(宙に浮いたテストID)
  3. .claude/** と CLAUDE.md 内のファイル参照切れ
     - この リポジトリの規約はMarkdownリンク`[text](path)`ではなく、
       バッククォート引用された `NN_xxx.md` 形式のファイル名参照(実測して確認済み)。
       `04_quality/01_review_checklist.md` のようにディレクトリプレフィックス付きで
       引用されることもあるため、両方の形式を認識する(プレフィックスの有無に
       かかわらず、実在確認は basename の一致で行う。.claude/ 配下に同名ファイルが
       複数存在しないことを実測で確認済み)
     - 参照されたファイル名が .claude/ 配下のどこかに実在するかを確認する

終了コード: 0=違反なし、1=違反あり(hard fail扱いの項目が1件以上)。
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
CLAUDE_DIR = REPO_ROOT / ".claude"
LEDGER_PATH = CLAUDE_DIR / "05_acceptance" / "01_acceptance_scope.md"
POLICY_PATH = CLAUDE_DIR / "05_acceptance" / "00_acceptance_policy.md"
CLAUDE_MD_PATH = REPO_ROOT / "CLAUDE.md"

ID_STRICT = re.compile(r"^([A-Z0-9]+)-AC-(\d+)$")
LEDGER_ID_CELL = re.compile(r"^[A-Z0-9]+-AC-\d+$")
PREFIX_TABLE_ROW = re.compile(r"^\|\s*([A-Z0-9]+)\s*\|")
TEST_ID_PATTERN = re.compile(r"[A-Z0-9]+-AC-[0-9]+")
# ディレクトリプレフィックス付き参照(例: `04_quality/01_review_checklist.md`)も
# 認識できるよう、任意のパスプレフィックスを許容する。実在確認はbasenameで行う
# (check_file_references参照)
BACKTICK_MD_REF = re.compile(r"`((?:[A-Za-z0-9_.-]+/)*[0-9]{2}_[A-Za-z0-9_]+\.md)`")

TEST_DIRS = [
    REPO_ROOT / "backend/src/test/java/com/swiss_stage/contract",
    REPO_ROOT / "frontend/tests/e2e",
    REPO_ROOT / "frontend/tests/unit",
]


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


# --- 1. 台帳のID整合 -----------------------------------------------------------


def registered_prefixes() -> set[str]:
    """00_acceptance_policy.md §2 の表からプレフィックス一覧を抽出する。"""
    text = read_text(POLICY_PATH)
    in_table = False
    prefixes: set[str] = set()
    for line in text.splitlines():
        if line.startswith("## 2."):
            in_table = True
            continue
        if in_table and line.startswith("## 3."):
            break
        if not in_table:
            continue
        if line.startswith("|----") or line.startswith("| Prefix"):
            continue
        m = PREFIX_TABLE_ROW.match(line)
        if m:
            prefixes.add(m.group(1))
    return prefixes


def parse_ledger_rows() -> list[tuple[str, str, str, int]]:
    """台帳の各行を (ID, priority, status, line_no) のリストで返す。

    台帳は `| ID | P | 受け入れ基準 | Status | 検証 |` の5列(00_acceptance_policy.md §5)。
    1本の正規表現で全列を一致させると `.*` の貪欲マッチで列がずれるため、
    `|` で分割してセル単位に検証する。
    """
    rows = []
    for i, line in enumerate(read_text(LEDGER_PATH).splitlines(), start=1):
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) != 5:
            continue
        id_cell, priority_cell, _criteria, status_cell, _verification = cells
        if not LEDGER_ID_CELL.match(id_cell):
            continue
        if not re.match(r"^P[0-2]$", priority_cell):
            continue
        rows.append((id_cell, priority_cell, status_cell, i))
    return rows


def check_ledger_id_integrity(errors: list[str], notes: list[str]) -> list[tuple[str, str, str, int]]:
    rows = parse_ledger_rows()
    prefixes = registered_prefixes()

    seen: dict[str, int] = {}
    by_prefix: dict[str, list[int]] = {}

    for id_, _priority, _status, line_no in rows:
        m = ID_STRICT.match(id_)
        if not m:
            fail(errors, f"[台帳:{line_no}] ID形式が規約(<PREFIX>-AC-<3桁>)に反しています: {id_}")
            continue
        prefix, num_str = m.group(1), m.group(2)
        format_ok = len(num_str) == 3
        if not format_ok:
            fail(
                errors,
                f"[台帳:{line_no}] 連番が3桁ではありません: {id_}"
                f"(00_acceptance_policy.md §3の形式 <PREFIX>-AC-<3桁の連番> を守る)",
            )
        if prefix not in prefixes:
            fail(
                errors,
                f"[台帳:{line_no}] 未登録のプレフィックスです: {id_}"
                f"(00_acceptance_policy.md §2の表に追記してから使う)",
            )
        if id_ in seen:
            fail(
                errors,
                f"[台帳:{line_no}] IDが重複しています: {id_}(初出は{seen[id_]}行目)",
            )
        else:
            seen[id_] = line_no
        # 欠番計算の母集団は形式が正しいIDのみ(桁数違反は既に上で個別報告済みで、
        # 混在させると欠番リストが桁数違反の数値に引きずられて異常に長くなる)
        if format_ok:
            by_prefix.setdefault(prefix, []).append(int(num_str))

    # 欠番はinformationalのみ(永久欠番は仕様上正当。00_acceptance_policy.md §3)
    for prefix, nums in sorted(by_prefix.items()):
        nums_sorted = sorted(set(nums))
        gaps = [n for n in range(nums_sorted[0], nums_sorted[-1] + 1) if n not in nums_sorted]
        if gaps:
            gap_str = ", ".join(f"{prefix}-AC-{g:03d}" for g in gaps)
            notes.append(f"[情報] {prefix} に欠番があります: {gap_str}(削除済みケースなら正常)")

    return rows


# --- 2. 台帳ID ↔ テストID の双方向突合 -----------------------------------------


def extract_test_ids() -> set[str]:
    ids: set[str] = set()
    for d in TEST_DIRS:
        if not d.exists():
            continue
        for path in d.rglob("*"):
            if not path.is_file():
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError):
                continue
            ids.update(TEST_ID_PATTERN.findall(text))
    return ids


def check_bidirectional_mapping(
    errors: list[str], rows: list[tuple[str, str, str, int]]
) -> None:
    test_ids = extract_test_ids()
    ledger_ids = {id_ for id_, _p, _s, _l in rows}

    # 台帳 -> テスト: Status=done かつ P0/P1 のケースはテストに存在するはず
    for id_, priority, status, line_no in rows:
        if status == "done" and priority in ("P0", "P1") and id_ not in test_ids:
            fail(
                errors,
                f"[台帳:{line_no}] {id_}(Status=done・{priority})に対応するテストが"
                f"見つかりません(contract/E2E/フロントエンドVitestのいずれにも無い)",
            )

    # テスト -> 台帳: テストが名乗るIDは台帳に実在するはず(宙に浮いたID)
    for id_ in sorted(test_ids - ledger_ids):
        fail(
            errors,
            f"[テスト] {id_} という受け入れケースIDがテストに存在しますが、"
            f"台帳(01_acceptance_scope.md)に見つかりません",
        )


# --- 3. .claude/** と CLAUDE.md 内のファイル参照切れ ---------------------------


def check_file_references(errors: list[str]) -> None:
    existing_basenames = {p.name for p in CLAUDE_DIR.rglob("*.md")}
    existing_basenames.add(CLAUDE_MD_PATH.name)

    targets = list(CLAUDE_DIR.rglob("*.md")) + [CLAUDE_MD_PATH]
    for path in targets:
        text = read_text(path)
        rel = path.relative_to(REPO_ROOT)
        for m in BACKTICK_MD_REF.finditer(text):
            ref = m.group(1)
            # ディレクトリプレフィックス付き参照(例: 04_quality/01_review_checklist.md)は
            # basenameだけを見て実在確認する(.claude/配下に同名ファイルが複数存在しない
            # ことを実測で確認済みのため、パスの厳密な一致までは求めない)
            basename = ref.rsplit("/", 1)[-1]
            if basename not in existing_basenames:
                line_no = text.count("\n", 0, m.start()) + 1
                fail(
                    errors,
                    f"[{rel}:{line_no}] 存在しないファイルへの参照です: `{ref}`",
                )


# --- main ------------------------------------------------------------------


def main() -> int:
    errors: list[str] = []
    notes: list[str] = []

    rows = check_ledger_id_integrity(errors, notes)
    check_bidirectional_mapping(errors, rows)
    check_file_references(errors)

    if notes:
        print("--- 情報(失敗要因にしない) ---")
        for n in notes:
            print(f"  {n}")
        print()

    if errors:
        print(f"--- 違反 {len(errors)}件 ---")
        for e in errors:
            print(f"  ✗ {e}")
        return 1

    print("docs-lint: 違反はありません。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
