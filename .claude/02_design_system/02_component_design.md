# コンポーネント設計(MUIベース)

> MUIコンポーネントをベースに、Swiss Stage固有の使い分けルールを定義する。
> ここにないパターンが必要になったら、まず本ドキュメントに追記する。

---

## 1. ボタン

| 種類 | MUI設定 | 用途 | 1画面あたり |
|------|---------|------|------------|
| プライマリ | `variant="contained" color="primary"` | 画面の主要アクション(保存・生成・確定) | 1つまで |
| セカンダリ | `variant="outlined"` | 補助アクション(キャンセル・戻る)。背景色は既定で`background.paper`(テーマの`styleOverrides`、`01_design_principles.md`) | 制限なし |
| テキスト | `variant="text"` | 低優先度(詳細を見る等) | 制限なし |
| 破壊的 | `variant="contained" color="error"` | 削除・トークン再発行 | 必ずConfirmDialogとセット |

- ローディング中は `disabled` + `CircularProgress`(二重送信防止)。送信系ボタンは必ずこの対応を入れる
- ボタンラベルは動詞で(「OK」ではなく「確定する」「削除する」)

### ResultButton(結果入力ボタン・独自コンポーネント)

- 勝ち=success系 / 負け=error系の大型ボタン(高さ56px以上)
- タップ → 選択状態表示 → 「送信する」で確定(2段階。誤タップ防止)

---

## 2. フィードバック

| パターン | コンポーネント | 用途 |
|---------|--------------|------|
| 操作成功 | Snackbar(3秒自動クローズ) | 保存完了・送信完了 |
| 操作失敗 | Snackbar(error, 手動クローズ+再試行ボタン) | APIエラー |
| 破壊的操作の確認 | ConfirmDialog(独自) | 削除・確定・再発行 |
| 重大確認 | ConfirmDialog + テキスト入力一致 | 大会削除(大会名入力) |
| 処理中 | ボタン内スピナー(短時間) / LinearProgress(画面上部・長時間) | |

- `window.alert` / `window.confirm` 禁止
- Snackbar表示はアプリ共通の `useSnackbar()` フック経由で行う(直接レンダリングしない)

---

## 3. データ表示

### 順位カード(RankingBoard)

- 共有ページ(順位表タブ)と管理画面(順位メニュー)で共用。「順位表」と「対戦結果」の内容重複を避けるため、
  順位カードは勝点・SOS/SOSOSのみを表示し、対局グリッドは持たない(それはMatchResultsTable側の役割)
- 1〜3位は金/銀/銅のメダルカード(`rank.gold/silver/bronze`、`01_design_principles.md`)。アイコン+氏名(所属)+
  勝点チップ+SOS/SOSOSを表示
- 4位以降はコンパクトなリスト行(丸い順位バッジ+氏名(所属)+SOS/SOSOS+勝点チップ)
- 上位メダルカード群はCSS Grid(`repeat(auto-fill, minmax(200px, 1fr))`)で並べる。同率タイで最終行にカードが
  1枚だけ余っても、他行と同じ列幅のまま左詰めで表示し、横幅いっぱいに間延びさせない
- 初期表示時のみ1件ずつ段階フェードイン(`03_animation_system.md`)。更新時の再フェードはしない
- 自分の行(共有ページで名前検索時)はハイライト
- ラウンド1が確定するまでは順位表を表示しない(未確定時は全員同率rank=1で返るため)。確定済みラウンドが
  1つもない間は空状態(「順位はまだありません。ラウンドを確定すると表示されます」)を表示する

### 対戦結果表(MatchResultsTable)

- MUI `Table` ベース。列: No.・氏名(所属)・段級位・(ラウンドごとに相手・結果)・勝点・SOS・SOSOS・順位
- ヘッダー行は `primary.main` 背景+白文字(コントラスト強調)
- データ行は `background.paper`/`background.default` を交互に敷くゼブラストライプ
- 結果の○は `success.main`、●は `error.main` で色分け(△は無色)。記号自体も必ず表示し、色だけに頼らない
- 相手列は氏名の代わりにNo.(entryOrder)を表示し、Tooltipで氏名(所属)を補足する
- 共有ページ(対戦結果タブ)と管理画面(対戦結果メニュー、順位メニューとは別画面)で共用

### 組み合わせ表(PairingTable)

- スマホカードでは卓番号を最も大きく表示(会場で自分の卓を探すため)。PCテーブルでは他の列と同じ通常ウェイトで表示(運営者が会場で卓を探す用途はないため)
- スマホでは1対局=1カード(`MatchCard`)、PCではテーブル表示
- 結果入力済みの対局は ○/● を表示
- PCのヘッダー行は対戦結果表と同じ `primary.main` 背景+白文字
- PCのデータ行は対戦結果表と同じ `background.paper`/`background.default` を交互に敷くゼブラストライプ
- 列: 卓・対局者1・対局者2・結果(入力コントロール)・申告ステータス(Chip)。申告ステータスは結果列に埋め込まず独立した列にする(スマホカードでは列がないため結果の下にChipをまとめて表示)

### 共有ページの意匠(SharedPage / SharedResultPage・2026-08刷新)

参加者が最も多く触れる画面のため、`01_design_principles.md` のコンセプト(和・深緑・落ち着いた品)を
最も明示的に表出させるパイロット対象とした(`.claude/07_plans/03_design_system_brushup.md`)。
その後、以下の意匠パターンを他画面へ横展開した(`.claude/07_plans/04_design_system_rollout.md`)。

- **ヘッダー帯**: 大会名・状態バッジ・現在ラウンドを `primary.light` の帯(角丸・内側余白)で囲み、
  ページ上部に基調色を置く。個人戦共有ページ(`SharedPage`)・団体戦共有ページ(`TeamSharedPage`)に加え、
  運営者管理画面の共通レイアウト(`TournamentLayout`)の見出し行にも同トークンで適用済み
- **卓番号タイル**(`SharedMatchCard`/`SharedTeamMatchCard`。各共有ページ専用のローカルコンポーネント):
  卓番号を `primary.main` 地に白抜き数字の丸みを帯びたタイル(角丸20px・最小幅40px・高さ40px)で表示する。
  碁石・駒の目印を想起させつつ、既存要件「会場で瞬時に読む数字」(本ドキュメント§3「組み合わせ表(PairingTable)」/
  `01_design_principles.md`§2)をそのまま形にしたもの。ラベルが長い場合(グループ名付き卓番号 `A-12` 等)は
  幅が伸びる前提でよく、正円である必要はない。高さは `minHeight`(固定`height`にしない)+
  `whiteSpace: nowrap` とし、長いグループ名でも折り返してタイルの縦寸法を崩さないようにする。
  タイル下に「卓」の `caption` を添えてラベルを補う。団体戦版はチーム名のみ表示し個人名は出さない
  - PairingTable/TeamPairingTable(運営者向け)の `MatchCard` はこの意匠を適用しない。
    会場での即時可読性より情報密度・機能性を優先する運営者管理画面の判断を維持するため
    (`00_basic_design.md`§4、`.claude/06_adr/08_design_system_admin_internal_scope.md`)
- **アクションアクセント**: 結果入力が可能なカードは左端に3px幅の `primary.main` アクセント線を付ける。
  色だけに頼らず「結果入力」ボタン自体が主たる合図であるため、アクセント線は補助にとどめる。
  `TournamentLayout` のサイドバー選択項目・下部タブ選択項目にも同じアクセント色(左罫線/選択色)を適用し、
  現在地の視覚的な合図を共有ページと揃えた
- **タブ**: `MuiTabs` のインジケーターをテーマ全体で太く(3px)・角丸にしている(`theme/index.ts`)。
  下線の下に敷く `divider` は上記の墨線トークンに追従する
- 選択系ボタン(`SharedResultPage`/`SharedTeamResultPage`「あなたはどちらですか」)には勝敗マーク(○/●)と
  紛らわしい装飾(丸バッジ等)を付けない。誤読リスクが「正確性」原則(CLAUDE.md)に反するため、
  意匠より安全性を優先した(団体戦版も同じ探索の結果、構造変更なしという同じ結論になった)
- `RankingBoard`/`TeamRankingBoard` の4位以降バッジは、卓番号タイルと同じ `primary.main` 地+白文字の
  配色に揃え、順位数字は等幅(tnum)で表示する(RND-AC-015)

### 参加者一覧表(ParticipantTable)

- 列: No.・氏名・所属(スマホでは非表示)・棋力・(複数グループ大会のみ)グループ・状態・操作
- ヘッダー行・データ行のゼブラストライプは対戦結果表と共通(`primary.main` 背景+白文字のヘッダー、`background.paper`/`background.default` の交互ストライプ)
- 棄権者の行はストライプに加えて `opacity: 0.55` で半透明表示し、状態列の「棄権」Chipに
  `PersonOffIcon` を添える(色だけに頼らない。PTC-AC-013。団体戦の`TeamTable`も同じ表示でTEAM-AC-025)

### 検索・フィルタツールバー(TournamentSearchToolbar)

- 一覧画面の絞り込みは、検索`TextField`(`InputAdornment`に`SearchIcon`、`placeholder`をラベル代わりに
  する。フォームの入力欄と異なり送信対象のデータではなく即時反映の表示フィルタのため、`4. フォーム`の
  「ラベルは必ず表示」は適用しない)+ 状態`TextField select`(`slotProps.select.SelectDisplayProps`で
  `aria-label`を明示、`TeamTable`のグループ選択と同じパターン)の組み合わせで構成する
- レイアウトは`Stack`(`direction={{ xs: 'column', sm: 'row' }}`)で、モバイルは縦積み・デスクトップは
  横並びにして検索欄を広めに確保する
- 絞り込み結果0件時は専用の`EmptyState`(「条件に一致する大会がありません」+「検索条件をクリア」)を出す
  (`07_plans/07_tournament_list_search_filter.md`)
- 対象データが1件も無い・ローディング中・エラー時はツールバー自体を表示しない(絞り込む対象が無い状態で
  操作可能に見せない)

### 個別admin画面内部の展開(`.claude/07_plans/05_design_system_admin_internal.md`)

参加者管理(`ParticipantTable`/`ParticipantFormDialog`/`CsvImportDialog`/`GroupManagerDialog`)・
チーム管理(`TeamTable`/`TeamFormDialog`/`TeamMemberFormDialog`/`TeamMemberManagerDialog`/
`TeamCsvImportDialog`)・組み合わせ表見出し(`RoundsPage`/`TeamRoundsPage`)・大会概要
(`TournamentOverviewPage`)・大会設定(`SettingsPage`)に、上記の意匠トークンを適用済み。
`ParticipantTable`/`TeamTable` は既存のヘッダー帯・ゼブラストライプに加え、棄権者の行を
`PersonOffIcon` 付きの「棄権」Chipで示す(半透明表示と併用、色だけに頼らない)。各ページの
サブ見出し(「参加者(N名)」等)には左端4pxの `primary.main` アクセント線を付けた。
ダイアログの `DialogTitle` は `primary.main` のベタ塗り背景+白文字(`primary.contrastText`)
にし(Paperの角丸から色がはみ出さないよう `overflow: hidden` でクリップする)、
`TournamentOverviewPage`/`SettingsPage` のカードには上端3pxの `primary.main` アクセント線
(危険な操作カードは全辺2pxの `error.main` 枠)を付けた。組み合わせ表の「結果」セレクトボックスは
ゼブラストライプの行によって塗りが変わって見えないよう `background.paper` を背景色にしている
(現在はテーマの`MuiOutlinedInput`の既定値になったため、コンポーネント側の個別`sx`指定は無い)。
いずれも `TournamentLayout` の見出し帯と二重にならないよう、既存トークンの微調整に留めている。
`PairingTable`/`TeamPairingTable` の `MatchCard`(卓番号のタイル化)と印刷用ページは
対象外のまま維持している(`00_basic_design.md`§4、`06_adr/08_design_system_admin_internal_scope.md`)。

### ステータス表示(StatusBadge)

- MUI `Chip` ベース: 準備中=default / 開催中=success / 終了=default(outlined)
- ラウンド状態: 組み合わせ中=warning / 対局中=info / 確定=success

### 空状態(EmptyState)

- アイコン + 一文 + 次のアクションボタンの3点セット
- 例: 「参加者がまだいません」+「CSVをインポート」ボタン

### 印刷帳票(PrintRoster / PrintTeamRoster / PrintMatchResultsTable / 対局カード)

紙で読む前提のため、画面向けコンポーネント(RankingBoard・MatchResultsTable・ParticipantTable)とは表現を変える。
既存コンポーネントの流用や `variant="print"` の追加はせず、`components/features/print/` 配下に専用コンポーネントを置く
(印刷用の見た目の変更が画面側に波及するのを防ぐ)。

- **ゼブラストライプなし**: 交互の背景色は白黒印刷でインクを食い、紙面では視認性向上に寄与しない
- **手書き記入用の表は濃色の格子罫線を引く**(`printSx.ts` の `writableGridSx`)。MUI Tableの既定(下線のみ・薄いdivider色)は
  記入欄の境界として薄すぎて実用にならないため、全辺に `text.primary` 色の罫線を引く
- **ヘッダーは緑ベタ塗りにしない**: `theme.print.headerBg`(薄グレー)+黒文字+罫線にする。全ページに緑を刷るとインク消費が大きい
- **Tooltipを使わない**: 紙にホバー操作は存在しない。SOS/SOSOSの説明(`TableHeaderTooltip`相当)は表の下に凡例1行で足す
- **フォント・余白はmm/pt単位**(`theme.print`トークン、`01_design_principles.md`)。8pxグリッドの対象外
- 全帳票は共通の `PrintReportHeader`(大会名・帳票名・開催日・グループ名)を先頭に置く
- 個人名の掲載可否は帳票ごとに異なる(`04_screen_transition_design.md` §5-2)。団体戦の対局カード・対戦結果表(印刷)は
  チーム名のみで、参加者名簿(運営専用)のみメンバー氏名を出してよい
- **対戦結果表(印刷)は大会開始前に印刷する手書き記入用シート**: 対戦相手・結果・勝点・SOS・SOSOS・順位はラウンド進行に
  合わせて手書きで記入するため常に空欄で出す。ラウンド列は生成済みラウンド数によらず `totalRounds` 分をすべて出す
  (画面版のMatchResultsTable/TeamMatchResultsTableとは実データを表示する点で用途が異なるため、専用の純関数 `printMatchResultsTableData.ts`
  を持つ。画面版の `matchResultsTableData.ts`/`teamMatchResultsTableData.ts` とは共有しない)
- **対局カードは転置レイアウト**(実業団囲碁大会の対局カード様式): ラウンドを列・記入項目を行にし、左にNo.を大きく、
  下に集計欄を置く。個人戦(`MatchCardSheet`)は氏名の横に段級位枠を持ち、記入行は「相手/勝敗」+集計「勝敗合計」。
  団体戦(`TeamMatchCardSheet`)は記入行が「相手/チーム勝敗/個人勝敗(主将・副将…のボード列)」+集計「チーム勝敗合計/個人勝敗合計」。
  団体戦はメンバーの個人名を出さない(チーム名のみ)
- **対局カードも手書き用の濃色罫線**を引く(`writableGridSx` の実線格子)。カードの外枠は `text.primary` の実線。
  既定の `divider` 色は印刷で見えない

---

## 4. フォーム

- React Hook Form + MUI `TextField`。`Controller` で接続
- ラベルは必ず表示(placeholderをラベル代わりにしない)
- エラーは `helperText` にインライン表示、送信時に最初のエラーへスクロール
- 必須項目には「必須」Chipを付ける(`*` だけにしない)
- CSVインポートはドラッグ&ドロップ + ファイル選択の両対応、取り込み前にプレビュー表示

---

## 5. 命名・実装規約

- 独自コンポーネントは `components/ui/` に置き、Propsは `XxxProps` で export
- MUIをラップする場合、元のPropsを `...rest` で透過させる
- Storybookは**ページレベルのみ**導入する(`10_frontend_design.md` §7)。`components/ui/` 単体のカタログ化はしない。
  当初「導入しない(MVP)」としていたが、実装前にUIの合意を取れず手戻りが発生する課題を解消するため撤回した
