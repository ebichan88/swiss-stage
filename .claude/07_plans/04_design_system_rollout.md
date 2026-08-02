# 04. デザインシステム改装を他画面へ展開する

- Status: planned
- Issue: #119
- PR: -

---

## 1. 背景・目的

Issue #115・PR #118 で共有ページ(個人戦)をパイロットとして改装し、go判定を得た
(`.claude/07_plans/03_design_system_brushup.md`)。デザイントークン(パレット・タイポグラフィ)は
`theme/index.ts` 経由で既に全画面に自動追従しているが、**構造・コンポーネントレベルの改装**
(卓番号タイル・ヘッダー帯のような、パイロットで確立した意匠パターン)は共有ページ(個人戦)のみに
留まっている。このままでは参加者・運営者が画面を移動するたびに「和紙・墨のトーン」の画面と
「MUI既定値のまま」の画面が混在し、一貫した体験にならない。

**本計画は新しいデザイン方向性を発明しない。** パイロットで確立した意匠(卓番号タイル・
ヘッダー帯・アクセント線・碁石を想起させる要素)を、対象範囲として合意した画面へ横展開することが
目的であり、パレット・タイポグラフィの再探索は行わない。

対象範囲は `.claude/06_adr/07_design_system_rollout_scope.md` で決定した。要約すると:

- (a) `RankingBoard`/`MatchResultsTable`(個人戦)・`TeamRankingBoard`/`TeamMatchResultsTable`(団体戦)
- (b) 団体戦の共有ページ(`TeamSharedPage`)・結果自己申告ページ(`SharedTeamResultPage`)
- (c) 運営者管理画面の共通レイアウト(`TournamentLayout`)の見出し帯・ナビゲーション
  (個別画面内部は対象外。`00_basic_design.md` §4 の「機能性優先でMUI標準」を維持)

個人戦の結果入力ページ(`SharedResultPage`)はパイロットで再検討済み・撤回済みのため対象外。

完了時に得られる状態:

- パイロットで確立した意匠パターンが (a)(b)(c) に展開されている
- 展開範囲が `02_component_design.md`「共有ページの意匠」に準拠する形でドキュメント化されている
- 既存のアクセシビリティ水準(SHR-AC-018〜020含む)が展開後も維持されている
- 展開対象の画面についてVRTベースラインが更新されている

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**

意匠の都合で正確性(結果の読み違い防止)を落とさない。特に勝敗表示・順位表示は、
見た目を変えても「誤読しにくさ」が現状より悪化してはならない。

## 2. 画面シナリオ

**本計画は挙動を変更しない。** 画面シナリオは現行のままであり、以下は
「改装後も一字一句そのまま成立していること」を確認する回帰対象として列挙する。

- 運営者が PCで大会管理画面(概要〜設定)を開くと 大会名・状態バッジを含む見出し帯と
  サイドバー(md未満は下部タブ)のナビゲーションが新意匠で表示され、現在地が分かる
- 運営者が 順位メニューを開くと RankingBoard が新意匠で表示され、勝点・SOS/SOSOSは
  現行どおり読み取れる
- 運営者が 対戦結果メニューを開くと MatchResultsTable が新意匠で表示され、
  ヘッダー・ゼブラストライプのコントラストは維持される
- 運営者が 団体戦大会で順位・対戦結果メニューを開くと TeamRankingBoard/TeamMatchResultsTable
  が同じ意匠で表示される
- 参加者が 団体戦の共有URLをスマホで開くと 大会名・状態・現在ラウンドの入ったヘッダー帯と、
  個人戦と同じ卓番号タイル(チーム名版)の対局カードが表示される
- 参加者が 団体戦の対局カードで「結果入力」を押すと 結果自己申告画面へ遷移する
- 参加者が 団体戦の結果自己申告画面で どちらのチームかを選び 各ボードの結果を選ぶと
  確認ダイアログが出て、申告すると組み合わせタブへ戻る(個人戦と同じ2段階フロー)
- 参加者が 団体戦の申告済み対局を再度開くと 誰が何を申告したかが表示される(現行維持)
- 参加者が 団体戦の申告不一致のボードがある対局を開くと 警告が表示される(現行維持)
- 運営者が スマホで大会管理画面(S07ラウンド管理等)を開くと 下部タブのナビゲーションで
  画面を移動でき、タップ領域・現在地表示は変わらない

観点チェック:

- 運営者(PC/スマホ)・参加者(個人戦/団体戦・スマホ)の**すべての組み合わせ**を書いたか
- グループ分けあり/なしの分岐(`multiGroup`)は現行どおり維持する
- 個別admin画面の内部(参加者管理表・`PairingTable`・CSVインポート・設定フォーム)は
  意匠変更の対象外(`06_adr/07_design_system_rollout_scope.md`)。誤って構造を変えないこと自体が
  回帰対象になる

## 3. UI仕様

### 3-1. 本セクションの位置づけ(パイロットとの違い)

パイロット(`03_design_system_brushup.md` §3-1)は「具体的な意匠を探索して決めること自体が
タスク」だった。本計画は**既に確立した意匠パターンの横展開**であり、パレット・タイポグラフィの
トークンは再探索しない(`01_design_principles.md` の値をそのまま使う)。

探索が必要なのは、確立済みの語彙(卓番号タイル・ヘッダー帯・アクセント線)を各画面の
固有の要素(チーム名・順位バッジ・ナビゲーション項目)にどう当てはめるか、という
適用レベルの判断のみ。そのため:

1. 動かしてはいけない不変条件(§3-2。パイロットと同じ + 団体戦・管理画面固有の制約を追加)
2. 動かしてよい範囲(§3-3。新トークンは追加しない)
3. 探索の進め方と単一go/no-go(§3-4。`06_adr/07_design_system_rollout_scope.md` の決定を反映)
4. 対象コンポーネント/画面の現行構造と、変更してよい/いけない要素(§3-5)

### 3-2. 不変条件(意匠の都合で動かさない)

パイロットの不変条件(`03_design_system_brushup.md` §3-2)をすべて引き継ぐ。加えて本計画固有:

| 不変条件 | 出典 |
|---|---|
| (パイロットの不変条件をすべて引き継ぐ: 3原則・優先度・タップ領域44px・本文16px・AA・色だけに頼らない・順位表アニメーション禁止・tnum・ダークモード非対応) | `03_design_system_brushup.md` §3-2 |
| 団体戦は個人名を一切表示しない(チーム名のみ) | `04_screen_transition_design.md` §5・`02_component_design.md`「印刷帳票」 |
| `PairingTable`(運営者向け組み合わせ表)の `MatchCard` はこの展開の対象外(パイロット時の決定を維持) | `02_component_design.md`「共有ページの意匠」 |
| 個別admin画面内部(参加者管理表・設定フォーム・CSVインポート等)は機能性優先でMUI標準のまま | `00_basic_design.md` §4、`06_adr/07_design_system_rollout_scope.md` |
| ナビゲーション(サイドバー/下部タブ)の `aria-current`・キーボード操作性を壊さない | `fixing-accessibility` skill |

### 3-3. 動かしてよい範囲

**新しいトークンは追加しない。** `01_design_principles.md` に定義済みの色・タイポグラフィ・
角丸例外(ヘッダー帯24px・卓番号タイル20px)のみを使う。

- 対象コンポーネント/画面への、既存の意匠パターン(ヘッダー帯・卓番号タイル・アクセント線)の適用
- `RankingBoard`/`TeamRankingBoard` の4位以降バッジ、`MatchResultsTable`/`TeamMatchResultsTable` の
  見た目の微調整(既存トークンの範囲内)
- `TournamentLayout` の見出し帯(大会名+状態バッジ部分)・ナビゲーション選択状態の視覚強調
- `theme.components` の styleOverrides への追加(新規パターンが増える場合)

### 3-4. 探索の進め方と go/no-go

`06_adr/07_design_system_rollout_scope.md` の決定により、パイロットと異なり**単一ゲート**とする。

```
① 本計画のマージで開始
    ↓
② frontend-design で対象範囲(a)(b)(c)の意匠を一括探索(Storybook上)
   - (a)(b): 既存ストーリー(SharedPage.stories.tsx / SharedResultPage.stories.tsx)に加え、
     StandingsPage.stories.tsx / MatchResultsPage.stories.tsx / TeamStandingsPage.stories.tsx を対象にする。
     TeamSharedPage.stories.tsx / SharedTeamResultPage.stories.tsx / TeamMatchResultsPage.stories.tsx は
     未作成のため、探索前に追加する(`02_component_design.md` §5「Storybookはページレベルのみ導入」)
   - (c): TournamentLayout を含むページストーリー(RoundsPage.stories.tsx 等)で確認する
    ↓
③ 実機相当(Storybookのモバイル/デスクトップ両ビューポート)で確認 → 対象範囲まとめて go/no-go を人間が判断
    ↓ go
④ (a)(b)(c) の実装 → 全画面の追従確認
    ↓
⑤ fixing-accessibility 再実行 → frontend-design の自己批評 + reviewer で仕上げ
    ↓
⑥ VRTベースライン更新
```

**go/no-go の判断基準**(パイロットと同じ4点。`03_design_system_brushup.md` §3-4 を流用):

- 3原則(明快・堅実・信頼)が現行より強まっているか
- コンセプト(和・落ち着いた品)が既存の共有ページ(個人戦)と一貫しているか
- 会場の環境・運営者の実務利用で読めるか・操作しやすいか
- 不変条件(§3-2)を1つも破っていないか

no-goの場合は②に戻る。対象範囲の一部にのみ問題がある場合は、`06_adr/07_design_system_rollout_scope.md`
§4の撤回条件に従い、(c) 運営者管理画面から先に対象外へ戻す。

### 3-5. 対象コンポーネント/画面の現行構造

#### (a) RankingBoard / MatchResultsTable(個人戦・団体戦)

`RankingBoard`(`components/features/standing/RankingBoard.tsx`)は現行、1〜3位を
`rank.gold/silver/bronze` のメダルカード、4位以降を `background.default` 地の丸バッジ+リスト行で
表示する(構造はパイロット未着手)。`MatchResultsTable` はヘッダー `primary.main` 背景+白文字、
データ行はゼブラストライプで、パイロット以前から意匠トークンを参照している。

- **改装してよい**: 4位以降バッジの配色(既存トークンの範囲内で卓番号タイルの語彙と揃える)、
  カードの余白・罫線の使い方
- **構造を変えない**: 1〜3位メダルカード+4位以降リストという情報構造、勝点・SOS/SOSOSの列/表示位置、
  結果記号(○/●)の表示方法(SHR-AC-020 相当の不変条件)
- `TeamRankingBoard`/`TeamMatchResultsTable` も同じ範囲で同期させる(個人戦と団体戦で見た目がずれないこと)

#### (b) 団体戦の共有ページ(`TeamSharedPage.tsx`)

現行のレイアウト構成(§3-5の参照元: 本ドキュメント冒頭で読み込んだ現行コード):

```
Container(maxWidth="md", py:3)
├─ 大会名(h2/h1) + StatusBadge         ← 横並び・flexWrap(ヘッダー帯なし)
├─ 「第Nラウンド / 全Mラウンド」(body2, text.secondary)
├─ Tabs(fullWidth): 組み合わせ / 順位表 / 対戦結果
└─ タブ本体
   ├─ 組み合わせ: [ラウンド選択(2R以降)] + 案内文 + 対局カード群(グループ見出しあり)
   ├─ 順位表:     TeamRankingBoard(グループごと)
   └─ 対戦結果:   TeamMatchResultsTable(グループごと)
```

対局カード(`SharedTeamMatchCard`。このファイル内のローカルコンポーネント)の現行構造:

```
Card(variant="outlined")
└─ CardContent(flex, gap:2)
   ├─ 卓番号(h3, flexShrink:0。「◯卓」のテキストのみ、タイル装飾なし)
   ├─ 中央ブロック(flexGrow:1, minWidth:0)
   │  ├─ チーム1(body1・結果記号を前置)
   │  ├─ チーム2(body1・結果記号を前置)
   │  └─ 状態テキスト(body2・不一致/申告待ち時のみ warning.main)
   └─ [結果入力ボタン](未確定時 contained / 確定済み outlined, size="small")
```

個人戦の `SharedMatchCard`/`SharedPage.tsx`(パイロットで改装済み)とほぼ同じ構造のため、
確立済みパターンをそのまま適用できる。

- **改装してよい**: ヘッダー部(ヘッダー帯の適用)、`SharedTeamMatchCard`(卓番号タイル・
  アクセント線の適用)、空状態の見せ方、グループ見出し
- **構造を変えない**: タブ構成、チーム名のみの表示(個人名を出さない)、結果記号の併記

#### (b) 団体戦の結果自己申告ページ(`SharedTeamResultPage.tsx`)

現行構造は個人戦の `SharedResultPage.tsx` とほぼ同じ(戻るボタン+Card+2段階選択+ConfirmDialog)。
個人戦版はパイロットで探索した結果、丸バッジ案が誤読リスクで撤回され**トークン刷新のみで
コード変更なし**という結論になった(`03_design_system_brushup.md` §4-3)。団体戦版も同じ探索を行い、
同じ結論(構造変更なし)になる可能性が高いが、②の探索で個人戦版の結論をそのまま踏襲してよいかを
確認してから決める(ボード単位の選択UIがある分、個人戦より要素数が多い違いはある)。

- **改装してよい**: カード全体の見せ方(ヘッダー帯適用の要否含む)、ReportStatus相当の情報階層
- **構造を変えない**: 2段階選択(どちらのチームか → 各ボードの勝敗)+ ConfirmDialog という
  誤タップ防止フロー(`02_component_design.md` §1 の ResultButton 規定と同じ理由)

#### (c) 運営者管理画面の共通レイアウト(`TournamentLayout.tsx`)

現行構造(§4-1 で読み込んだ現行コード):

```
Box(flex)
├─ (md以上) サイドバー: List + ListItemButton(選択状態=selected props、aria-current)
├─ Container(maxWidth="lg")
│  ├─ 見出し行: 大会名(h2) + StatusBadge(ヘッダー帯なし・共有ページと非対称)
│  └─ Outlet(各画面本体)
└─ (md未満) 下部固定 BottomNavigation(6項目、aria-current)
```

- **改装してよい**: 見出し行へのヘッダー帯(`primary.light`)適用、サイドバー選択項目・
  下部タブ選択アイコンの視覚的アクセント(既存トークンの範囲内)
- **構造を変えない**: サイドバー(md以上)/下部タブ(md未満)の出し分け、6項目という項目数
  (下部タブが横スクロールなしで収まる制約、`04_screen_transition_design.md` §4)、
  `aria-current` によるページ通知、`Outlet` 配下(各画面本体)は対象外

### 3-6. 4状態・レスポンシブ・大量データ

いずれも**現行の挙動を維持する**。改装で失わないことを確認する対象として明示する。

**(a) RankingBoard/MatchResultsTable(個人戦・団体戦共通)**

| 状態 | 表示(現行・維持) |
|---|---|
| 通常 | メダルカード+リスト行 / ヘッダー+ゼブラストライプの表 |
| 空(0件) | 呼び出し元(SharedPage/StandingsPage等)がEmptyStateで出し分け(コンポーネント自体は空配列を渡されない前提を維持) |
| ローディング | 呼び出し元がFullPageSpinnerを出す(コンポーネント自体は変更なし) |
| エラー | 呼び出し元がErrorStateを出す(コンポーネント自体は変更なし) |

**(b) 団体戦の共有ページ・結果自己申告ページ**

パイロットの個人戦版(`03_design_system_brushup.md` §3-6)と同一の状態設計を維持する。
「対局が見つかりません」「締切後」の独自表示も個人戦版と同じ扱いとする。

**(c) 運営者管理画面(TournamentLayout)**

| 状態 | 表示(現行・維持) |
|---|---|
| 通常 | サイドバー(md以上)/下部タブ(md未満) |
| ローディング | FullPageSpinner(大会情報取得中) |
| エラー | ErrorState(再試行ボタン付き) |

- **レスポンシブ**: 共有ページ・結果入力は375pxを主対象(変更なし)。管理画面は
  `md`(900px)ブレークポイントでサイドバー⇔下部タブが切り替わる現行の挙動を維持する
- **大量データ時**: 300人・全ラウンドでの順位表・対戦結果表の見え方(縦リスト伸長・横スクロール)は
  パイロット同様、成立条件を壊さないこと(§3-5の「構造を変えない」範囲)

## 4. 技術設計

### 4-1. レイヤーごとの変更点

- **domain / application / infrastructure**: 変更なし
- **presentation(backend)**: 変更なし
- **API**: 変更なし(`schema/openapi.yaml` は更新しない)
- **frontend**: 以下のみ

### 4-2. 変更するファイル(実装PRでの想定。探索結果により増減しうる)

- `frontend/src/components/features/standing/RankingBoard.tsx` / `MatchResultsTable.tsx`
- `frontend/src/components/features/team/TeamRankingBoard.tsx` / `TeamMatchResultsTable.tsx`
- `frontend/src/pages/TeamSharedPage.tsx` — ヘッダー帯・`SharedTeamMatchCard` の改装
- `frontend/src/pages/SharedTeamResultPage.tsx` — ②の探索結果次第(個人戦版同様コード変更なしの可能性あり)
- `frontend/src/components/layouts/TournamentLayout.tsx` — 見出し行・ナビゲーションの改装
- 新規Storybookストーリー: `TeamSharedPage.stories.tsx` / `SharedTeamResultPage.stories.tsx` /
  `TeamMatchResultsPage.stories.tsx`(探索に必要。§3-4)
- `frontend/tests/vrt/__screenshots__/` — ベースライン更新(workflow_dispatch で実行)

新しい抽象は原則作らない。既存の `components/ui/` を再利用し、切り出しが必要な規模になった場合のみ
`02_component_design.md` に定義を先に追加する。

### 4-3. 波及範囲の確認

`RankingBoard`/`MatchResultsTable`(及び団体戦版)は共有ページ・管理画面の両方で使われるため、
確認範囲は両方にまたがる。パイロット時に指摘された「`primary.main` は表ヘッダー白文字とボタン背景を
兼ねる」制約(`03_design_system_brushup.md` §4-2)は既にトークンとして確定済みのため、
本計画では再検討しない(パレット自体は変更しないため)。

## 5. 受け入れケース

本計画は挙動を変えないため、新規ケースは**改装で壊れやすい不変条件を機械検査に落とすもの**に限る
(パイロットと同じ考え方。`03_design_system_brushup.md` §5)。既存最大値+1から採番する。

| ID | P | 受け入れ基準 | 検証手段 |
|---|---|---|---|
| SHR-AC-021 | P1 | 団体戦の共有ページ(TeamSharedPage)の対局カードは、卓番号タイル等の意匠変更後も勝敗を色だけでなく記号で併記し、個人名を出さずチーム名のみで表示する(SHR-AC-020の団体戦版・個人名非表示の回帰防止を兼ねる) | TeamSharedPage.test(Vitest) |
| RND-AC-015 | P1 | RankingBoard/TeamRankingBoardの4位以降バッジは意匠変更後も順位数字を等幅表示(tnum)し、氏名・SOS/SOSOSと視覚的に区別できる | RankingBoard.test, TeamRankingBoard.test(Vitest) |
| TRN-AC-017 | P1 | 運営者管理画面の共通レイアウト(TournamentLayout)は見出し帯・ナビゲーションの意匠変更後も、現在のページに対応するナビゲーション項目に`aria-current="page"`が設定され、キーボードのみでフォーカス移動できる | TournamentLayout.test(Vitest、新規) |

- 優先度は `00_acceptance_policy.md` §4 の定義に従う。判定フローは `02_severity.md`。
  **3件ともP1**: いずれも破られると「利用者が情報を読めない・読み違える」「操作できない」状態になり、
  仕様違反かつユーザー影響のあるバグに当たる(≒Major)。大会当日に運営が止まる・結果が狂う・
  情報が漏れるには当たらないためP0ではない
- `SHR-AC-021` は個人名非表示(既存不変条件、`04_screen_transition_design.md` §5)の回帰防止も兼ねるため、
  意匠変更(色・記号)だけでなくテキスト内容(チーム名のみ)も検証する

## 6. 更新する設計資料

- [ ] `.claude/07_plans/04_design_system_rollout.md` — 本計画(このPR)
- [ ] `.claude/06_adr/07_design_system_rollout_scope.md` — 対象範囲・go/no-go単位・PR分割のADR(このPR)
- [ ] `.claude/05_acceptance/01_acceptance_scope.md` — SHR-AC-021・RND-AC-015・TRN-AC-017 を Status=todo で追加(このPR)
- [ ] `.claude/02_design_system/00_basic_design.md` — §4の「運営者管理画面: 構造・コンポーネント改装は
      対象外」を、共通レイアウト(TournamentLayout)に限定して撤回する記述に更新(このPR)
- [ ] `.claude/02_design_system/02_component_design.md` — **実装PR**。「共有ページの意匠」節を
      団体戦・RankingBoard/MatchResultsTable・TournamentLayoutの展開内容で更新する
- [ ] `.claude/02_design_system/01_design_principles.md` — **実装PR**。新しい角丸例外・トークンの
      組み合わせが発生した場合のみ更新(新トークンの追加自体は想定していない)

`schema/openapi.yaml` と `05_swiss_pairing_algorithm.md` は**更新しない**(API・マッチング・順位計算に変更なし)。

## 7. DoD(完了の定義)

- [ ] `pnpm run check`(frontend)が通る。backendに変更がないため `./gradlew check` は対象外
- [ ] SHR-AC-021・RND-AC-015・TRN-AC-017 が台帳でdoneになり、対応するテストにIDが埋まっている
- [ ] 新規画面はないためsmoke E2Eの新設は不要。既存の共有ページE2E(`cp2-shared-mobile.spec.ts`)・
      団体戦を含むクリティカルパスE2Eがグリーンであることを確認する
- [ ] §6で挙げた設計資料が実装PRで更新されている
- [ ] Storybook(375px・デスクトップ両方)で動作確認済み。§3-4の単一go/no-goを人間が判断してから実装する
- [ ] `fixing-accessibility` skillを再実行し、コントラスト・フォーカス可視・タップ領域の回帰がないことを確認する
- [ ] `frontend-design` の自己批評パスとreviewerエージェントのレビューを受ける
- [ ] `scripts/vrt.sh` でベースラインを更新する。差分は対象範囲(a)(b)(c)に限定されることを目視確認する

## 8. リスク・未確定事項

| リスク | 対応 |
|---|---|
| **RankingBoard/MatchResultsTableは共有ページ・管理画面で共用のため、片方に最適化するともう片方が崩れる懸念** | 探索段階(②)で両方のストーリーを見比べながら判断する(§3-4) |
| **団体戦の結果自己申告ページは個人戦版と同じく「構造変更なし」に終わる可能性** | それ自体は問題ない結論として許容する(パイロットの前例と同じ)。DoDの「実装した」は「探索の結果を文書化した」ことも含む |
| **単一go/no-goで一部の画面にのみ問題が見つかった場合の切り戻し** | `06_adr/07_design_system_rollout_scope.md` §4の撤回条件(コミット単位の部分revert、(c)から先に対象外化)に従う |
| **TournamentLayoutの改装でaria-current・キーボード操作が壊れる** | TRN-AC-017で機械検査を追加。`fixing-accessibility` skillの再実行をDoDに含めた |
| **VRT差分が対象範囲全体に出て、意図しない崩れが埋もれる** | ベースライン更新前に全差分を目視確認することをDoDに明記した |

**中止・切り戻し条件**: ③の単一go/no-goでno-goの場合、対象範囲(a)(b)(c)のうち問題の少ない部分
(パイロットに最も近い(b)団体戦共有ページ)のみへスコープを縮小して再探索する。2回目もno-goの場合は
本計画をいったん中止し、トークン追従のみの状態(現状)を維持する。
