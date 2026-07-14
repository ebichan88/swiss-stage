# 負荷テスト(リリース前・大会前に実施)

ラウンド確定直後に参加者全員(最大300名)が共有ページを一斉に開くスパイクを模擬する
(設計: `.claude/01_development_docs/14_performance_optimization.md` §6)。

**合格基準: エラー率0%、p95 < 1秒**(キャッシュコールド状態から開始)

## 手順

```bash
# 1. DynamoDB Local + バックエンド起動(レート制限を緩和して起動する。
#    IP別60req/分の制限は本番では参加者ごとに別IPのため、単一IPからの負荷計測では外す)
cd backend && docker compose up -d dynamodb-local && ./scripts/create-table.sh
./gradlew bootRun --args='--spring.profiles.active=local \
  --app.rate-limit.shared.capacity=100000 --app.rate-limit.shared.refill-per-minute=100000'

# 2. データ投入(300名・第1ラウンド生成済み・共有トークン発行)
#    標準エラーにマッチング所要時間、標準出力にトークンが出る
TOKEN=$(node perf/seed-shared.mjs)

# 3. 負荷実行(k6: https://k6.io)
k6 run -e TOKEN=$TOKEN perf/k6-shared-spike.js
```
