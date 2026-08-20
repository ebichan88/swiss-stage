# 16. モーダル(Dialog)内側余白のテーマ側底上げ

- Status: in_progress
- Issue: #193
- PR: #195

---

## 1. 背景・目的

`Dialog`/`DialogTitle`/`DialogContent`/`DialogActions` を使うモーダル8件が MUI v9 のデフォルト
paddingのまま実装されており、以下2点により「メッセージやボタンがモーダルの端に寄りすぎている」
印象を与えている(Issue #193、`GroupManagerDialog` のスクリーンショットで指摘)。

- `DialogTitle` の直後に `DialogContent` が続くと、MUIの隣接セレクタ規則
  (`.MuiDialogTitle-root + &`)で `DialogContent` の上paddingが **0** にリセットされる。
  ヘッダーを `bgcolor: primary.main` で色付けしている7件で特に、色境界の直下に本文が
  余白なしで始まる継ぎ目が目立つ
- `DialogActions`(ボタン行)は上下左右すべて **8px** のみで、同じダイアログ内の
  `DialogContent`(20px上/24px左右)と比べて狭く、ボタンが下・左右の端に寄って見える

完了時に得たい状態: `theme/index.ts` に `MuiDialogTitle`/`MuiDialogContent`/`MuiDialogActions`
の components override を追加し、8件のモーダルすべてで縦横とも `.claude/02_design_system/01_design_principles.md`
の「コンポーネント内部の余白: 16px」という既存トークンを下回らないようにする。個別ファイルの
`sx` 上書きは行わない(CLAUDE.md 落とし穴#7「色・余白のハードコード禁止」に反するため)。

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度** — 本変更は表示のみの調整であり、
マッチング・順位計算・データには一切触れない。

## 2. 画面シナリオ

対象は特定の1画面ではなく、モーダルを開く8つの箇所すべてに共通するテーマ側の変更である。
個別の画面シナリオが増減するわけではない(できることは変わらない)ため、代表シナリオのみ挙げる。

```
- 運営者が 参加者一覧画面で「グループ管理」を開くと(GroupManagerDialog) ヘッダー直下の説明文・
  下部ボタン行がそれぞれ16px以上の余白を保って表示される
- 運営者が 破壊的操作(削除等)の確認ダイアログ(ConfirmDialog)を開くと 「キャンセル」「削除する」
  ボタンが左右・下端から16px以上離れて表示される
- 参加者が 共有結果入力ページ(SharedResultPage/SharedTeamResultPage、スマホ・ログイン不要)で
  結果を申告しようとすると(ConfirmDialog)「結果を申告しますか?」のメッセージ・「申告する」
  ボタンが16px以上の余白を保って表示される
```

異常系・競合・権限外アクセスの経路は本変更で変化しない(表示調整のみのため対象外)。

## 3. UI仕様

### 対象: 全8モーダル共通の `Dialog` 内側余白

- **現状の実測値**(MUI v9 デフォルト、`node_modules/@mui/material/Dialog{Title,Content,Actions}`
  のソースで確認済み):
  | 要素 | 現状 |
  |---|---|
  | `DialogTitle` | 上下16px / 左右24px |
  | `DialogContent`(`DialogTitle` 直後) | 上**0px**(隣接セレクタでリセット) / 左右24px / 下24px |
  | `DialogActions` | 上下左右すべて**8px** |
- **変更後の目標値**(`01_design_principles.md` の「コンポーネント内部の余白: 16px」に合わせる):
  | 要素 | 変更後 |
  |---|---|
  | `DialogTitle` | 変更なし(既に16px/24pxで基準を満たす) |
  | `DialogContent` | 上**16px**(`DialogTitle` 直後でも0にリセットさせない) / 左右24px / 下24px |
  | `DialogActions` | 上下左右すべて**16px**(左右は`DialogContent`の24pxと完全一致まではしないが、
    16px以上を保証しつつボタン間隔の詰まりすぎを避ける) |
- **4状態の見せ方**: 本変更はローディング・エラー・空状態の表示内容を変更しない
  (対象外。トークンのみの変更)
- **大量データ時**: `GroupManagerDialog`/`TeamMemberManagerDialog` の一覧が伸びる場合も
  `DialogContent` は元々 `overflow-y: auto` でスクロールする挙動のまま変わらない
- **レスポンシブ**: 375px〜デスクトップの両方で同じpadding値を適用する。対象8モーダルには
  `GroupManagerDialog` 等の運営者管理画面向け(`00_basic_design.md` §4「4. 運営者管理画面」)だけ
  でなく、`ConfirmDialog` のように参加者向け共有ページ(同§4「1. 共有ページ(スマホ)」=最優先)
  でも使われるものが含まれる。いずれの区分でも padding のブレークポイント出し分けは不要と判断した
  (16px以上という基準はスマホ幅でも狭すぎない値であり、375px〜デスクトップで崩れる要素がないため)
- **既存画面との一貫性**: `.claude/02_design_system/01_design_principles.md` の「コンポーネント
  内部の余白: 16px」「カード内: 16px」と同じ基準に揃える。新しいUIパターンではなく、既存
  トークンの適用漏れを埋める変更
- **新規画面・大きなレイアウト変更に該当するか**: 該当しない。構造・要素・ボタン配置は一切変更せず
  paddingの数値のみを底上げするため、`04_development_process.md` §5.1.1 の
  「新規画面・大きなレイアウト変更」の対象外と判断し、`.stories.tsx` は作成しない

## 4. 技術設計

### フロントエンドのみの変更(レイヤー: frontend/src/theme)

- `frontend/src/theme/index.ts` の `components` に以下を追加する:
  - `MuiDialogContent.styleOverrides.root`: `DialogTitle` 直後でも上paddingが0にリセットされない
    よう明示的に上paddingを指定する(`.MuiDialogTitle-root + &` セレクタとの詳細度の関係で
    上書きされない場合は、実装時にブラウザで実際に効いているか確認し、必要なら
    `[.MuiDialogTitle-root + &]` を明示的に上書きするセレクタを追加する)
  - `MuiDialogActions.styleOverrides.root`: `padding: 16px` に統一する
- 個別の8ファイル(`GroupManagerDialog.tsx` 等)は変更しない。既存の `DialogTitle` の
  `bgcolor: primary.main` 等のsxはそのまま維持する
- API変更なし。データモデル変更なし。マッチング・順位計算に触れない

### 影響範囲の確認

`grep` で確認済み: `src/theme/index.ts` に既存の `MuiDialog*` override は無く、各ダイアログ側にも
`DialogContent`/`DialogActions` のpaddingを上書きする `sx` は無い(全8ファイル)。よって
テーマ側の追加のみで完結し、既存の個別実装との衝突は無い。

## 5. 受け入れケース

**該当なし。** `.claude/05_acceptance/00_acceptance_policy.md` §1.5 の3条件
(できることが増える/業務ルールが変わる/破られると大会運営が止まる・結果が狂う・情報が漏れる)の
いずれにも当たらない「意匠変更のみ」のため、受け入れケースは追加しない。

代わりに、`frontend/tests/unit/theme/theme.test.ts` に既存の回帰防止テスト(`MuiOutlinedInput`/
`MuiButton` のstyleOverridesをID無しで検証している既存パターンと同型)を1件追加し、
`theme.components.MuiDialogActions.styleOverrides.root.padding` 等の値をID無しでassertする
(実装PRで追加。台帳の更新は無いためPlan PRでは追加しない)。

| ID | P | 受け入れ基準 | 検証手段 |
|---|---|---|---|
| (該当なし) | - | - | - |

## 6. 更新する資料

### Plan PRで更新するもの

- [ ] 本ファイル(`.claude/07_plans/16_dialog_spacing_theme_override.md`)のみ。
      受け入れケース追加なし・ADR不要・API変更なし・新規画面/大きなレイアウト変更なしのため、
      他に追加更新するファイルは無い

### 実装PRで更新が必要な設計ドキュメント(今は更新しない・実装時の申し送り)

- [x] `.claude/02_design_system/02_component_design.md` §「個別admin画面内部の展開」—
      ダイアログの余白トークン適用について1文追記する
- [x] `frontend/tests/unit/theme/theme.test.ts` — 上記の回帰防止テストを追加

## 7. DoD(完了の定義)

- [x] `pnpm run check`(frontend)が通る
- [x] `theme.test.ts` に回帰防止テストが追加されている(受け入れケースIDは無し)
- [x] §6「実装PRで更新が必要な設計ドキュメント」が実装PRで更新されている
- [x] ローカル実機で `GroupManagerDialog`(Issue #193のスクリーンショット対象)・その中の
      `ConfirmDialog`(自動振り分け確認)・`SharedResultPage` の `ConfirmDialog`(375px幅)を
      Playwrightで開いてスクリーンショット確認済み。テーマ変更前は8モーダル共通の同一パターン
      (MuiDialog系のデフォルトpadding)だったため、この3点の確認で他5件にも同じ効果が及ぶことを
      確認とした
- [x] 新規画面ではないため smoke E2E の追加は不要。既存の `pnpm run test`(Vitest)がグリーンで
      あることを確認済み(E2Eフルスイートは本変更専用には実行していない)

## 8. リスク・未確定事項

- **CSS詳細度のリスク**: `DialogContent` の上paddingを0にリセットする `.MuiDialogTitle-root + &`
  セレクタは、テーマの `styleOverrides.root` よりCSS詳細度が高くなる可能性がある。理論値だけで
  判断せず、実装時に実際のDOMで `getComputedStyle` を確認し、効いていなければセレクタを明示的に
  合わせて上書きする
- **受け入れケース非該当の判断**: 「意匠変更のみ」という判断はAIの提案であり、
  `04_development_process.md` §2 の「受け入れケースの追加・変更・廃止の判断は人間のみが行う」に
  従い、本Plan PRのレビューで最終確認を受ける
- 対象8モーダルの一覧は Issue #193 で洗い出し済みのものをそのまま踏襲する(新規モーダルの追加調査は行わない)
