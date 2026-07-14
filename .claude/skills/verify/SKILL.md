---
name: verify
description: Swiss Stageの変更をローカルで実際に動かして確認する手順(DynamoDB Local + Spring Boot + Vite + Playwright)
---

# Swiss Stage 動作確認手順

## 起動(3プロセス)

```bash
# 1. DynamoDB Local(:8000)+ テーブル
cd backend && docker compose up -d dynamodb-local && ./scripts/create-table.sh

# 2. バックエンド(:8080、localプロファイル)
./gradlew bootRun --args='--spring.profiles.active=local'   # backend/ で。起動ログに "Started SwissStageApplication"

# 3. フロントエンド(:5173、/api→:8080 プロキシ)
cd frontend && npm run dev
```

- ログインは `POST /api/v1/auth/test-login`(local/test限定)。UIからは `/login` の「開発用ログイン」ボタン。
- この環境では `curl` が権限で拒否される。疎通確認はログ(`Started SwissStageApplication` / `VITE ready`)か Playwright で行う。

## UIの駆動(Playwright)

- Playwright は導入済み(`frontend/tests/e2e/`、CP1〜CP4)。実行は backend 起動済みの状態で:
  `LD_LIBRARY_PATH=~/.cache/swiss-stage-e2e-libs/usr/lib/x86_64-linux-gnu npm run test:e2e`
- **sudoなし環境の注意**: headless shell が `libnspr4.so` 等で起動失敗する。必要ライブラリは
  `~/.cache/swiss-stage-e2e-libs/` に展開済み(無ければ `apt-get download libnspr4 libnss3 libasound2t64 libatk1.0-0t64 libatk-bridge2.0-0t64 libcups2t64 libxkbcommon0 libatspi2.0-0t64 libxcomposite1 libxdamage1 libxfixes3 libxrandr2 libgbm1 libcairo2 libpango-1.0-0 libpangocairo-1.0-0` → `dpkg -x *.deb <dir>` で再作成)。
- アドホックにUIを駆動する場合も同じ `LD_LIBRARY_PATH` を付けて `node script.mjs`。

## 負荷テスト(perf/)

- 手順は `perf/README.md`。この環境は curl/wget が権限で拒否され k6 を導入できないため、
  データ投入は `node perf/seed-shared.mjs`、計測は Node fetch の同等スクリプトで代替する
  (300 VU 並行で `GET /api/v1/shared/{token}` を叩き p95/エラー率を出す)。
- レート制限(IP別60req/分)に当たるため、負荷計測時は
  `--app.rate-limit.shared.capacity=100000 --app.rate-limit.shared.refill-per-minute=100000` を付けてbackendを起動する。

## セレクタのハマりどころ

- サイドバー/下部タブのナビ項目は `Link` なので `getByRole('link', ...)`(buttonではない)。
- MUI Select の combobox は表示用divに役割が付く。`aria-label` は `slotProps={{ select: { SelectDisplayProps: {...} } }}` 経由でないと届かない。
- 参加者テーブルのセルは操作列のaria-labelに氏名が含まれるため `{ exact: true }` を付ける。
- 結果入力後もセレクトは残る(確定まで変更可)。行数ぶんだけ `nth(i)` で1回ずつ入力する。

## 駆動する価値のあるフロー(クリティカルパス)

ログイン → 大会作成 → 参加者追加+CSVインポート(奇数人にするとBYEも通る)→ 大会開始 →
組み合わせ生成 → 全結果入力 → ラウンド確定 → 順位表 → 設定(改名・削除ガード)。
不正CSV(未知の段級位)を投げると行番号付きエラーがダイアログに出るのも確認できる。
