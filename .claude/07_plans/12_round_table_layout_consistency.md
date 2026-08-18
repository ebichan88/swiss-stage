# 12. ラウンド画面テーブルの行高・列幅統一

- Status: in_progress
- Issue: #180
- PR: #190

## 1. 背景・目的

ラウンド画面(個人戦 `PairingTable` / 団体戦 `TeamPairingTable`)のテーブル表示に、見た目の一貫性を
損なう2つの問題がある。

1. ラウンドが「確定」のときと「対局中」のときでテーブルの行の縦幅が異なる。「対局中」は結果入力用の
   `TextField select`(MUI `size="small"`)が表示される分、行が高くなっており見やすい。「確定」側は
   静的テキストのみで行が詰まっている
2. 「確定」ステータスのテーブルは `table-layout: auto`(既定)かつ列に `width` 指定がないため、
   対局者名・所属の長さに応じて各列の幅がラウンドごとに自動計算される。ラウンドタブを切り替えると
   卓・対局者1・対局者2・結果・申告ステータスの列位置がずれ、視線移動のコストになる

完了時に得られる状態: 確定・対局中どちらの状態でも行の高さが揃い、列幅がラウンドをまたいで固定される。
機能・データは変わらず、既存の見た目の粗さを直すのみ。

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**(本変更は表示のみでドメインロジックに触れない)

### スコープ

- 対象(PCテーブル): `PairingTable`(個人戦, `frontend/src/components/features/round/PairingTable.tsx`、
  `結果`列は`MatchResultControl`を使用)、`TeamPairingTable`(団体戦,
  `frontend/src/components/features/team/TeamPairingTable.tsx`)。団体戦PCテーブルのBYE行は
  `TeamPairingTable.tsx`内の直書き分岐(111〜122行目)、ボードごとの結果・申告ステータスは
  `TeamBoardResultField`・`TeamBoardStatusCell`(いずれも`TeamMatchResultControl.tsx`内で定義)を使用する
  (`TeamMatchResultControl`本体はPCテーブルでは使われていない。AI Plan Review指摘[PL1]で判明した
  当初案の誤りを修正)
- 対象(モバイルカード表示・団体戦の参加者向け自己申告ページ): `MatchResultControl`は個人戦PCテーブルの
  `結果`列とモバイルカード表示(`PairingTable.tsx`の`isMobile`分岐)の両方で共用され、
  `TeamMatchResultControl`は団体戦モバイルカード表示(`TeamPairingTable.tsx`の`isMobile`分岐)と
  参加者向け自己申告ページ`frontend/src/pages/SharedTeamResultPage.tsx`の両方で共用されている。
  これらのコンポーネントを対象外にすると、PCテーブル向けの変更(行の最小高さ統一)が意図せず
  モバイル・参加者ページにも伝播してしまう(共通コンポーネントのため)。伝播を防ぐより、一貫した
  見た目にする方が望ましいと判断し、モバイルカード表示・参加者ページも対象に含める
  (AI Plan Review指摘[PL2]を受けてユーザーに確認し決定)。ただし**列幅固定(`table-layout: fixed`)は
  表(Table)を使うPCの`PairingTable`/`TeamPairingTable`のみに適用**し、カード/インライン表示である
  モバイル・参加者ページには適用しない(そもそも列という概念がなく、ずれの問題が発生しないため)

> **Issue #180との差分(AI Plan Review指摘[PL3])**: Issue #180の「制約・やらないこと」は
> 「テーブルのレスポンシブ対応(スマホ幅での列幅・折り返し)は既存の挙動を維持し、本Issueのスコープ外
> とする」としており、本計画はこれを一部拡張している。ただし拡張したのは「モバイルカード表示・
> 参加者ページの**結果欄の最小高さ**」のみで、Issueが明示的に除外した「スマホ幅での**列幅・折り返し**」
> (列幅固定・table-layout)には手を入れない。共通コンポーネント(`MatchResultControl`/
> `TeamMatchResultControl`)を使う都合上、PCテーブル向けの行高統一だけを完全に分離することができず、
> 分離を諦めて一貫性を優先した([PL2]参照)。Issue #180側にもこの経緯をコメントで残す(本文は
> ユーザーの原文のまま変更しない)
- 個人戦・団体戦は同一パターンの実装のため、一方だけ直すと不整合が残る(ユーザー確認済み)
- 対象外: `MatchResultsTable`(対戦結果表)・`TeamMatchResultsTable`。Issueで報告された課題はラウンド画面の
  組み合わせ表のみで、対戦結果表に同様の課題は報告されていない
- ADR: 不要(`04_development_process.md` §3のいずれの条件にも該当しない。データモデル・外部サービス・
  認証方式・レイヤー構造に関わらず、単一の設計ドキュメント`02_component_design.md`に閉じる決定のため)

## 2. 画面シナリオ

- 運営者が ラウンド画面(個人戦・PC)で 確定済みラウンドのタブを開くと 対局中ラウンドと同じ行の高さで
  テーブルが表示される
- 運営者が ラウンド画面(個人戦・PC)で ラウンドタブを切り替えると 卓・対局者1・対局者2・結果・
  申告ステータスの列位置がずれずに揃って表示される
- 運営者が ラウンド画面(個人戦・PC)で 対局者名(所属含む。例:「橋本 竹子(天元クラブ)」)が列幅を
  超えて長い対局を見ると 名前は省略記号(…)で1行に収まり、ホバーするとTooltipで全文が表示される
- 運営者が ラウンド画面(個人戦・PC)で BYE(不戦勝)の対局を見ると 結果列に「不戦勝」のChipが表示され、
  行の高さは他の行と揃う(現状はBYE行だけラップ用の`Box`を経由せず、他行より低くなっている)
- 運営者が ラウンド画面(個人戦・PC)で 申告不一致・申告待ちの対局(申告ステータス列に詳細テキストが
  追加表示される)を見ると その行だけ基準の高さより伸びる(詳細テキストの行数に応じた自然な伸長は
  許容し、無理に隠さない。既存の`ReportedResultsDetail`表示は変更しない)
- 運営者が ラウンド画面(団体戦・PC)で 同様の操作をすると 個人戦と同じ基準の行高・固定列幅で表示される
- 運営者が 300名規模・複数ラウンドの大会で ラウンド画面を縦スクロールすると 行の高さ・列幅が一定のため
  表がジャンプせず読める
- 運営者が ラウンド画面(個人戦・団体戦、スマホ幅の`MatchCard`カード表示)で 確定済み対局のカードを見ると
  対局中カードと同じ最小高さで結果欄が表示される(列幅固定はカード表示には適用しない。カードはもともと
  1列レイアウトのため列ずれの問題が存在しない)
- 参加者が 団体戦の共有ページ(`SharedTeamResultPage`)で 確定済みボードの結果を見ると 他の状態と同じ
  最小高さで表示される(操作方法・送信内容は変わらず、見た目の一貫性のみの変更)

観点の抜けチェック:

- 運営者(PC・モバイル)に加え、団体戦は参加者(スマホ共有ページ`SharedTeamResultPage`)も対象にした
  (`MatchResultControl`/`TeamMatchResultControl`をPCテーブルとモバイル・参加者ページで共用しているため。
  対象外のままにすると変更が意図せず伝播することがAI Plan Review指摘[PL2]で判明し、対象に含める判断にした)
- 個人戦・団体戦の両方を対象にした(上記スコープ参照)
- グループ分けあり(複数グループ、卓番号が`A-1`形式になる)/なしのどちらでも列幅固定の挙動は変わらない
- 競合(同時操作)・権限外アクセス・無効トークンの経路: 本変更は表示のみで通信・権限を伴わないため該当なし

## 3. UI仕様

### ラウンド画面(個人戦・団体戦共通、PCテーブル表示)

- **レイアウト構成**: 既存のまま変更なし(卓 / 対局者1・対局者2(または個人戦のみ結果入力欄) /
  結果 / 申告ステータスの5列ヘッダー + データ行)
- **主要要素**: 既存のまま。列の並び・ラベル・ボタン配置は変更しない
- **4状態の見せ方**: 本変更はデータ取得状態(通常/空/ローディング/エラー)を変えない。テーブルの
  行高・列幅の統一は、通常表示状態の中の「確定」「対局中」という**ラウンドステータス**の違いに対する
  対応であり、既存のローディング・エラー・空状態(対局0件)の表示ロジックには手を入れない
- **行の高さ**: 「対局中」(結果入力用`TextField select`、`size="small"`)が実測でとる高さを基準の
  最小行高(`min-height`)とする。個人戦は`MatchResultControl`の3分岐(BYE / 非editable(確定) /
  editable(対局中))、団体戦PCテーブルは`TeamPairingTable.tsx`内のBYE行分岐 + `TeamBoardResultField`の
  2分岐(非editable / editable)+ `TeamBoardStatusCell`のそれぞれに、同じ`min-height`のコンテナを適用する
  (対象コンポーネントの詳細は§4参照)。申告ステータス列に複数行の詳細テキストが表示される行は、
  その内容量に応じて基準より伸びてよい(行の伸長自体は既存の許容挙動で、今回変えるのは
  「最小の高さを揃える」ことのみ)
- **列幅**: `Table`に`table-layout: fixed`を指定し、ヘッダー`TableCell`(またはネイティブ`colgroup`)に
  各列の固定幅を指定する。対局者1・対局者2列は氏名+所属の想定最大長で収まる幅を確保し、収まらない場合は
  `white-space: nowrap` + `text-overflow: ellipsis`で1行に収め、`Tooltip`で全文を表示する
  (対戦結果表の「相手」列がNo.表示+Tooltipで同様の考え方を採っているのと一貫)
- **大量データ時**: 300名・複数ラウンドでも行高・列幅が一定のため、`TableContainer`の縦スクロール中に
  表がガタつかない。列固定により横スクロールの発生条件は変わらない想定(既存の`overflowX: auto`のまま)
- **レスポンシブ**: 375px(モバイル)の`useMediaQuery`による分岐(`MatchCard`カード表示)自体は変更しない
  (レイアウト構成・要素の増減はなし)。ただしカード内の結果表示部分(`MatchResultControl`/
  `TeamMatchResultControl`)は行高統一の対象に含む(下記「モバイルカード表示・参加者向け自己申告ページ」
  参照)。列幅固定(`table-layout: fixed`)はデスクトップのテーブル表示のみが対象
- **既存画面との一貫性**: 個人戦`PairingTable`と団体戦`TeamPairingTable`は同一パターンの実装であり、
  同じ基準行高・列幅ルールを適用して一貫させる。対戦結果表(`MatchResultsTable`)は列幅固定パターンを
  持たないが、今回のスコープ外(§1参照)。長い氏名の省略記号+Tooltipパターンは既存の対戦結果表の
  「相手」列(Tooltip補足)と方向性を合わせる
- **新しいUIパターンの有無**: 「行の最小高さ統一」「列幅固定+省略記号+Tooltip」は`PairingTable`/
  `TeamPairingTable`にとって新しいパターンのため、実装PRで`02_component_design.md`§3
  「組み合わせ表(PairingTable)」に追記する(§6参照)

### モバイルカード表示・団体戦の参加者向け自己申告ページ(SharedTeamResultPage)

- **レイアウト構成**: 既存のまま変更なし。カードの並び・要素構成は変えない
- **主要要素**: 既存のまま
- **4状態の見せ方**: PCテーブルと同様、通常表示状態の中の結果表示の高さ統一のみで、
  ローディング・エラー・空状態のロジックには手を入れない
- **対応内容**: `MatchResultControl`/`TeamMatchResultControl`の各分岐(BYE / 非editable / editable)に、
  PCテーブルと同じ基準の`min-height`を適用する。これにより個人戦モバイルカード・団体戦モバイルカード・
  団体戦参加者ページ(`SharedTeamResultPage`)の結果表示の高さも、確定/対局中で揃う
- **列幅固定は対象外**: カード・インライン表示は列という概念を持たないため、`table-layout: fixed`等の
  対応はしない(そもそも列ずれの問題が発生しない)
- **大量データ時**: モバイルカードは1画面1対局分の情報量で、300名規模でも表示件数の増減以外の影響はない
- **既存画面との一貫性**: PCテーブルの`結果`欄と同じコンポーネント・同じ`min-height`基準を使うため、
  自然に一貫する

> 本変更は新規画面ではなく、既存画面の列・行の並びや主要要素を変えない見た目の微調整(行高・列幅の
> 固定化)であり、`04_development_process.md`§5.1の「大きなレイアウト変更」には当たらないと判断した。
> そのため`.stories.tsx`の作成(Plan PRの「コード0行」原則の例外)は適用しない

## 4. 技術設計

- **レイヤー**: フロントエンドの`components/features/round`・`components/features/team`のみ。
  domain/application/presentation(バックエンド)・infrastructureへの変更はない
- **API変更**: なし。`schema/openapi.yaml`の更新は不要
- **データモデル**: 変更なし
- **フロントエンド**:
  - `frontend/src/components/features/round/MatchResultControl.tsx`: BYE分岐・非editable分岐・
    editable分岐の3つを、同じ`min-height`を持つ共通のラッパー(`Box`の`sx`または小さな内部コンポーネント)
    に揃える。具体的な`min-height`の値は「対局中」の`TextField select`(`size="small"`、MUI標準の
    アウトライン小サイズ入力の実高さ)を実装時に実測して確定する。このコンポーネントは個人戦PCテーブルの
    `結果`列とモバイルカード表示(`PairingTable.tsx`の`isMobile`分岐)の両方から使われているため、
    1箇所の修正で両方に反映される
  - `frontend/src/components/features/round/PairingTable.tsx`: `isMobile`が`false`の分岐(PCテーブル)に
    限り`Table`に`sx={{ tableLayout: 'fixed' }}`を指定し、ヘッダー`TableCell`に列幅(`width`)を設定する。
    対局者1・対局者2セルの表示を`white-space: nowrap` + `overflow: hidden` + `text-overflow: ellipsis`にし、
    `Tooltip`で全文表示する(`playerText`が返す文字列をそのままTooltipのtitleに渡す)。`isMobile`が`true`の
    カード分岐は変更しない
  - 団体戦PCテーブル(`frontend/src/components/features/team/TeamPairingTable.tsx`): 91行目以降の
    `Table`に個人戦と同じ`table-layout: fixed`+列幅指定を適用する。`チーム1`/`チーム2`セルにも
    個人戦の対局者1・対局者2セルと同じ`white-space: nowrap` + `overflow: hidden` +
    `text-overflow: ellipsis` + `Tooltip`(`teamText`が返す文字列をtitleに渡す)を適用し、個人戦と
    崩れ方を揃える(**PL4対応**: チーム名は氏名+所属より短い想定だが、長いチーム名でも個人戦と
    異なる折り返し・はみ出しが起きないよう同じ処理で統一する)。BYE行の直書き分岐(111〜122行目)に
    上記と同じ`min-height`を適用する。ボードごとの行(132行目以降)は`TeamBoardResultField`・
    `TeamBoardStatusCell`(いずれも`TeamMatchResultControl.tsx`内で定義、非editable/editableの2分岐)に
    同じ`min-height`を適用する(**PL1修正**: 当初案は誤って`TeamMatchResultControl`本体を対象としていたが、
    PCテーブルの各行はこの2つの別コンポーネントで描画されており、`TeamMatchResultControl`本体は使われて
    いない)
  - `frontend/src/components/features/team/TeamMatchResultControl.tsx`の`TeamMatchResultControl`本体
    (BYE分岐 / 非editable分岐 / editable分岐)にも同じ`min-height`を適用する。このコンポーネントは
    団体戦モバイルカード表示(`TeamPairingTable.tsx`の`isMobile`分岐、73行目)と参加者向け自己申告ページ
    `frontend/src/pages/SharedTeamResultPage.tsx`(148行目)の両方から使われているため、1箇所の修正で
    両方に反映される
  - 列幅の値は個人戦・団体戦で列構成が異なる(団体戦はチーム名のみで所属を含まない、ボード数に応じた
    列がある)ため、共通定数ファイルには切り出さず各コンポーネント内にローカル定数として持つ
    (現時点で3箇所以上の重複がなく、共通化は時期尚早と判断)
  - 新しいテーマトークン(`theme/index.ts`)は追加しない。行高・列幅は対象コンポーネントというローカルな
    スコープに閉じた値であり、印刷帳票用の`PrintTokens`のような複数コンポーネント・複数ファイルにまたがる
    共有トークンには当たらないと判断した
- **マッチング・順位計算**: 触れない

## 5. 受け入れケース

`.claude/05_acceptance/00_acceptance_policy.md`§1.5の3条件(できることが増える/業務ルールが変わる/
破られると大会当日の運営が止まる・結果が狂う・情報が漏れる)のいずれにも該当しない
(モバイルカード表示・参加者向け自己申告ページを対象に含めても、意匠変更のみでユーザーができることも
業務ルールも変わらない点は同じ)。そのため**受け入れケース台帳への追加は該当なし**とし、以下の
回帰テスト(IDタグなし)で担保する:

- `frontend/tests/unit/components/MatchResultControl.test.tsx`: 個人戦の3分岐(BYE・確定・対局中)で
  同じ`min-height`(またはそれに準ずるスタイル)が適用されることを検証(PCテーブルの`結果`列・
  モバイルカードの両方から使われる共通コンポーネント)
- `frontend/tests/unit/components/TeamPairingTable.test.tsx`: 団体戦PCテーブルのBYE行分岐・
  `TeamBoardResultField`/`TeamBoardStatusCell`の各行で同じ`min-height`が適用されることを検証
- `frontend/tests/unit/components/TeamMatchResultControl.test.tsx`: `TeamMatchResultControl`本体の
  3分岐(BYE・確定・対局中)で同じ`min-height`が適用されることを検証(団体戦モバイルカード・
  参加者ページ双方から使われる共通コンポーネント)
- `frontend/tests/unit/pages/SharedTeamResultPage.test.tsx`: 既存テストに影響がないこと
  (`TeamMatchResultControl`の変更が参加者ページの表示・操作を壊していないこと)を回帰確認する
- `frontend/tests/unit/components/PairingTable.test.tsx` / `TeamPairingTable.test.tsx`:
  PCテーブル分岐で`table-layout: fixed`と各列の`width`が設定されていること、長い対局者名が
  Tooltipの`title`として渡っていることを検証。モバイルカード分岐には列幅指定が無いことも確認する

## 6. 更新する資料

### Plan PRで更新するもの

- [x] `.claude/07_plans/12_round_table_layout_consistency.md` — 本ファイル
- [ ] `.claude/05_acceptance/01_acceptance_scope.md` — 該当なし(§5参照。追加しない)
- [ ] `.claude/06_adr/NN_<slug>.md` — 該当なし(§1参照)
- [ ] `schema/openapi.yaml` — 該当なし(API変更なし)
- [ ] `src/pages/XxxPage.stories.tsx` — 該当なし(§3末尾参照。大きなレイアウト変更に当たらない)

### 実装PRで更新が必要な設計ドキュメント(今は更新しない・実装時の申し送り)

- [x] `.claude/02_design_system/02_component_design.md`§3「組み合わせ表(PairingTable)」—
  行の最小高さ統一・列幅固定(`table-layout: fixed`)・長い氏名の省略記号+Tooltip表示の方針を追記する

## 7. DoD(完了の定義)

- [x] `pnpm run check`(frontend)が通る
- [x] 上記の回帰テスト(§5)が追加され、通る
- [x] §6「実装PRで更新が必要な設計ドキュメント」(`02_component_design.md`)が実装PRで更新されている
- [ ] ローカル実機で動作確認済み(`.claude/skills/verify`)。個人戦・団体戦それぞれで「確定」「対局中」
  両方のラウンドを表示し、行高が揃うこと・ラウンドタブ切り替えで列位置がずれないこと・長い対局者名が
  省略されTooltipで全文が見えることを目視確認する。加えて個人戦・団体戦のモバイルカード表示
  (ブラウザ幅375px)と団体戦の参加者向け共有ページ(`SharedTeamResultPage`)でも、確定/対局中の
  結果表示の高さが揃うことを確認する
  (このセッションはサンドボックス制約でローカルブラウザ起動・localhostへのネットワークアクセスが
  できず、実施できていない。ユーザー側での目視確認が必要)
- [ ] `vrt.yml`を手動実行してVRTベースラインを更新した(行高・列幅が変わり既存スクリーンショットと
  差分が出るため。`09_test_strategy.md`)

## 8. リスク・未確定事項

- 「対局中」の`TextField select`(`size="small"`)の実高さはMUIのテーマ設定に依存するため、正確な
  `min-height`の値は実装時に実測して確定する(本プランでは方針のみを決め、px値は確定しない)
- 対局者名・所属が極端に長い場合(想定を超える長さ)、Tooltipでの全文表示に頼ることになる。既存の
  対戦結果表の「相手」列と同じ妥協点であり、新たなリスクではないと判断した
- 列幅固定により、現状よりテーブル全体の横幅が広がる可能性がある(特に対局者列を広めに確保する場合)。
  デスクトップの一般的な画面幅では収まる想定だが、実装時に狭い画面幅(タブレット横向き等)での
  横スクロール発生有無を確認する
