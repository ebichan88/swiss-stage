/**
 * 負荷テスト用データ投入(14_performance_optimization.md §6)。
 * 300名の大会を作成 → 開始 → 第1ラウンド生成 → 共有トークン発行し、トークンを標準出力に出す。
 * 併せてマッチング生成の所要時間を表示する(目標: 300名で5秒以内)。
 *
 * 前提: バックエンド(:8080、localプロファイル)+ DynamoDB Local 起動済み。
 * 実行: node perf/seed-shared.mjs [participants=300]
 */
const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8080';
const PARTICIPANTS = Number(process.argv[2] ?? 300);

let cookie = '';

async function api(method, path, body, headers = {}) {
  const res = await fetch(`${BASE_URL}/api/v1${path}`, {
    method,
    headers: { cookie, ...headers },
    body,
  });
  const setCookie = res.headers.get('set-cookie');
  if (setCookie) cookie = setCookie.split(';')[0];
  const json = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(`${method} ${path} -> ${res.status}: ${JSON.stringify(json)}`);
  }
  return json?.data;
}

const json = (body) =>
  [JSON.stringify(body), { 'content-type': 'application/json' }];

// 1. 開発用ログイン(local/testプロファイル限定)
await api('POST', '/auth/test-login');

// 2. 大会作成(300名 → 5ラウンド想定)
const tournament = await api('POST', '/tournaments', ...json({
  name: `負荷テスト ${new Date().toISOString()}`,
  gameType: 'GO',
  totalRounds: 5,
}));

// 3. 参加者CSVインポート(1リクエストで投入)
const ranks = ['9段', '5段', '初段', '1級', '5級', '10級', '15級', ''];
const csv = ['氏名,所属,段級位']
  .concat(
    Array.from({ length: PARTICIPANTS }, (_, i) =>
      `負荷 ${String(i + 1).padStart(3, '0')},テスト会,${ranks[i % ranks.length]}`),
  )
  .join('\n');
const form = new FormData();
form.append('file', new Blob([csv], { type: 'text/csv' }), 'participants.csv');
await api('POST', `/tournaments/${tournament.id}/participants/import`, form);

// 4. 開始 → 第1ラウンド生成(マッチング時間を計測)
await api('POST', `/tournaments/${tournament.id}/start`);
const startedAt = performance.now();
await api('POST', `/tournaments/${tournament.id}/rounds`);
const pairingMs = Math.round(performance.now() - startedAt);

// 5. 公開範囲=TOKEN + 共有トークン発行
const current = await api('GET', `/tournaments/${tournament.id}`);
await api('PATCH', `/tournaments/${tournament.id}`, ...json({
  visibility: 'TOKEN',
  version: current.version,
}));
const withToken = await api('POST', `/tournaments/${tournament.id}/share-token/regenerate`);

console.error(`tournamentId: ${tournament.id}`);
console.error(`participants: ${PARTICIPANTS}`);
console.error(`round1 pairing: ${pairingMs}ms (目標: 5000ms以内)`);
console.log(withToken.shareToken);
