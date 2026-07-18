# 開発環境セットアップ

## 1. 必須ツール

| ツール | バージョン | 確認コマンド |
|-------|----------|-------------|
| Node.js | 20.x LTS | `node -v` |
| Java JDK | 21 (Temurin推奨) | `java -version` |
| Gradle | 8.x (Wrapperを使用) | `./gradlew -v` |
| Docker | 24.x以上 | `docker -v` |
| AWS CLI | 2.x | `aws --version` |

---

## 2. リポジトリ構成(モノレポ)

```
swiss-stage/
├── frontend/          # React + Vite
├── backend/           # Spring Boot
├── infra/             # IaC・AWS設定スクリプト(将来)
├── docs/              # 公開ドキュメント
└── .claude/           # AI向け設計ドキュメント(本ディレクトリ)
```

---

## 3. 初回セットアップ手順

### バックエンド

```bash
cd backend

# DynamoDB Local 起動(初回はイメージ取得あり)
docker compose up -d dynamodb-local

# ローカルテーブル作成(GSI含む)
./scripts/create-table.sh

# 起動(localプロファイル)
./gradlew bootRun --args='--spring.profiles.active=local'
# → http://localhost:8080
```

### フロントエンド

```bash
cd frontend
npm install
npm run dev
# 依存を追加・更新して package-lock.json が変わるときは、CI(Node 20 / npm 10)と
# 同じ解決になるよう npm@10 で再生成する(npm 11はpeerDependenciesをlockに含めず npm ci が落ちる):
#   npx -y npm@10 install --package-lock-only
# → http://localhost:5173 (Viteが /api を localhost:8080 にプロキシ)
```

### 環境変数

`backend/src/main/resources/application-local.yml` で管理(ローカル用はコミット可)。
**基本的な動作確認だけなら環境変数は不要**(`JWT_SECRET`はダミー値がyamlにデフォルト設定済み、Google OAuth2も未設定でよい。下記「ログイン」参照)。
実Google認証を試す場合や秘密情報を上書きしたい場合のみ、環境変数で注入する(**絶対にコミットしない**):

```
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx
JWT_SECRET=xxx            # ローカルは任意の32文字以上
DYNAMODB_ENDPOINT=http://localhost:8000   # localのみ
```

フロントは `frontend/.env.local`(gitignore済み)に `VITE_API_BASE_URL` 等を設定。

### ログイン(ローカル)

Google OAuth2を実際に通す必要はない。`local`/`test` プロファイル限定のテストログインエンドポイント
`POST /api/v1/auth/test-login` があり、UIからは `/login` 画面の「開発用ログイン」ボタンで使える
(**本番ビルドには含まれない**)。詳細: `12_e2e_test_design.md` §3、`03_api_design.md`。

### 起動確認(どのURLを開くか)

3つのポートは役割が異なる。**ブラウザで開くのは 5173 のみ**。

| ポート | 役割 | ブラウザで開く? |
|-------|------|----------------|
| 5173 | フロントエンド(Vite) | ✅ ここを開く |
| 8080 | バックエンドAPI(Spring Boot) | ❌ フロントから `/api` 経由で呼ばれるだけ |
| 8000 | DynamoDB Local | ❌ データ置き場のAPIエンドポイント。UIなし |

起動確認の手順:

1. `docker compose up -d dynamodb-local` → `./scripts/create-table.sh`(backend/)
2. `./gradlew bootRun --args='--spring.profiles.active=local'`(backend/)。ログに
   `Started SwissStageApplication` が出れば起動完了(`curl`が使えない環境ではログで確認する)
3. `npm run dev`(frontend/)。ログに `VITE ready` が出れば起動完了
4. ブラウザで **http://localhost:5173** を開き、`/login` の「開発用ログイン」でログイン

---

## 4. 日常の開発コマンド

### フロントエンド(frontend/)

```bash
npm run dev           # 開発サーバー起動
npm run build         # プロダクションビルド
npm run lint          # oxlint
npm run format        # Prettier
npm run type-check    # tsc --noEmit
npm run test          # Vitest単体テスト
npm run test:e2e      # Playwright(バックエンド起動が前提)
npm run check         # lint + format + type-check + test(コミット前に必ず実行)
```

### バックエンド(backend/)

```bash
./gradlew bootRun     # 起動
./gradlew test        # 単体+統合テスト(DynamoDB Local自動利用)
./gradlew check       # test + 静的解析(コミット前に必ず実行)
./gradlew build       # ビルド(jar生成)
```

---

## 5. Docker Compose(backend/docker-compose.yml)

```yaml
services:
  dynamodb-local:
    image: amazon/dynamodb-local:latest
    ports:
      - "8000:8000"
    command: "-jar DynamoDBLocal.jar -sharedDb -dbPath /data"
    user: root   # named volumeがroot所有のため。非rootだとSQLiteが書けずクラッシュループする
    volumes:
      - dynamodb-data:/data
volumes:
  dynamodb-data:
```

- `-sharedDb` を必ず付ける(付けないと認証情報ごとにDBが分かれ「テーブルがない」事故になる)
- データを消してやり直したい場合: `docker compose down -v` → 再起動 → テーブル再作成

---

## 6. Git運用ルール

- ブランチ: `main`(常にデプロイ可能) / `feature/xxx` / `fix/xxx`
- コミット前に `npm run check`(frontend)・`./gradlew check`(backend)を通す
- コミットメッセージ: `feat:` `fix:` `docs:` `test:` `refactor:` `chore:` プレフィックス(Conventional Commits)
- `.claude/` 配下の設計ドキュメントは実装と乖離したら**同じPRで更新する**
