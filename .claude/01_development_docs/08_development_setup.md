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
秘密情報(Google OAuth2クライアントシークレット等)は環境変数で注入し、**絶対にコミットしない**:

```
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx
JWT_SECRET=xxx            # ローカルは任意の32文字以上
DYNAMODB_ENDPOINT=http://localhost:8000   # localのみ
```

フロントは `frontend/.env.local`(gitignore済み)に `VITE_API_BASE_URL` 等を設定。

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
