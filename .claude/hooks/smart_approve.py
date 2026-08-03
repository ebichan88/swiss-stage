#!/usr/bin/env python3
import fnmatch, json, sys, re
from pathlib import Path

def _load_permissions(settings_file):
    if not settings_file.exists():
        return [], []
    with open(settings_file) as f:
        s = json.load(f)
    perms = s.get("permissions", {})
    def extract(rules):
        result = []
        for r in rules:
            if r.startswith("Bash(") and r.endswith(")"):
                p = r[5:-1]
                wildcard = bool(re.search(r'(:\*| \*)$', p))
                p = re.sub(r':\*$', '', p)
                p = re.sub(r' \*$', '', p)
                result.append((p, wildcard))
        return result
    return extract(perms.get("allow", [])), extract(perms.get("deny", []))

def load_patterns():
    # ユーザーのホーム設定とプロジェクト設定(このファイルが属するリポジトリの
    # .claude/settings.json)の両方を読み込み、マージする。プロジェクト側の
    # deny がホーム側の allow によって迂回されないようにするため。
    project_settings = Path(__file__).resolve().parent.parent / "settings.json"
    home_settings = Path.home() / ".claude" / "settings.json"
    allow_patterns, deny_patterns = [], []
    for settings_file in (home_settings, project_settings):
        allow, deny = _load_permissions(settings_file)
        allow_patterns += allow
        deny_patterns += deny
    return allow_patterns, deny_patterns

# 単一の `>`/`>>`/`<` によるファイルリダイレクトやコマンド置換(`$(...)`/バッククォート)
# を検出したら安全側に倒し、自動許可しない
UNSAFE_SHELL_PATTERN = re.compile(r'[><`]|\$\(')

def matches(pattern, wildcard, command):
    command = command.strip()
    if "*" in pattern:
        # 文字列途中に`*`を埋め込んだパターン(例: `repos/*/issues/comments/*`)。
        # 末尾スペース+`*`規約(wildcard=True)は extract() で既にトリム済みの
        # ためここには残らない。fnmatchでコマンド全体を対象にglob解釈する。
        return fnmatch.fnmatchcase(command, pattern)
    if wildcard:
        return command == pattern or command.startswith(pattern + " ")
    return command == pattern

def decompose(cmd):
    # クォートを考慮してパイプ・&&・単一の&(バックグラウンド実行)・;・改行 で分割
    parts, current, in_sq, in_dq = [], [], False, False
    i = 0
    while i < len(cmd):
        c = cmd[i]
        if c == '\\' and not in_sq:
            current.append(c)
            if i + 1 < len(cmd):
                current.append(cmd[i+1]); i += 2
            else:
                # 末尾が \ 単独で終わる場合、次の文字が存在しないため
                # i を進めないと while ループが終了しない(無限ループ)。
                i += 1
            continue
        if c == "'" and not in_dq:
            in_sq = not in_sq
        elif c == '"' and not in_sq:
            in_dq = not in_dq
        elif not in_sq and not in_dq:
            if c == '|' or c == ';' or c == '\n':
                parts.append(''.join(current).strip())
                current = []
                i += 1; continue
            if c == '&':
                # `&&` (論理AND) と単一の `&` (バックグラウンド実行) の両方を
                # ステージ区切りとして扱う。単一&を放置すると
                # `git log & rm -rf /path` のような後続コマンドが
                # 分割されずに許可パターンのstartswithマッチをすり抜ける。
                parts.append(''.join(current).strip())
                current = []
                if i + 1 < len(cmd) and cmd[i+1] == '&':
                    i += 2
                else:
                    i += 1
                continue
        current.append(c); i += 1
    parts.append(''.join(current).strip())
    return [p for p in parts if p]

def main():
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)
    command = data.get("tool_input", {}).get("command", "")
    if not command:
        sys.exit(0)
    allow_patterns, deny_patterns = load_patterns()
    if not allow_patterns:
        sys.exit(0)
    stages = decompose(command)
    for stage in stages:
        # fdの複製(例: 2>&1)のみを安全な冗長表現として除去する。
        # ファイルへのリダイレクト(単一の `>`/`>>`/`<`)はここでは除去しない。
        clean = re.sub(r'\d*>&\d*', '', stage).strip()
        if not clean:
            continue
        if UNSAFE_SHELL_PATTERN.search(clean):
            sys.exit(0)
        if any(matches(p, w, clean) for p, w in deny_patterns):
            sys.exit(0)
        if not any(matches(p, w, clean) for p, w in allow_patterns):
            sys.exit(0)
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "allow",
            "permissionDecisionReason": "All stages matched allow patterns"
        }
    }))

if __name__ == "__main__":
    main()
