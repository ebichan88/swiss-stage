# 08. フォーム部品(TextField・Select・outlinedボタン)の背景色を白系に変更する

- Status: in_progress
- Issue: #158
- PR: #159(Plan PR)

---

## 1. 背景・目的

大会作成画面(`TournamentCreatePage`)をはじめとする運営者向けフォームは、`TextField`・`Select`
(実体は`TextField select`)・`Button variant="outlined"`のいずれもMUIのデフォルト(背景`transparent`)
のままで、`AppLayout`配下がPaper/Cardで囲われていないページではページ背景の生成り色
(`background.default` #F7F3EC)がそのまま透けて見える。結果として入力欄・キャンセルボタンの
境界が背景に溶け込み、入力箇所が視認しづらい(Issue #158)。

本計画は、この3種のコンポーネントの背景色を、既存のデザイントークン`background.paper`
(#FFFDF9・「紙の白」)に統一することで解決する。新しい色定義は追加しない。**フロントエンドの
テーマ(表示)変更のみ**で、バックエンド・API・データモデルには一切影響しない。

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**

## 2. 画面シナリオ

- 運営者が 大会作成画面(`TournamentCreatePage`)を開くと 大会名テキストボックス・競技/大会形式
  セレクトボックスの背景が`background.paper`(白系)になり、ページ背景との境界がはっきり見える
- 運営者が 大会作成画面で「キャンセル」ボタンを見ると 背景が`background.paper`になり、
  「作成する」(`contained`・`primary.main`塗り)との区別は保ったまま視認性が上がる
- 運営者が 参加者登録・チーム登録・大会設定など他の管理画面のフォームダイアログ(`ParticipantFormDialog`
  / `TeamFormDialog`/ `SettingsPage`等)を開いても、同じ`TextField`/`Select`/`outlined`ボタンの
  スタイルがテーマ側の変更で自動的に揃う(画面ごとの個別対応は不要)
- 運営者が 組み合わせ表(`PairingTable`/`TeamPairingTable`)の結果セレクト
  (`MatchResultControl`/`TeamMatchResultControl`)を操作しても、ゼブラストライプの行に埋もれず
  従来どおり視認できる(既存の個別`bgcolor`指定をテーマ既定への統合に置き換えるだけで、見た目は
  変化しない)
- 参加者が 共有ページ(`SharedResultPage`/`SharedTeamResultPage`)で結果選択の`outlined`ボタンを
  見ても、既存の`Card variant="outlined"`(背景は元々`background.paper`)の上に乗っているため、
  見た目・操作性ともに変化しない(回帰なし)

観点の抜けチェック:

- 運営者(PC)・参加者(スマホ・共有ページ)の両方を対象範囲に含めた(テーマ全体に一律適用する
  ため、共有ページのoutlinedボタンも対象。ただし共有ページは既存のCard背景と同色になるため
  実質的な見た目の変化は生じない見込み)
- 個人戦・団体戦で挙動差は無い(色トークンの変更のみで、両方の画面に同様に適用される)
- グループ分けあり/なしでも変わらない
- 競合・権限外アクセス: 該当なし(表示のみの変更で、APIアクセス・認可判定に触れない)
- 異常系(バリデーションエラー時のhelperText表示、フォーカス時の枠線色)は本変更で変わらない
  (背景色以外のトークンは変更しないため)

## 3. UI仕様

新規画面の追加ではなく、既存フォーム部品のテーマレベルの配色変更のため、`00_basic_design.md`
§4の優先度(4. 運営者管理画面はMUI標準に寄せてよい)に従い、レイアウト構成そのものの記述は省略する。

### 対象コンポーネントと配色

| コンポーネント | 変更前 | 変更後 |
|---|---|---|
| `TextField`(outlined、標準入力・`select`・`date`・`number`) | 背景`transparent` | 背景`background.paper`(#FFFDF9) |
| `Select`(`TextField select`経由、`MuiOutlinedInput`を内部で使用するため上記と同一の変更で対応) | 背景`transparent` | 背景`background.paper` |
| `Button variant="outlined"`(色指定なし・`color="error"`等含む全色) | 背景`transparent` | 背景`background.paper` |
| `Button variant="contained"` / `variant="text"` | 変更しない | 変更しない(Issue #158の制約どおりプライマリボタンは対象外) |

- **4状態の見せ方**: 本変更はデータ取得を伴う新規画面ではないため必須ではないが、影響確認のため記載する

  | 状態 | 表示 |
  |---|---|
  | 通常 | 背景`background.paper`、テキストは`text.primary`のまま(コントラスト比14:1超で従来より悪化しない) |
  | 無効(disabled) | `TextField`の`Mui-disabled`は文字色のみ`text.disabled`相当に薄くなる既存挙動を維持。`outlined`ボタンの`disabled`も背景`background.paper`のまま(枠線・文字が薄くなる既存の見た目で無効状態を示す)。実装後に不自然な見た目にならないかローカル実機で確認する(§8リスク参照) |
  | フォーカス | 枠線色(`primary.main`)・太さの変更は無し。背景色のみ変わる |
  | エラー | `helperText`のエラー表示・枠線の`error.main`化は変更なし |

- **大量データ時**: 影響なし(色トークンの変更のみで、件数に依存する挙動は変えない)
- **レスポンシブ**: 影響なし(375px・デスクトップともに色のみの変更)
- **既存画面との一貫性**: 組み合わせ表の結果セレクト(`MatchResultControl`/`TeamMatchResultControl`)
  は既に`sx={{ bgcolor: 'background.paper' }}`を個別指定しており、今回のテーマ変更後は同じ値が
  デフォルトになるため、この個別指定は冗長になる。重複を残さない方針とし、実装PRで該当sxを削除する
- **新しいUIパターンの有無**: 無し(新規トークン・新規コンポーネントを追加しない。既存の
  `background.paper`トークンの適用範囲をテーマの`components.styleOverrides`で広げるのみ)

## 4. 技術設計

- **レイヤーごとの変更点**: フロントエンドのみ(`frontend/src/theme/index.ts`)。
  domain/application/presentation(バックエンド)への変更は無い
- **API変更**: 無し。`schema/openapi.yaml`の更新も無い
- **データモデル**: 変更無し
- **フロントエンド**:
  - `frontend/src/theme/index.ts`の`components`に以下のstyleOverridesを追加する:
    - `MuiOutlinedInput`: `styleOverrides.root`に`backgroundColor: '#FFFDF9'`
      (`theme.palette.background.paper`と同値。`TextField`のoutlined variantおよび、それを内部で
      使う`Select`の両方に適用される。生の`<Select>`コンポーネントの直接使用は現状0件のため
      `MuiSelect`個別のoverrideは不要)
    - `MuiButton`: `styleOverrides.outlined`(MUIの`variant`別クラスキー。`color`に依らず
      `outlined`系すべてに適用される)に`backgroundColor: '#FFFDF9'`を追加。`contained`/`text`用の
      styleOverridesには触れない
  - `frontend/src/components/features/round/MatchResultControl.tsx`と
    `frontend/src/components/features/team/TeamMatchResultControl.tsx`の個別
    `sx={{ bgcolor: 'background.paper' }}`(3箇所)を削除し、テーマのデフォルトに委ねる
  - 上記以外のコンポーネントに個別のsx上書きは無いため、他ファイルの変更は不要(調査済み)
- **マッチング・順位計算**: 触れない

## 5. 受け入れケース

| ID | P | 受け入れ基準 | 検証手段 |
|---|---|---|---|
| TRN-AC-024 | P2 | `TextField`(`select`・`date`・`number`を含むoutlined variant全般)の背景色がテーマの`background.paper`(#FFFDF9)になる | Vitest |
| TRN-AC-025 | P2 | `Button variant="outlined"`(色指定に依らない)の背景色がテーマの`background.paper`になり、`variant="contained"`の配色(`primary.main`)は変更されない(回帰防止) | Vitest |
| TRN-AC-026 | P2 | 組み合わせ表の結果セレクト(`MatchResultControl`/`TeamMatchResultControl`)は、個別sx指定をテーマ既定への統合に置き換えた後もゼブラストライプの行に埋もれず表示され、結果選択・送信の既存動作に影響がない(回帰防止) | Vitest |

- 連番は台帳の`TRN`最大値(023)+1から採番
- いずれもAPI契約に変化がないUI表示のみのケースのため、`00_acceptance_policy.md` §2の規定により
  Vitest単体テストを検証手段とする(E2Eは新設しない)
- 優先度はP2(`02_severity.md`: 動作は正しいが視認性・一貫性の問題であり、大会運営を止める欠陥でも
  仕様違反でもないため)

## 6. 更新する資料

### Plan PRで更新するもの

- [x] `.claude/05_acceptance/01_acceptance_scope.md` — TRN-AC-024〜026をStatus=todoで追加
- [ ] `.claude/06_adr/NN_<slug>.md` — 対象外(`04_development_process.md` §3のいずれの条件にも
      該当しない: データモデル・外部サービス・認証方式・レイヤー構造の決定を含まず、複数の
      アーキテクチャ案を比較した決定でもなく、CLAUDE.mdの落とし穴に項目が増えるような規約でもない。
      既存トークン`background.paper`の適用範囲を広げるだけの、単一の設計ドキュメントに閉じる決定)
- [ ] `schema/openapi.yaml` — 対象外(API変更なし)

### 実装PRで更新が必要な設計ドキュメント(今は更新しない・実装時の申し送り)

- [x] `.claude/02_design_system/01_design_principles.md` — カラーパレット表の`background.paper`の
      用途欄に「フォーム部品(TextField/Select/outlinedボタン)の既定背景」を追記した
- [x] `.claude/02_design_system/02_component_design.md` — §1ボタン表の「セカンダリ」行に背景色の
      既定を明記し、§3内の「組み合わせ表の結果セレクトは`background.paper`を明示指定した」という
      記述を、個別指定ではなくテーマ既定になった旨に更新した

## 7. DoD(完了の定義)

- [ ] `pnpm run check`(frontend)が通る
- [ ] 受け入れケース(TRN-AC-024〜026)が台帳でdoneになり、対応するテストにIDが埋まっている
- [ ] 新規画面ではないため新規smoke E2Eは不要。既存のクリティカルパス(大会作成・結果入力)への
      追加検証は行わず、Vitestのみで担保する
- [ ] §6「実装PRで更新が必要な設計ドキュメント」が実装PRで更新されている
- [ ] ローカル実機で動作確認済み(`.claude/skills/verify`)。特にフォーム系の`disabled`状態の見た目
      (§3参照)と、共有ページ(`SharedResultPage`等)の結果選択ボタンに意図しない見た目の変化が
      無いことを確認する
- [ ] テーマレベルの色変更のため`vrt.yml`を手動実行してベースラインを更新した(`09_test_strategy.md`)

## 8. リスク・未確定事項

- `Button variant="outlined"`の`disabled`状態は現状「背景`transparent`のまま枠線・文字色が薄くなる」
  MUI標準挙動だが、背景を`background.paper`に固定した後も同じ見た目になるかは実装時にローカル実機で
  確認する(コントラストが下がりすぎる場合は`MuiButton`の`styleOverrides`に`&.Mui-disabled`の
  個別調整を追加することを検討する。ただし本計画のスコープでは追加調整を前提とせず、標準挙動で
  問題ないことをまず確認する)
- 共有ページ(`SharedResultPage`/`SharedTeamResultPage`)のoutlinedボタンをテーマ全体の変更に含める
  判断をしたが、これらは既に`Card`(背景`background.paper`)の内側にあるため実質的な見た目の変化は
  無い見込み。実機確認で想定外の差分(影・境界線の二重化等)が出た場合は、実装PRの中でこの2画面のみ
  除外する対応に切り替える余地を残す
