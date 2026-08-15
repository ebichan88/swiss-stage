# 12. ラウンド画面テーブルの行高・列幅統一

- Status: planned
- Issue: #180
- PR: -

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

- 対象: `PairingTable`(個人戦, `frontend/src/components/features/round/PairingTable.tsx`)、
  `MatchResultControl`(同ディレクトリ)、`TeamPairingTable`(団体戦,
  `frontend/src/components/features/team/TeamPairingTable.tsx`)、`TeamMatchResultControl`(同ディレクトリ)。
  個人戦・団体戦は同一パターンの実装のため、一方だけ直すと不整合が残る(ユーザー確認済み)
- 対象外: `MatchResultsTable`(対戦結果表)・`TeamMatchResultsTable`。Issueで報告された課題はラウンド画面の
  組み合わせ表のみで、対戦結果表に同様の課題は報告されていない
- 対象外: モバイル表示(`MatchCard`)。Issueの「制約・やらないこと」どおりレスポンシブ挙動は変更しない
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

観点の抜けチェック:

- 運営者(PC)のみが対象。参加者(スマホ共有ページ)の対局カード表示は対象外(表を使っていないため無関係)
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
  最小行高(`min-height`)として、`MatchResultControl`/`TeamMatchResultControl`の3分岐
  (BYE / 非editable(確定) / editable(対局中))すべてに同じ`min-height`のコンテナを適用する。
  申告ステータス列に複数行の詳細テキストが表示される行は、その内容量に応じて基準より伸びてよい
  (行の伸長自体は既存の許容挙動で、今回変えるのは「最小の高さを揃える」ことのみ)
- **列幅**: `Table`に`table-layout: fixed`を指定し、ヘッダー`TableCell`(またはネイティブ`colgroup`)に
  各列の固定幅を指定する。対局者1・対局者2列は氏名+所属の想定最大長で収まる幅を確保し、収まらない場合は
  `white-space: nowrap` + `text-overflow: ellipsis`で1行に収め、`Tooltip`で全文を表示する
  (対戦結果表の「相手」列がNo.表示+Tooltipで同様の考え方を採っているのと一貫)
- **大量データ時**: 300名・複数ラウンドでも行高・列幅が一定のため、`TableContainer`の縦スクロール中に
  表がガタつかない。列固定により横スクロールの発生条件は変わらない想定(既存の`overflowX: auto`のまま)
- **レスポンシブ**: 375px(モバイル)は対象外。`useMediaQuery`によるカード表示(`MatchCard`)の分岐・
  実装は変更しない。デスクトップ(テーブル表示)のみが対象
- **既存画面との一貫性**: 個人戦`PairingTable`と団体戦`TeamPairingTable`は同一パターンの実装であり、
  同じ基準行高・列幅ルールを適用して一貫させる。対戦結果表(`MatchResultsTable`)は列幅固定パターンを
  持たないが、今回のスコープ外(§1参照)。長い氏名の省略記号+Tooltipパターンは既存の対戦結果表の
  「相手」列(Tooltip補足)と方向性を合わせる
- **新しいUIパターンの有無**: 「行の最小高さ統一」「列幅固定+省略記号+Tooltip」は`PairingTable`/
  `TeamPairingTable`にとって新しいパターンのため、実装PRで`02_component_design.md`§3
  「組み合わせ表(PairingTable)」に追記する(§6参照)

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
    アウトライン小サイズ入力の実高さ)を実装時に実測して確定する
  - `frontend/src/components/features/round/PairingTable.tsx`: `Table`に`sx={{ tableLayout: 'fixed' }}`
    を指定し、ヘッダー`TableCell`に列幅(`width`)を設定する。対局者1・対局者2セルの表示を
    `white-space: nowrap` + `overflow: hidden` + `text-overflow: ellipsis`にし、`Tooltip`で全文表示する
    (`playerText`が返す文字列をそのままTooltipのtitleに渡す)
  - `frontend/src/components/features/team/TeamPairingTable.tsx` /
    `TeamMatchResultControl.tsx`: 個人戦と同じ方針を団体戦の列構成(チーム名2列 + ボードごとの結果)に
    適用する
  - 列幅の値は個人戦・団体戦で列構成が異なる(団体戦はチーム名のみで所属を含まない、ボード数に応じた
    列がある)ため、共通定数ファイルには切り出さず各コンポーネント内にローカル定数として持つ
    (現時点で3箇所以上の重複がなく、共通化は時期尚早と判断)
  - 新しいテーマトークン(`theme/index.ts`)は追加しない。行高・列幅は`PairingTable`/
    `TeamPairingTable`というローカルなスコープに閉じた値であり、印刷帳票用の`PrintTokens`のような
    複数コンポーネント・複数ファイルにまたがる共有トークンには当たらないと判断した
- **マッチング・順位計算**: 触れない

## 5. 受け入れケース

`.claude/05_acceptance/00_acceptance_policy.md`§1.5の3条件(できることが増える/業務ルールが変わる/
破られると大会当日の運営が止まる・結果が狂う・情報が漏れる)のいずれにも該当しない
(意匠変更のみで、ユーザーができることも業務ルールも変わらない)。そのため**受け入れケース台帳への
追加は該当なし**とし、以下の回帰テスト(IDタグなし)で担保する:

- `frontend/tests/unit/components/MatchResultControl.test.tsx` / `TeamMatchResultControl.test.tsx`:
  BYE・確定・対局中の3分岐で同じ`min-height`(またはそれに準ずるスタイル)が適用されることを検証
- `frontend/tests/unit/components/PairingTable.test.tsx` / `TeamPairingTable.test.tsx`:
  `table-layout: fixed`と各列の`width`が設定されていること、長い対局者名がTooltipの`title`として
  渡っていることを検証

## 6. 更新する資料

### Plan PRで更新するもの

- [x] `.claude/07_plans/12_round_table_layout_consistency.md` — 本ファイル
- [ ] `.claude/05_acceptance/01_acceptance_scope.md` — 該当なし(§5参照。追加しない)
- [ ] `.claude/06_adr/NN_<slug>.md` — 該当なし(§1参照)
- [ ] `schema/openapi.yaml` — 該当なし(API変更なし)
- [ ] `src/pages/XxxPage.stories.tsx` — 該当なし(§3末尾参照。大きなレイアウト変更に当たらない)

### 実装PRで更新が必要な設計ドキュメント(今は更新しない・実装時の申し送り)

- [ ] `.claude/02_design_system/02_component_design.md`§3「組み合わせ表(PairingTable)」—
  行の最小高さ統一・列幅固定(`table-layout: fixed`)・長い氏名の省略記号+Tooltip表示の方針を追記する

## 7. DoD(完了の定義)

- [ ] `pnpm run check`(frontend)が通る
- [ ] 上記の回帰テスト(§5)が追加され、通る
- [ ] §6「実装PRで更新が必要な設計ドキュメント」(`02_component_design.md`)が実装PRで更新されている
- [ ] ローカル実機で動作確認済み(`.claude/skills/verify`)。個人戦・団体戦それぞれで「確定」「対局中」
  両方のラウンドを表示し、行高が揃うこと・ラウンドタブ切り替えで列位置がずれないこと・長い対局者名が
  省略されTooltipで全文が見えることを目視確認する
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
