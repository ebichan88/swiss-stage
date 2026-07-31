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
  4. ADR(.claude/06_adr/)・プラン(.claude/07_plans/)のファイル名規約とヘッダ
     (04_development_process.md §4・§5)
     - ファイル名が `NN_<snake_case>.md` 規約に反していないか(違反すると上記2の
       参照切れ検査からそのファイルが漏れるため、命名自体を規約違反として報告する)
     - 連番の重複(informational・非エラー: 欠番。永久欠番と同じ発想で正当な場合がある)
     - ADR: `Status` / `Issue` / `Date` ヘッダの有無。`Status` が
       `Proposed`/`Accepted`/`Superseded by NN_xxx.md` のいずれかで、Supersededの
       参照先ADRが実在するか
     - プラン: `Status` / `Issue` / `PR` ヘッダの有無。`Status` が
       `planned`/`in_progress`/`done` のいずれかで、`done` なのに `PR` が未記入
       (`-` のまま)になっていないか

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
ADR_DIR = CLAUDE_DIR / "06_adr"
PLAN_DIR = CLAUDE_DIR / "07_plans"

ID_STRICT = re.compile(r"^([A-Z0-9]+)-AC-(\d+)$")
LEDGER_ID_CELL = re.compile(r"^[A-Z0-9]+-AC-\d+$")
PREFIX_TABLE_ROW = re.compile(r"^\|\s*([A-Z0-9]+)\s*\|")
TEST_ID_PATTERN = re.compile(r"[A-Z0-9]+-AC-[0-9]+")
# ディレクトリプレフィックス付き参照(例: `04_quality/01_review_checklist.md`)も
# 認識できるよう、任意のパスプレフィックスを許容する。実在確認はbasenameで行う
# (check_file_references参照)
BACKTICK_MD_REF = re.compile(r"`((?:[A-Za-z0-9_.-]+/)*[0-9]{2}_[A-Za-z0-9_]+\.md)`")

# ADR・プランのファイル名規約(04_development_process.md §4・§5)。
# 2桁の連番で始まらないと BACKTICK_MD_REF にマッチせず参照切れ検査から漏れるため、
# 命名規約そのものを個別に検査する
NN_FILENAME = re.compile(r"^([0-9]{2})_[a-z0-9_]+\.md$")
STATUS_LINE = re.compile(r"^-\s*Status:\s*(.+)$")
ISSUE_LINE = re.compile(r"^-\s*Issue:\s*(.+)$")
DATE_LINE = re.compile(r"^-\s*Date:\s*(.+)$")
PR_LINE = re.compile(r"^-\s*PR:\s*(.+)$")
SUPERSEDED_BY = re.compile(r"^Superseded by ([0-9]{2}_[A-Za-z0-9_]+\.md)$")

ADR_STATUS_VALUES = {"Proposed", "Accepted"}
PLAN_STATUS_VALUES = {"planned", "in_progress", "done"}

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


# --- 4. ADR・プランのファイル名規約とヘッダ ------------------------------------


def _find_header_value(text: str, pattern: re.Pattern[str]) -> str | None:
    for line in text.splitlines():
        m = pattern.match(line.strip())
        if m:
            return m.group(1).strip()
    return None


def check_numbered_docs(
    errors: list[str], notes: list[str], dir_path: Path, kind: str
) -> dict[str, Path]:
    """dir_path配下の *.md をファイル名規約(NN_<snake_case>.md)で検証する。

    連番の重複はerror、欠番はinformational(台帳のID体系と同じ発想: 削除済みの
    ADR・プランがあり得るため、欠番自体は仕様上正当)。
    規約に違反したファイル名は、check_file_referencesの参照切れ検査から
    そのまま漏れてしまうため、ここで個別に報告する。
    """
    if not dir_path.exists():
        return {}

    by_number: dict[str, list[Path]] = {}
    for path in sorted(dir_path.glob("*.md")):
        rel = path.relative_to(REPO_ROOT)
        m = NN_FILENAME.match(path.name)
        if not m:
            fail(
                errors,
                f"[{rel}] ファイル名が規約(NN_<snake_case>.md)に反しています"
                f"(この規約から外れると参照切れ検査の対象外になる)",
            )
            continue
        by_number.setdefault(m.group(1), []).append(path)

    for num, paths in sorted(by_number.items()):
        if len(paths) > 1:
            names = ", ".join(p.name for p in paths)
            fail(errors, f"[{kind}] 連番 {num} が重複しています: {names}")

    valid_nums = sorted(int(n) for n in by_number)
    if valid_nums:
        gaps = [n for n in range(valid_nums[0], valid_nums[-1] + 1) if n not in valid_nums]
        if gaps:
            gap_str = ", ".join(f"{g:02d}" for g in gaps)
            notes.append(f"[情報] {kind} に欠番があります: {gap_str}(削除済みなら正常)")

    return {num: paths[0] for num, paths in by_number.items() if len(paths) == 1}


def check_adr_headers(errors: list[str], adr_files: dict[str, Path]) -> None:
    known_filenames = {p.name for p in adr_files.values()}
    for path in adr_files.values():
        rel = path.relative_to(REPO_ROOT)
        text = read_text(path)

        status = _find_header_value(text, STATUS_LINE)
        if status is None:
            fail(errors, f"[{rel}] `- Status:` ヘッダがありません")
        elif status not in ADR_STATUS_VALUES:
            m = SUPERSEDED_BY.match(status)
            if not m:
                fail(
                    errors,
                    f"[{rel}] Statusの値が不正です: {status}"
                    f"(Proposed / Accepted / Superseded by NN_xxx.md のいずれか)",
                )
            elif m.group(1) not in known_filenames:
                fail(errors, f"[{rel}] Supersededの参照先が存在しません: {m.group(1)}")

        if _find_header_value(text, ISSUE_LINE) is None:
            fail(errors, f"[{rel}] `- Issue:` ヘッダがありません")
        if _find_header_value(text, DATE_LINE) is None:
            fail(errors, f"[{rel}] `- Date:` ヘッダがありません")


def check_plan_headers(errors: list[str], plan_files: dict[str, Path]) -> None:
    for path in plan_files.values():
        rel = path.relative_to(REPO_ROOT)
        text = read_text(path)

        status = _find_header_value(text, STATUS_LINE)
        if status is None:
            fail(errors, f"[{rel}] `- Status:` ヘッダがありません")
        elif status not in PLAN_STATUS_VALUES:
            fail(
                errors,
                f"[{rel}] Statusの値が不正です: {status}"
                f"(planned / in_progress / done のいずれか)",
            )

        if _find_header_value(text, ISSUE_LINE) is None:
            fail(errors, f"[{rel}] `- Issue:` ヘッダがありません")

        pr = _find_header_value(text, PR_LINE)
        if pr is None:
            fail(errors, f"[{rel}] `- PR:` ヘッダがありません")
        elif status == "done" and pr == "-":
            fail(errors, f"[{rel}] Status=done なのに `- PR:` が未記入です(`-` のまま)")


# --- main ------------------------------------------------------------------


def main() -> int:
    errors: list[str] = []
    notes: list[str] = []

    rows = check_ledger_id_integrity(errors, notes)
    check_bidirectional_mapping(errors, rows)
    check_file_references(errors)

    adr_files = check_numbered_docs(errors, notes, ADR_DIR, "ADR")
    check_adr_headers(errors, adr_files)
    plan_files = check_numbered_docs(errors, notes, PLAN_DIR, "プラン")
    check_plan_headers(errors, plan_files)

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
