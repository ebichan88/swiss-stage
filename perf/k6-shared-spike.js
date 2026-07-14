import http from 'k6/http';
import { check } from 'k6';

/**
 * ラウンド確定直後のスパイクを模擬する負荷テスト(14_performance_optimization.md §6)。
 * 参加者全員(300名)が一斉に共有ページ(組み合わせ・順位表の集約API)を開く。
 *
 * 前提:
 *   1. node perf/seed-shared.mjs でデータ投入し、出力されたトークンを使う
 *   2. IPベースのレート制限(60req/分)は本番では参加者ごとに別IPのため、
 *      ローカル計測ではバックエンドを上限緩和で起動する:
 *      ./gradlew bootRun --args='--spring.profiles.active=local \
 *        --app.rate-limit.shared.capacity=100000 --app.rate-limit.shared.refill-per-minute=100000'
 *
 * 実行:
 *   k6 run -e TOKEN=<shareToken> [-e BASE_URL=http://localhost:8080] perf/k6-shared-spike.js
 *
 * 合格基準: エラー率0%、p95 < 1秒(キャッシュコールド状態から開始)
 */
export const options = {
  scenarios: {
    round_confirm_spike: {
      executor: 'per-vu-iterations',
      vus: 300,
      iterations: 5,
      maxDuration: '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN;

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/shared/${TOKEN}`);
  check(res, {
    'status 200': (r) => r.status === 200,
    'success payload': (r) => r.json('success') === true,
  });
}
