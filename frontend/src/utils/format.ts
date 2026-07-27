/** ISO8601文字列 → 「2026/7/12 10:00」形式。不正な値は空文字(表示を壊さない) */
export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toLocaleString('ja-JP', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

/**
 * 開催日(YYYY-MM-DD) → 「2026/8/15」形式。未設定(null)・不正な値は空文字(表示を壊さない)。
 * new Date() のタイムゾーン解釈で日付がずれるのを避けるため、文字列のまま組み立てる
 */
export function formatEventDate(date: string | null): string {
  if (date === null) {
    return '';
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  if (match === null) {
    return '';
  }
  return `${match[1]}/${Number(match[2])}/${Number(match[3])}`;
}

/** 勝点(0.5刻み)の表示。整数はそのまま、小数は「2.5」形式 */
export function formatPoints(points: number): string {
  return Number.isInteger(points) ? String(points) : points.toFixed(1);
}
