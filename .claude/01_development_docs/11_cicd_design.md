# CI/CD設計書

## 1. 基本方針

- CI: GitHub Actions(PR時に必ず実行、mainへの直接pushは禁止)
- CD: MVP期は手動デプロイ(スクリプト化)。安定後に AWS CodeDeploy 化を検討
- **「mainは常にデプロイ可能」**を守る

---

## 2. CIパイプライン(PR時)

`.github/workflows/ci.yml`

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main]

jobs:
  frontend:
    runs-on: ubuntu-latest
    defaults: { run: { working-directory: frontend } }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: npm, cache-dependency-path: frontend/package-lock.json }
      - run: npm ci
      - run: npm run lint
      - run: npm run type-check
      - run: npm run test -- --ci
      - run: npm run build

  backend:
    runs-on: ubuntu-latest
    defaults: { run: { working-directory: backend } }
    services:
      dynamodb-local:
        image: amazon/dynamodb-local:latest
        ports: ["8000:8000"]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21, cache: gradle }
      - run: ./scripts/create-table.sh
        env: { DYNAMODB_ENDPOINT: "http://localhost:8000" }
      - run: ./gradlew check build
```

### ルール

- どちらかのジョブが失敗したPRはマージ禁止(ブランチ保護設定)
- E2E(Playwright)はPRごとには実行しない(遅いため)。`.github/workflows/e2e.yml`(workflow_dispatch)をリリース前に手動実行
- 依存更新は Dependabot(週次、`gradle` / `npm` / `github-actions`)

---

## 3. デプロイ(MVP期: 手動スクリプト)

`infra/scripts/deploy.sh`(概略):

```bash
# 1. フロントエンドをビルドし、Spring Bootのstatic配下へ配置
cd frontend && npm ci && npm run build
cp -r dist/* ../backend/src/main/resources/static/

# 2. バックエンドをビルド
cd ../backend && ./gradlew clean build

# 3. EC2へ転送し、サービス再起動
scp build/libs/swiss-stage.jar ec2:/opt/swiss-stage/
ssh ec2 'sudo systemctl restart swiss-stage'
```

### デプロイ運用ルール

- **大会前日・当日のデプロイ禁止**(検証時間が取れないため)
- デプロイ後は必ずスモークチェック: ログイン → 大会一覧表示 → 共有ページ表示
- EC2上は systemd でプロセス管理(`swiss-stage.service`)。JVMオプションは t3.micro(1GB)に合わせ `-Xmx512m`

---

## 4. 環境

| 環境 | 用途 | インフラ |
|------|------|---------|
| local | 開発 | DynamoDB Local + bootRun |
| production | 本番 | EC2 + DynamoDB |

- MVP期はステージング環境を持たない(コスト優先)。代わりに本番DynamoDBに `TEST#` プレフィックスの大会を作って検証し、検証後削除する
- 本番の秘密情報は EC2 の環境変数ファイル(`/opt/swiss-stage/env`、パーミッション600)で管理

---

## 5. 将来(MVP後)

- CodeDeploy によるゼロダウンタイムデプロイ
- mainマージで自動デプロイ(大会カレンダーと連動したデプロイ凍結期間の仕組み)
- CloudFront + S3 でフロントエンドを分離配信
