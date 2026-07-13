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

/** 勝点(0.5刻み)の表示。整数はそのまま、小数は「2.5」形式 */
export function formatPoints(points: number): string {
  return Number.isInteger(points) ? String(points) : points.toFixed(1);
}
