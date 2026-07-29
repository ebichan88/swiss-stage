#!/usr/bin/env bash
# check-test-weakening.sh の検証。隔離したscratch gitリポジトリでシナリオを再現する。
set -uo pipefail

# 実行時のカレントディレクトリに依存しないよう、このスクリプトの位置から解決する
SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/check-test-weakening.sh"
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

cd "$WORK"
git init -q .
git config user.email t@example.com
git config user.name t

mkdir -p backend/src/main/java/com/swiss_stage/domain/model
mkdir -p backend/src/test/java/com/swiss_stage/unit/domain
mkdir -p backend/src/test/java/com/swiss_stage/contract
mkdir -p frontend/tests/unit frontend/src

# ---- 初期コミット ----
cat > backend/src/main/java/com/swiss_stage/domain/model/Doomed.java <<'EOF'
public class Doomed {}
EOF
cat > backend/src/test/java/com/swiss_stage/unit/domain/DoomedTest.java <<'EOF'
class DoomedTest {
  @Test void a() { assertThat(1).isEqualTo(1); }
  @Test void b() { assertThat(2).isEqualTo(2); }
}
EOF
cat > backend/src/test/java/com/swiss_stage/contract/TrnContractTest.java <<'EOF'
class TrnContractTest {
  @DisplayName("TRN-AC-003: 大会を作成できる")
  @Test void create() { assertThat(1).isEqualTo(1); }
}
EOF
cat > backend/src/test/java/com/swiss_stage/unit/domain/KeepTest.java <<'EOF'
class KeepTest {
  @Test void a() { assertThat(1).isEqualTo(1); }
  @Test void b() { assertThat(2).isEqualTo(2); }
  @Test void c() { assertThat(3).isEqualTo(3); }
}
EOF
cat > frontend/tests/unit/Sample.test.tsx <<'EOF'
it('renders', () => { expect(1).toBe(1); });
EOF
git add -A && git commit -qm init
BASE=$(git rev-parse HEAD)

run_case() {
  local name="$1" expected="$2" mode="$3"
  set +e
  out=$($SCRIPT "$mode" "$BASE" HEAD 2>&1)
  code=$?
  set -e
  if [ "$code" = "$expected" ]; then
    echo "  ✅ $name (exit=$code, 期待=$expected)"
  else
    echo "  ❌ $name (exit=$code, 期待=$expected)"
    echo "$out" | sed 's/^/       /'
    FAILED=1
  fi
  git reset -q --hard "$BASE"
}

FAILED=0
echo "=== シナリオ検証 ==="

# 1. 変更なし → 0
run_case "変更なし" 0 strict

# 2. 実装クラスの削除に伴うテスト削除(AC-IDなし) → 許容 = 0  ★ユーザー指摘のケース
git rm -q backend/src/main/java/com/swiss_stage/domain/model/Doomed.java
git rm -q backend/src/test/java/com/swiss_stage/unit/domain/DoomedTest.java
git commit -qm "refactor: Doomedクラスを廃止"
run_case "実装削除に伴うテスト削除(正当)" 0 strict

# 3. テストだけ削除(実装削除なし) → ESCALATE = 2
git rm -q backend/src/test/java/com/swiss_stage/unit/domain/DoomedTest.java
git commit -qm "[ai-fix] テストを削除"
run_case "テストだけ削除" 2 strict

# 4. AC-ID付きテストの削除(実装削除あり) → BLOCK = 1
git rm -q backend/src/main/java/com/swiss_stage/domain/model/Doomed.java
git rm -q backend/src/test/java/com/swiss_stage/contract/TrnContractTest.java
git commit -qm "[ai-fix] contractテストを削除"
run_case "AC-ID付きテストの削除" 1 strict

# 5. @Disabled の追加 → BLOCK = 1
sed -i 's/class KeepTest {/class KeepTest {\n  @Disabled("flaky")/' backend/src/test/java/com/swiss_stage/unit/domain/KeepTest.java
git commit -qam "[ai-fix] テストを無効化"
run_case "@Disabled の追加" 1 strict

# 6. it.skip の追加(frontend) → BLOCK = 1
echo "it.skip('broken', () => { expect(1).toBe(2); });" >> frontend/tests/unit/Sample.test.tsx
git commit -qam "[ai-fix] skipを追加"
run_case "it.skip の追加" 1 strict

# 7. アサーションの純減 → ESCALATE = 2
cat > backend/src/test/java/com/swiss_stage/unit/domain/KeepTest.java <<'EOF'
class KeepTest {
  @Test void a() { assertThat(1).isEqualTo(1); }
}
EOF
git commit -qam "[ai-fix] アサーションを削減"
run_case "アサーションの純減" 2 strict

# 8. アサーションの純減 + light mode → 検査対象外 = 0
cat > backend/src/test/java/com/swiss_stage/unit/domain/KeepTest.java <<'EOF'
class KeepTest {
  @Test void a() { assertThat(1).isEqualTo(1); }
}
EOF
git commit -qam "refactor: テストを整理"
run_case "アサーション純減(lightでは対象外)" 0 light

# 9. テストだけ削除 + light mode → 検査対象外 = 0
git rm -q backend/src/test/java/com/swiss_stage/unit/domain/KeepTest.java
git commit -qm "chore: テスト削除"
run_case "テストだけ削除(lightでは対象外)" 0 light

# 10. @Disabled 追加 + light mode → BLOCK = 1(人間のPRでも検知する)
sed -i 's/class KeepTest {/class KeepTest {\n  @Disabled("wip")/' backend/src/test/java/com/swiss_stage/unit/domain/KeepTest.java
git commit -qam "chore: 一時的に無効化"
run_case "@Disabled 追加(lightでも検知)" 1 light

# 11. テストを正しく追加した場合 → 0(誤検知しないこと)
cat >> backend/src/test/java/com/swiss_stage/unit/domain/KeepTest.java <<'EOF'
// 追加テスト
class ExtraTest { @Test void d() { assertThat(4).isEqualTo(4); } }
EOF
git commit -qam "test: テストを追加"
run_case "テスト追加(誤検知なし)" 0 strict

# 12. 実装のみの変更 → 0
echo "// changed" >> backend/src/main/java/com/swiss_stage/domain/model/Doomed.java
git commit -qam "feat: 実装を変更"
run_case "実装のみの変更" 0 strict

echo
[ "$FAILED" = "0" ] && echo "全シナリオ合格" || echo "失敗あり"
exit ${FAILED:-0}
