# 08. 個別admin画面内部へのデザインシステム展開スコープ

- Status: Proposed
- Issue: #122
- Date: 2026-08-02

---

## 1. 文脈

Issue #119・PR #121(`.claude/07_plans/04_design_system_rollout.md`)で、共有ページ(個人戦)の
意匠パターンを RankingBoard/TeamRankingBoard・団体戦共有ページ・運営者管理画面の共通レイアウト
(`TournamentLayout`)へ展開した。その際 `.claude/06_adr/07_design_system_rollout_scope.md` は
個別admin画面の内部(参加者管理表・`PairingTable`・設定フォーム・CSVインポート等)を明示的に
対象外とし、`00_basic_design.md` §4「運営者管理画面: 機能性優先でMUI標準に寄せてよい」を
その範囲に限って維持した。

Issue #122 は、この対象外領域を残したままだと個別admin画面だけ「MUI既定値のまま」で
他画面と見た目が揃わない状態が続く、という課題を提起した。決めるべき点は3つ
(Issue #122 本文「制約・やらないこと」):

1. **対象範囲**: どの個別admin画面・コンポーネントを展開対象にするか
2. **実装PRの分け方**: 画面・機能領域ごとに分割するか、まとめるか
3. **go/no-goの単位**: 対象が表・フォーム・ダイアログと多様になるため、単一ゲートを維持するか

加えて、`ADR 07` が明示的に据え置いた「`PairingTable` の `MatchCard` は卓番号タイル意匠を
適用しない」という決定を維持するかどうかも、本ADRの対象範囲決定に含まれる。

## 2. 決定

**対象範囲**: 個別admin画面の内部のうち、印刷用ページを除く全ての画面・コンポーネントを
展開対象に含める。

- 参加者管理: `ParticipantTable`・`ParticipantFormDialog`・`CsvImportDialog`・
  `GroupManagerDialog`(`ParticipantsPage`)
- 団体戦チーム管理: `TeamTable`・`TeamFormDialog`・`TeamMemberFormDialog`・
  `TeamMemberManagerDialog`・`TeamCsvImportDialog`(`TeamsPage`)
- 組み合わせ表: `PairingTable`・`TeamPairingTable`(`RoundsPage`・`TeamRoundsPage`)
- 大会概要: `TournamentOverviewPage`
- 大会設定: `SettingsPage`

印刷用ページ(`PrintRosterPage`・`PrintMatchCardsPage`・`PrintMatchResultsPage`とその配下の
`components/features/print/*`)は対象外とする。

`PairingTable`/`TeamPairingTable` の `MatchCard` に卓番号タイル・アクセント線などの
「会場で瞬時に読む」ための構造的意匠は**引き続き適用しない**(ADR 07 の決定を維持)。
Issue #122 自身が「タイルのような意匠まで全て持ち込む必要はない」と明記しており、
運営者はPC操作に慣れた少人数という前提(`00_basic_design.md` §4)も変わらないため。
適用してよいのは色・タイポグラフィトークン、ヘッダー帯・アクセント線のうち
ページ見出し・ナビゲーション相当の箇所に限る(詳細は `.claude/07_plans/05_design_system_admin_internal.md` §3-3)。

これにより `00_basic_design.md` §4 の「個別画面内部は対象外」は撤回し、
「印刷用ページと `PairingTable`/`TeamPairingTable` の `MatchCard` 構造(卓番号タイル)のみ対象外」に
縮小する。

**実装PR**: 対象範囲をまとめて1本とする(ADR 07 と同じ判断)。

**go/no-goの単位**: 対象が表・フォーム・ダイアログと多様になるが、単一ゲートを維持する。
理由は決定3を参照。

## 3. 却下した案

- **機能領域ごとに複数の実装PRへ分割する案**(参加者管理/チーム管理/組み合わせ表/設定、の
  ように分ける): レビュー・切り戻しの粒度は上がるが、対象範囲の大半は既存トークン(`primary.main`
  ヘッダー・ゼブラストライプ)が既に適用済みで差分は小さく、機能領域をまたいで一貫性を目視確認する
  手間が領域数分かかる。ADR 07 と同じ判断(パターンが既に確立済みで差分が予測しやすい)がここでも
  成り立つため、まとめて1PRとした
- **画面・コンポーネントごとに個別go/no-goゲートを設ける案**: 表・フォーム・ダイアログと
  コンポーネント種別は増えるが、いずれも「確立済みトークンの適用」であり、パイロットのような
  新規探索を伴わない。種別ごとにゲートを設けるコストに見合わないと判断した(ADR 07 と同じ理由)
- **`PairingTable`/`TeamPairingTable` の `MatchCard` にも卓番号タイルを適用する案**: 展開対象を
  完全に統一できるが、Issue #122 自身が明示的に不要としており、会場での即時可読性より
  情報密度・機能性を優先するという運営者管理画面の既存判断(`00_basic_design.md` §4)とも矛盾する。
  タイル化は行数の多い表(参加者数・対局数が多い大会)で視覚的なノイズになるリスクもある
- **印刷用ページも対象に含める案**: 個別admin画面という括りでは同じだが、印刷は紙面上の
  レイアウト制約(色を使わない前提、`components/features/print/*` の既存設計)が画面の意匠展開とは
  性質が異なる。混ぜると印刷可読性の検証観点が増え、本Issueの目的(画面の見た目の一貫性)から
  逸れるため対象外とした

## 4. 結果

**得たもの**: 印刷用ページを除く全ての画面で、パイロットが確立した意匠トークン(色・
ヘッダー帯・アクセント線)が一貫して適用される。実装・レビューコストは単一PRで完結する。

**引き受けたトレードオフ**: 単一ゲート・単一PRのため、対象範囲のどこかで問題が見つかった場合、
範囲全体の切り戻し判断が必要になる(ADR 07 と同じトレードオフ)。`PairingTable`/`TeamPairingTable`
の `MatchCard` だけ意匠が異なる(タイルなし)状態が今後も残るため、「なぜここだけ違うのか」を
`02_component_design.md` に明記しておく必要がある。

**撤回条件**: 一括go/no-goのレビューで対象範囲の一部にのみ問題が見つかった場合、その画面・
コンポーネントの変更のみコミット単位でrevertする(ADR 07 §4 と同じ運用)。
