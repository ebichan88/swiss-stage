#!/usr/bin/env bash
set -euo pipefail

# Visual Regression Testをローカルで実行する(09_test_strategy.md)。
#
# ベースライン画像はローカルのネイティブ環境で生成しない
# (WSL2・macOS等とCIランナーでフォントレンダリングが異なり100%差分になるため)。
# 必ずこのスクリプト経由で、CI(.github/workflows/vrt.yml)と同じPlaywrightコンテナから実行する。
#
# コンテナはホストと同じUID/GIDで実行する(rootで実行すると生成物がroot所有になり、
# ホスト側からrm/上書きできなくなるため)。
#
# 使い方:
#   ./scripts/vrt.sh              # 既存ベースラインと比較
#   ./scripts/vrt.sh --update     # ベースラインを更新(意図した変更のときのみ)

cd "$(dirname "$0")/.."

# .github/workflows/vrt.yml のcontainerイメージと同じタグを使う。
# @playwright/test のバージョンを上げたら、このタグも合わせて上げること
PLAYWRIGHT_IMAGE="mcr.microsoft.com/playwright:v1.61.1-noble"

UPDATE_ARGS=()
if [[ "${1:-}" == "--update" ]]; then
  UPDATE_ARGS=(--update-snapshots)
fi

docker run --rm \
  -v "$(pwd):/work" \
  -w /work \
  --ipc=host \
  -e CI=true \
  -e HOME=/tmp \
  -e npm_config_store_dir=/tmp/pnpm-store \
  --user "$(id -u):$(id -g)" \
  "$PLAYWRIGHT_IMAGE" \
  bash -c "mkdir -p /tmp/corepack-bin && corepack enable --install-directory /tmp/corepack-bin && export PATH=/tmp/corepack-bin:\$PATH && pnpm install --frozen-lockfile && pnpm exec playwright test --config=playwright-vrt.config.ts ${UPDATE_ARGS[*]}"
