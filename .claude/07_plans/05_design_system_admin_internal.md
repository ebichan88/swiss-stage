# 05. 個別admin画面内部へデザインシステムを展開する

- Status: planned
- Issue: #122
- PR: -

---

## 1. 背景・目的

Issue #119・PR #121(`.claude/07_plans/04_design_system_rollout.md`)で、共有ページ(個人戦)の
意匠パターンを RankingBoard/TeamRankingBoard・団体戦共有ページ・運営者管理画面の共通レイアウト
(`TournamentLayout`)へ展開した。一方 `.claude/06_adr/07_design_system_rollout_scope.md` の決定により、
個別admin画面の内部(参加者管理表・`PairingTable`・設定フォーム・CSVインポート等)は対象外のまま
残し、`00_basic_design.md` §4「運営者管理画面: 機能性優先でMUI標準に寄せてよい」をその範囲に
限って維持した。

このままでは個別admin画面だけ「MUI既定値のまま」で他画面と見た目が揃わない状態が残り続ける。
本計画は、その残った対象外領域(印刷用ページを除く)にパイロットで確立した意匠パターンを展開する。

**本計画は新しいデザイン方向性を発明しない。** パレット・タイポグラフィの再探索は行わず、
`01_design_principles.md` に定義済みのトークンを適用する。**機能・画面シナリオの変更も行わない**
(見た目の改善に限る、Issue #122「制約・やらないこと」)。

対象範囲・実装PRの分け方・go/no-go単位は `.claude/06_adr/08_design_system_admin_internal_scope.md`
で決定した。要約すると:

- 対象: 参加者管理(`ParticipantTable`・`ParticipantFormDialog`・`CsvImportDialog`・
  `GroupManagerDialog`)・チーム管理(`TeamTable`・`TeamFormDialog`・`TeamMemberFormDialog`・
  `TeamMemberManagerDialog`・`TeamCsvImportDialog`)・組み合わせ表(`PairingTable`・
  `TeamPairingTable`)・大会概要(`TournamentOverviewPage`)・大会設定(`SettingsPage`)
- 対象外: 印刷用ページ(`PrintRosterPage`等・`components/features/print/*`)。
  `PairingTable`/`TeamPairingTable` の `MatchCard` は卓番号タイル等の構造的意匠を引き続き適用しない
- 実装PR: 対象範囲まとめて1本
- go/no-go: 単一ゲート(パイロット・ADR 07 と同じ考え方を踏襲)

完了時に得られる状態:

- 対象範囲の画面・コンポーネントに、既存の意匠トークン(色・ヘッダー帯・アクセント線)が
  一貫して適用されている(構造・情報配置は変更しない)
- `00_basic_design.md` §4・`02_component_design.md`「共有ページの意匠」が展開後の実態に
  合わせて更新されている
- 既存のアクセシビリティ水準が展開後も維持されている
- 対象範囲についてVRTベースラインが更新されている

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**

意匠の都合で正確性を落とさない。参加者管理表・組み合わせ表の情報(棋力・グループ・申告状況等)は、
見た目を変えても「誤読しにくさ」が現状より悪化してはならない。

## 2. 画面シナリオ

**本計画は挙動を変更しない。** 画面シナリオは現行のままであり、以下は「改装後も一字一句そのまま
成立していること」を確認する回帰対象として列挙する。

- 運営者が 参加者管理画面(個人戦)を開くと ParticipantTable が新意匠(トークン適用)で表示され、
  棄権者の半透明表示・グループ列の出し分けは現行どおり成立する
- 運営者が 参加者管理画面で「追加」を押すと ParticipantFormDialog が新意匠で開き、
  入力・バリデーションエラー表示は現行どおり成立する
- 運営者が 参加者管理画面でCSVインポートを実行すると CsvImportDialog のプレビュー表・
  エンコーディング判定・エラー表示は現行どおり成立する
- 運営者が 団体戦大会でチーム管理画面を開くと TeamTable・TeamFormDialog・
  TeamMemberManagerDialog・TeamCsvImportDialog が個人戦版と同じ意匠パターンで表示される
- 運営者が 組み合わせ画面(個人戦/団体戦)を開くと PairingTable/TeamPairingTable が
  色・タイポグラフィトークンのみ新意匠になり、卓番号タイル等の構造は変わらない(会場での
  即時可読性より情報密度を優先する現行判断を維持)
- 運営者が 大会概要画面を開くと TournamentOverviewPage が他画面と揃った意匠で表示される
- 運営者が 大会設定画面を開くと SettingsPage のフォーム・共有URL表示・削除ボタンが
  新意匠で表示され、保存・共有URL再発行・大会削除の挙動は現行どおり成立する
- 運営者が スマホで上記いずれかの画面を開くと レスポンシブ表示(表→カード切替等)は
  現行どおり成立する
- 運営者が 印刷プレビュー(名簿・組み合わせ表・結果一覧)を開くと 印刷用ページは
  本計画の対象外のため現行のまま変わらない

観点チェック:

- 個人戦・団体戦の両方を書いたか
- PC・スマホ(レスポンシブ切替)の両方を書いたか
- 対象外(印刷用ページ、`PairingTable`/`TeamPairingTable` の `MatchCard` 構造)を
  誤って変更しないこと自体を回帰対象に含めたか

## 3. UI仕様

### 3-1. 本セクションの位置づけ

本計画は既に確立した意匠パターンの横展開であり(`04_design_system_rollout.md` §3-1と同じ位置づけ)、
パレット・タイポグラフィのトークンは再探索しない。探索が必要なのは、確立済みの語彙
(ヘッダー帯・アクセント線・ゼブラストライプ)を各画面固有の要素にどう当てはめるかという
適用レベルの判断のみ。

### 3-2. 不変条件(意匠の都合で動かさない)

パイロット・展開計画の不変条件(`03_design_system_brushup.md` §3-2、`04_design_system_rollout.md` §3-2)を
すべて引き継ぐ。加えて本計画固有:

| 不変条件 | 出典 |
|---|---|
| (パイロット・展開の不変条件をすべて引き継ぐ) | `03_design_system_brushup.md` §3-2、`04_design_system_rollout.md` §3-2 |
| `PairingTable`/`TeamPairingTable` の `MatchCard` は卓番号タイル・アクセント線等の構造的意匠を適用しない(色・タイポグラフィトークンの適用に限る) | `06_adr/08_design_system_admin_internal_scope.md` |
| 印刷用ページ(`PrintRosterPage`等)・`components/features/print/*` は対象外 | `06_adr/08_design_system_admin_internal_scope.md` |
| 参加者管理表の棄権者の視覚表現(半透明+`PersonOffIcon`)を維持する | `ParticipantTable.tsx` 現行実装 |
| フォーム(`ParticipantFormDialog`等)のバリデーションエラー表示(`helperText`/`aria-describedby`)を維持する | MUI標準機能・現行実装 |
| 大会削除・共有URL再発行等の破壊的操作は `ConfirmDialog` を経由する(CLAUDE.md 避けるべき落とし穴 9) | `SettingsPage.tsx` 現行実装 |

### 3-3. 動かしてよい範囲

**新しいトークンは追加しない。** `01_design_principles.md` に定義済みの色・タイポグラフィ・
角丸例外(ヘッダー帯24px)のみを使う。

- 各ページ見出し(`ParticipantsPage`/`TeamsPage`/`RoundsPage`/`TeamRoundsPage`/
  `TournamentOverviewPage`/`SettingsPage` の大会名・画面タイトル部分)へのヘッダー帯・
  アクセント線の適用(`TournamentLayout` の見出し行と重複しない範囲で、各画面固有の
  サブ見出し・ツールバー部分に適用する)
- ダイアログ(`ParticipantFormDialog`・`CsvImportDialog`・`GroupManagerDialog`・
  `TeamFormDialog`・`TeamMemberFormDialog`・`TeamMemberManagerDialog`・`TeamCsvImportDialog`)の
  `DialogTitle`・アクションボタンの配色・余白の微調整(既存トークンの範囲内)
- `PairingTable`/`TeamPairingTable` のヘッダー行・ゼブラストライプの配色(既存トークンの範囲内。
  卓番号の表示方法自体は変えない)
- `theme.components` の styleOverrides への追加(新規パターンが増える場合)

### 3-4. 探索の進め方とgo/no-go

`06_adr/08_design_system_admin_internal_scope.md` の決定により単一ゲートとする。

```
① 本計画のマージで開始
    ↓
② frontend-design で対象範囲を一括探索(Storybook上)
   - 既存ストーリー(ParticipantsPage.stories.tsx 等)に加え、未作成のページ
     (TeamsPage/RoundsPage/TeamRoundsPage/TournamentOverviewPage/SettingsPage)の
     ストーリーを探索前に追加する
    ↓
③ 実機相当(Storybookのモバイル/デスクトップ両ビューポート)で確認 → 対象範囲まとめてgo/no-goを人間が判断
    ↓ go
④ 対象範囲の実装 → 全画面の追従確認
    ↓
⑤ fixing-accessibility 再実行 → frontend-design の自己批評 + reviewer で仕上げ
    ↓
⑥ VRTベースライン更新
```

go/no-goの判断基準は展開計画と同じ4点(`04_design_system_rollout.md` §3-4):

- 3原則(明快・堅実・信頼)が現行より強まっているか
- コンセプト(和・落ち着いた品)が既存の共有ページ・展開済み画面と一貫しているか
- 運営者の実務利用で読めるか・操作しやすいか(会場での即時可読性は優先度が下がってよい)
- 不変条件(§3-2)を1つも破っていないか

no-goの場合は②に戻る。対象範囲の一部にのみ問題がある場合は、`06_adr/08_design_system_admin_internal_scope.md`
§4の撤回条件に従い、問題のある画面・コンポーネントのみコミット単位でrevertする。

### 3-5. 対象画面/コンポーネントの現行構造

#### 参加者管理(`ParticipantsPage.tsx` / `ParticipantTable.tsx` / `ParticipantFormDialog.tsx` / `CsvImportDialog.tsx` / `GroupManagerDialog.tsx`)

現行、`ParticipantTable` は既に `primary.main` 背景+白文字のヘッダー行を持つ(ゼブラストライプは
未適用、`background.paper` 単色)。`ParticipantsPage` 自体の見出し(`Typography` のみ、帯なし)・
ツールバー(複数 `Button`)はMUI標準のまま。

- **改装してよい**: ページ見出しへのヘッダー帯適用、`ParticipantTable` のデータ行ゼブラストライプ
  (対戦結果表と同じ配色)、ダイアログ(`ParticipantFormDialog`/`CsvImportDialog`/
  `GroupManagerDialog`)の `DialogTitle` 配色
- **構造を変えない**: 列構成(No./氏名/所属/棋力/グループ/状態/操作)、棄権者の半透明+アイコン表示、
  CSVインポートのプレビュー行数上限(`PREVIEW_MAX_ROWS`)・エンコーディング自動判定ロジック

#### 団体戦チーム管理(`TeamsPage.tsx` / `TeamTable.tsx` / `TeamFormDialog.tsx` / `TeamMemberFormDialog.tsx` / `TeamMemberManagerDialog.tsx` / `TeamCsvImportDialog.tsx`)

参加者管理と対になる構造(`TeamTable` も同様のヘッダー行スタイル)。参加者管理と同じ範囲・
同じ制約で改装し、個人戦と団体戦で見た目がずれないようにする。

- **改装してよい/構造を変えない**: 参加者管理と同じ(チーム名・メンバー構成の列構造は変えない)

#### 組み合わせ表(`RoundsPage.tsx` / `PairingTable.tsx`、`TeamRoundsPage.tsx` / `TeamPairingTable.tsx`)

現行、`PairingTable`(PC表示)は既に `primary.main` ヘッダー+ゼブラストライプを持つ。
スマホ表示は `Card` ベースの1対局1カードで、卓番号を `h2` テキストのみで表示する
(卓番号タイルの丸み装飾なし。これは意図的な現行仕様)。

- **改装してよい**: `RoundsPage`/`TeamRoundsPage` のページ見出しへのヘッダー帯適用、
  `PairingTable`/`TeamPairingTable` のヘッダー・ゼブラストライプの配色微調整(既存トークン範囲内)
- **構造を変えない**: PC=表・スマホ=カードの出し分け、スマホカードの卓番号テキスト表示
  (タイル化しない。`06_adr/08_design_system_admin_internal_scope.md`)、
  `MatchResultControl`/`ReportStatusChip` の申告状況表示ロジック

#### 大会概要(`TournamentOverviewPage.tsx`)

現行、大会名・状態バッジ・各種サマリーカードで構成(`TournamentLayout` の見出し行と別に、
ページ本体側にも大会情報の表示がある)。

- **改装してよい**: サマリーカードの配色・区切り線(既存トークン範囲内)
- **構造を変えない**: `TournamentLayout` の見出し行(既に展開済み)と重複する情報を
  ページ本体側で再度ヘッダー帯化しない(二重のヘッダー帯を避ける)

#### 大会設定(`SettingsPage.tsx`)

現行、`Card`+`react-hook-form` によるフォーム(名前・開催日・公開範囲・結果入力許可の
スイッチ・共有URL表示・共有URL再発行・大会削除)。

- **改装してよい**: `Card` の配色・区切り、フォームラベルのタイポグラフィ
- **構造を変えない**: フォーム項目の並び順、共有URL再発行・大会削除の `ConfirmDialog` 経由フロー

### 3-6. 4状態・レスポンシブ・大量データ

いずれも**現行の挙動を維持する**。改装で失わないことを確認する対象として明示する。

| 状態 | 表示(現行・維持) |
|---|---|
| 通常 | 各画面の現行レイアウト(トークンのみ変更) |
| 空(0件) | `EmptyState`(参加者0人・チーム0件等。既存メッセージ・導線を維持) |
| ローディング | `LoadingState`/`FullPageSpinner`(既存) |
| エラー | `ErrorState`(再試行導線。既存) |

- **レスポンシブ**: 参加者管理表・組み合わせ表は既存のブレークポイント(`sm`/`md`)での
  表→カード切替・列非表示を維持する
- **大量データ時**: 300人規模の参加者一覧・組み合わせ表での縦スクロール・横スクロールは
  既存の挙動を維持する(構造を変えないため新規の崩れは想定しないが、VRTで確認する)

## 4. 技術設計

### 4-1. レイヤーごとの変更点

- **domain / application / infrastructure**: 変更なし
- **presentation(backend)**: 変更なし
- **API**: 変更なし(`schema/openapi.yaml` は更新しない)
- **frontend**: 以下のみ

### 4-2. 変更するファイル(実装PRでの想定。探索結果により増減しうる)

- `frontend/src/components/features/participant/ParticipantTable.tsx` / `ParticipantFormDialog.tsx` /
  `CsvImportDialog.tsx` / `GroupManagerDialog.tsx`
- `frontend/src/components/features/team/TeamTable.tsx` / `TeamFormDialog.tsx` /
  `TeamMemberFormDialog.tsx` / `TeamMemberManagerDialog.tsx` / `TeamCsvImportDialog.tsx`
- `frontend/src/components/features/round/PairingTable.tsx`
- `frontend/src/components/features/team/TeamPairingTable.tsx`
- `frontend/src/pages/ParticipantsPage.tsx` / `TeamsPage.tsx` / `RoundsPage.tsx` /
  `TeamRoundsPage.tsx` / `TournamentOverviewPage.tsx` / `SettingsPage.tsx`
- 新規Storybookストーリー: `TeamsPage.stories.tsx` / `RoundsPage.stories.tsx` /
  `TeamRoundsPage.stories.tsx` / `TournamentOverviewPage.stories.tsx` / `SettingsPage.stories.tsx`
  (探索に必要。既存の `ParticipantsPage.stories.tsx` は流用)
- `frontend/tests/vrt/__screenshots__/` — ベースライン更新(workflow_dispatch で実行)

新しい抽象は原則作らない。既存の `components/ui/` を再利用する。

### 4-3. 波及範囲の確認

`PairingTable`/`TeamPairingTable` は本計画の対象内・対象外の境界(色トークンは変更・
構造は維持)が明確なため、実装時に取り違えないよう §3-5「構造を変えない」を実装PRの
レビュー観点に明記する。それ以外のコンポーネントは各画面専用で他画面との共用がない。

## 5. 受け入れケース

本計画は挙動を変えないため、新規ケースは**改装で壊れやすい不変条件を機械検査に落とすもの**に限る
(パイロット・展開計画と同じ考え方)。既存最大値+1から採番する。

| ID | P | 受け入れ基準 | 検証手段 |
|---|---|---|---|
| TRN-AC-018 | P2 | 運営者向け組み合わせ表(PairingTable/TeamPairingTable)は意匠変更後も、スマホ表示の卓番号を共有ページの卓番号タイル(丸み・アクセントカラー地の装飾)ではなくプレーンなテキストのまま表示する(会場での即時可読性より情報密度・機能性を優先する現行判断の回帰防止) | PairingTable.test, TeamPairingTable.test(Vitest) |

- 優先度は `00_acceptance_policy.md` §4 の定義に従う。判定フローは `02_severity.md`。
  **P2**: 破られても情報が読めなくなる・操作できなくなるわけではなく、意図した意匠の
  範囲を超えて構造を変えてしまう「見た目の一貫性方針の逸脱」に留まるため(≒Minor寄りのP2)。
  Issue #122 自体の優先度もP2であることと整合させた

## 6. 更新する設計資料

- [ ] `.claude/07_plans/05_design_system_admin_internal.md` — 本計画(このPR)
- [ ] `.claude/06_adr/08_design_system_admin_internal_scope.md` — 対象範囲・PR分割・go/no-go単位のADR(このPR)
- [ ] `.claude/05_acceptance/01_acceptance_scope.md` — TRN-AC-018 を Status=todo で追加(このPR)
- [ ] `.claude/02_design_system/00_basic_design.md` — §4の「個別画面内部は対象外」を、
      「印刷用ページと`PairingTable`/`TeamPairingTable`の`MatchCard`構造(卓番号タイル)のみ
      対象外」に縮小する記述に更新(このPR)
- [ ] `.claude/02_design_system/02_component_design.md` — 「共有ページの意匠」内の
      `PairingTable` に関する既存の注記(§106「PairingTable(運営者向け)のMatchCardはこの意匠を
      適用しない」)を、対象範囲拡大後も変わらない決定として維持しつつ根拠を本ADRに更新する。
      あわせて参加者一覧表(ParticipantTable)節に、ダイアログ・組み合わせ表・設定フォーム等の
      展開結果を反映する(このPRで暫定注記、断定的な記述は**実装PR**で行う)

`schema/openapi.yaml` と `05_swiss_pairing_algorithm.md` は**更新しない**(API・マッチング・
順位計算に変更なし)。

## 7. DoD(完了の定義)

- [ ] `pnpm run check`(frontend)が通る。backendに変更がないため `./gradlew check` は対象外
- [ ] TRN-AC-018 が台帳でdoneになり、対応するテストにIDが埋まっている
- [ ] 新規画面はないためsmoke E2Eの新設は不要。既存のクリティカルパスE2E
      (参加者登録・組み合わせ生成・結果入力を含むフロー)がグリーンであることを確認する
- [ ] §6で挙げた設計資料が実装PRで更新されている
- [ ] Storybook(375px・デスクトップ両方)で動作確認済み。§3-4の単一go/no-goを人間が判断してから実装する
- [ ] `fixing-accessibility` skillを再実行し、コントラスト・フォーカス可視・タップ領域の回帰がないことを確認する
- [ ] `frontend-design` の自己批評パスとreviewerエージェントのレビューを受ける
- [ ] `scripts/vrt.sh` でベースラインを更新する。差分は対象範囲に限定され、印刷用ページ・
      `PairingTable`/`TeamPairingTable` の卓番号表示に構造変更がないことを目視確認する

## 8. リスク・未確定事項

| リスク | 対応 |
|---|---|
| **`PairingTable`/`TeamPairingTable` の改装がトークンのみに留まらず、卓番号タイルまで適用してしまう** | TRN-AC-018で機械検査を追加。§3-5の「構造を変えない」を実装PRのレビュー観点に明記した |
| **`TournamentOverviewPage` で `TournamentLayout` の見出し帯と二重のヘッダー帯ができる** | §3-5で「二重のヘッダー帯を避ける」ことを明記し、探索段階(②)で実機相当の見た目を確認する |
| **対象コンポーネント数が多く(参加者管理・チーム管理・組み合わせ表・設定の4領域)、単一go/no-goでの見落としリスクが展開計画より高い** | 探索段階でのStorybook確認を領域ごとに区切って行い(ゲート自体は単一のまま)、reviewerエージェントのレビューで領域横断の一貫性を確認する |
| **単一go/no-goで一部の画面にのみ問題が見つかった場合の切り戻し** | `06_adr/08_design_system_admin_internal_scope.md` §4の撤回条件(コミット単位の部分revert)に従う |
| **VRT差分が対象範囲全体に出て、意図しない崩れが埋もれる** | ベースライン更新前に全差分を目視確認することをDoDに明記した |

**中止・切り戻し条件**: ③の単一go/no-goでno-goの場合、対象範囲のうち最も差分が小さい
参加者管理(既にトークンの大半が適用済み)のみへスコープを縮小して再探索する。
2回目もno-goの場合は本計画をいったん中止し、現状(トークン追従のみ)を維持する。
