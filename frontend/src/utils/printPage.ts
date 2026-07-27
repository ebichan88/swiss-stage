/**
 * ブラウザの印刷ダイアログを開く。CLAUDE.md 落とし穴9(window.*直呼び禁止)の例外
 * (代替APIが存在しないため許容)。この関数以外から window.print() を呼ばないこと。
 * データ取得完了前に呼ぶと白紙が印刷されるため、useEffect等での自動実行は禁止し、
 * ユーザー操作(印刷ボタンのクリック)からのみ呼び出す。
 */
export function printPage(): void {
  window.print();
}
