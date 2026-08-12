# 11. LoginPage/TopPageの大幅レイアウト変更

- Status: planned
- Issue: #173
- PR: -

## 1. 背景・目的

デザインシステム展開(#79 → PR #121 → PR #151)で、共有ページ・順位表・参加者管理・ラウンド管理・
設定・大会一覧など主要画面には意匠パターンが一通り適用された。一方 `LoginPage`(`/login`)と
`TopPage`(`/`)は展開対象にすら挙がっておらず、プロジェクト作成時のMUIデフォルトのまま(中央寄せの
`Box`1枚に`Typography`と`Button`が並ぶだけ)で残っている。運営者が最初に触れる画面(トップ→ログイン)
が、以降のどの画面よりも作り込まれていない状態になっており、第一印象の一貫性を欠いている。

あわせて、Plan PRの「コード0行」原則に対象画面の`.stories.tsx`のみを例外として認めるプロセス変更
(Issue #170、PR #172でマージ済み)を、実際の既存画面レイアウト変更で使う初回ケースとして検証する。

**最重要原則: 正確性 > シンプルさ > コスト > 機能 > 速度**。本プランは表示のみの変更であり、
認証ロジック・OAuth2遷移・楽観ロック等の正確性に関わる処理には一切触れない。

## 2. 画面シナリオ

- 未認証の訪問者が トップページ(`/`)を開くと Swiss Stageのロゴ・キャッチコピー・主要な特徴
  (スイス方式マッチングの自動化・結果集計と順位表示の自動化・参加者はログイン不要)・
  「運営者ログイン」ボタンが表示される
- 訪問者が トップページの「運営者ログイン」ボタンを押すと ログイン画面(`/login`)に遷移する
- 未認証の運営者が ログイン画面(`/login`)を開くと ロゴ・見出し・「Googleでログイン」ボタンが
  表示される
- 運営者が ログイン画面で「Googleでログイン」を押すと Google OAuth2の認可画面にフルページ遷移する
  (既存の `<a href="/api/v1/auth/login">` のまま、変更しない)
- OAuth認可後にエラーが返ると(`?error=oauth`) ログイン画面にエラーメッセージ(Alert)が表示され、
  再度Googleログインを試せる
- 開発ビルド(`import.meta.env.DEV`)のみ、ログイン画面に「開発用ログイン」ボタンが表示され、押すと
  バックエンドのlocalプロファイル限定エンドポイントでログインできる。本番ビルドでは表示されない
  (既存ロジックを維持)
- 認証状態のロード中は 既存の `FullPageSpinner` が表示され、ボタン等は表示されない(CLAUDE.md
  落とし穴#10「認証ロード完了前にリダイレクトしない」を維持。デザイン変更の対象外)
- 既にログイン済みの運営者が `/` または `/login` に直接アクセスすると 大会一覧(`/tournaments`)へ
  自動的にリダイレクトされる(既存の `Navigate` ロジックを維持。デザイン変更の対象外)
- 訪問者がスマホ(375px)で トップページ・ログイン画面を開くと 同じ内容が縦積みで表示され、
  読みやすいフォントサイズ・余白になる

観点の抜けチェック:

- 運営者(PC)・訪問者(スマホ)の両方を記載した(上記)
- 個人戦・団体戦、グループ分けあり/なしによる差は無い(認証前の画面のため無関係)
- 競合(2人が同時に操作)は該当しない(認証は各ユーザー独立)
- 権限外アクセス: ログイン済みユーザーの `/`・`/login` への直接アクセスは既存のリダイレクトロジックを
  維持することを明記した(再デザインで壊さないことがこのプランの制約)

## 3. UI仕様

対象は運営者管理画面カテゴリの一部だが(`00_basic_design.md` §4 では「4. 運営者管理画面」に近い)、
運営者が最初に触れる画面という性質上、他の運営者管理画面よりもう一段丁寧に作り込む。

### TopPage(`/`)

- **レイアウト構成**(上から順に):
  1. ロゴ(`/swiss-stage.svg`、64×64、角丸)
  2. サービス名(`Typography variant="h1"`)「Swiss Stage」
  3. キャッチコピー「大会といえば Swiss Stage」+ サブテキスト「囲碁・将棋大会の運営をもっと
     シンプルに」
  4. 特徴紹介(3項目、アイコン+見出し+説明文):
     - スイス方式マッチングを自動生成(再戦禁止・BYE重複禁止を守りながら組み合わせを自動作成)
     - 結果集計・順位表示を自動化(入力するだけで勝点・SOS/SOSOSに基づく順位が反映)
     - 参加者はログイン不要(共有URLをスマホで開くだけで対局・結果・順位を確認)
  5. 囲碁・将棋を連想させる装飾(`/igo.svg`・`/shogi.svg`、控えめな不透明度)
  6. CTAボタン「運営者ログイン」(`/login` へ遷移。プライマリボタンは1画面1つの原則を守る)
- **主要要素**: 見出し・キャッチコピー・特徴紹介3項目・装飾アイコン2つ・ボタン1つ(ラベル「運営者
  ログイン」)
- **4状態の見せ方**: データ取得を伴わない静的画面のため、通常状態のみ(空/ローディング/エラーは
  該当なし)
- **大量データ時**: 該当なし(静的コンテンツ)
- **レスポンシブ**: 375px・デスクトップとも `Container maxWidth="sm"` の中央寄せ1カラムを維持
  (現状と同じ)。特徴紹介はどちらの幅でも縦積み(横幅が狭いため2カラム化しない)
- **既存画面との一貫性**: ボタンの用語・配置は既存のまま(「運営者ログイン」)。特徴紹介のアイコン
  (`Autorenew`・`Leaderboard`)は既存画面で使用中のものを再利用し、新規に導入するのは
  `PhoneIphone` のみ
- **新しいUIパターンの有無**: 「認証・LP系ページの構成パターン」は `02_component_design.md` に
  前例が無い。実装PRで同ドキュメントに新設する(§4技術設計参照)

### LoginPage(`/login`)

- **レイアウト構成**(上から順に):
  1. ロゴ(`/swiss-stage.svg`、56×56。TopPageと共通の意匠)
  2. サービス名(`Typography variant="h1"`)「Swiss Stage」
  3. 見出し「運営者としてログインしてください」
  4. カード(`Paper variant="outlined"`、上端3pxの`primary.main`アクセント線。他の運営者管理画面の
     カード意匠と統一):
     - エラー時のみ `Alert severity="error"`(OAuth失敗メッセージ)
     - 「Googleでログイン」ボタン(`variant="contained" fullWidth`)
     - 開発ビルドのみ「開発用ログイン」ボタン(`variant="outlined" fullWidth`)+ 注記テキスト
- **主要要素**: 見出し・エラーAlert(条件付き)・Googleログインボタン・開発用ログインボタン
  (DEV限定)
- **4状態の見せ方**: 静的画面のため通常状態のみ。「エラー」に相当する状態はOAuth失敗時のAlert
  表示として別途ストーリーで確認する(AUTH-AC-007)
- **大量データ時**: 該当なし
- **レスポンシブ**: TopPageと同じ `Container maxWidth="sm"` を維持
- **既存画面との一貫性**: カードの意匠(`Paper variant="outlined"` + 上端アクセント線)は
  `TournamentOverviewPage`/`SettingsPage`で確立済みのパターン(`02_component_design.md`
  「個別admin画面内部の展開」)をそのまま踏襲する
- **新しいUIパターンの有無**: TopPageと同じ新設パターンを共有する

### プレースホルダーストーリー(Plan PRに含める。§5.1の例外)

`04_development_process.md` §5.1・`.claude/06_adr/12_story_first_existing_page_placeholder.md`
の決定により、既存画面の大きなレイアウト変更でも新規画面と同様にプレースホルダーを許可する
(現行の `LoginPage.tsx`/`TopPage.tsx` はまだ旧レイアウトのため、本物のページを直importしても
旧デザインしか見えないため)。

- `frontend/src/pages/TopPage.stories.tsx` — インラインの `TopPagePlaceholder` を新規作成
  (Plan PRに含む。本ファイルに完成)
- `frontend/src/pages/LoginPage.stories.tsx` — インラインの `LoginPagePlaceholder` を新規作成
  (`Default`/`OAuthError` の2状態。Plan PRに含む。本ファイルに完成)
- 実装PRで `LoginPage.tsx`/`TopPage.tsx` をこのプレースホルダーの内容に合わせて書き換え、
  ストーリーを実importへ差し替える

## 4. 技術設計

- **レイヤー**: フロントエンドのみ。`domain`/`application`/`presentation`(バックエンド)への
  変更はない
- **API変更**: なし。`schema/openapi.yaml` の更新は不要
- **データモデル**: なし
- **フロントエンド**:
  - `frontend/src/pages/TopPage.tsx` / `LoginPage.tsx` を、Plan PRのプレースホルダー
    (`TopPage.stories.tsx` / `LoginPage.stories.tsx` 内の `TopPagePlaceholder` /
    `LoginPagePlaceholder`)の内容に合わせて書き換える。既存の hooks 呼び出し(`useAuth` /
    `useTestLogin` / `useNavigate` / `useLocation` / `useSearchParams` / `useSnackbar`)・
    分岐ロジック(`isLoading` → `FullPageSpinner`、`user` → `Navigate`、`import.meta.env.DEV` →
    開発用ログインボタン表示)はそのまま維持し、レイアウト部分のみ差し替える
  - 新規コンポーネントの `components/ui/` 切り出しは行わない。2画面だけの共有であり、3箇所目の
    再利用が必要になった時点で切り出す(過剰な抽象化を避ける)
  - 新規MUIアイコン: `PhoneIphoneIcon`(TopPageの特徴紹介3点目)のみ追加。他は既存画面で使用中の
    アイコン(`AutorenewIcon`/`LeaderboardIcon`/`GoogleIcon`/`LoginIcon`)を再利用する
- **マッチング・順位計算**: 触れない

### プロセス側の技術設計(Issue #170メカニズムの補正、ADR 12)

`.claude/06_adr/12_story_first_existing_page_placeholder.md` の決定を反映し、実装PRで以下を
更新する(このプランでは更新しない。§6参照):

- `.claude/00_project/04_development_process.md` §5.1 — 「既存画面の大きなレイアウト変更では
  本物のページを直importする」という記述を、「新規画面と同じくプレースホルダーを許可し、実装PRで
  本物のページに差し替える」に修正する
- `.claude/01_development_docs/10_frontend_design.md` §7 — 同上の修正。「新規画面(0→1)の例外」を
  「新規画面・既存画面の大きなレイアウト変更の例外」に一般化する
- `.claude/commands/plan.md` 手順5 — 「既存画面の大きなレイアウト変更の場合: 本物のページを
  直importする」という分岐記述を削除し、新規画面と同じ手順に統一する
- `.claude/02_design_system/02_component_design.md` — 「認証・LP系ページの構成パターン」を新設
  (ロゴ+見出し+アクションのシンプル構成、カード意匠の踏襲)

## 5. 受け入れケース

`AUTH` プレフィックス(認証、`AuthApiTest`)を使うが、本プランはAPI契約に変化のないUI表示のみの
変更のため、`00_acceptance_policy.md` の規定どおりVitest単体テストを検証手段にする。

| ID | P | 受け入れ基準 | 検証手段 |
|---|---|---|---|
| AUTH-AC-006 | P2 | TopPageに大会運営の主要な特徴(スイス方式マッチング・結果集計/順位表示の自動化・参加者ログイン不要)が表示される | Vitest |
| AUTH-AC-007 | P2 | LoginPageでOAuth認可失敗時(`?error=oauth`)にエラーメッセージが表示され、再度Googleログインを試せる | Vitest |
| AUTH-AC-008 | P2 | 開発ビルドのみLoginPageに開発用ログインボタンが表示され、本番ビルドでは表示されない | Vitest |
| AUTH-AC-009 | P2 | 認証済みユーザーが `/` または `/login` に直接アクセスすると大会一覧へリダイレクトされる(再デザイン後も既存ロジックが壊れていないことの回帰防止) | Vitest |

- 優先度はすべてP2: 大会当日の運営停止・結果誤り・情報漏えいに該当せず(Critical該当なし)、仕様
  違反や新規のユーザー影響バグでもない(既存ロジックの回帰防止が主眼のためMajor該当なし)
- AUTH-AC-009は既存動作(変更しない部分)の回帰防止だが、レイアウト全面書き換えでリダイレクト
  ロジックの記述位置が変わりうるため、境界(ログイン済み/未ログイン)の両側を明示的にケース化した

## 6. 更新する資料

### Plan PRで更新するもの

- [x] `.claude/05_acceptance/01_acceptance_scope.md` — AUTH-AC-006〜009をStatus=todoで追加
- [x] `.claude/06_adr/12_story_first_existing_page_placeholder.md` — ADRを新規作成
      (`04_development_process.md` §3の条件2「複数案比較」に該当。既存ルールのまま/ストーリーを
      作らない/Plan PRで本物のページも書き換える、の3案を比較し却下)
- [x] `frontend/src/pages/TopPage.stories.tsx` — プレースホルダー実装を含めて新規作成
- [x] `frontend/src/pages/LoginPage.stories.tsx` — プレースホルダー実装を含めて新規作成

### 実装PRで更新が必要な設計ドキュメント(今は更新しない・実装時の申し送り)

- [ ] `frontend/src/pages/TopPage.tsx` / `LoginPage.tsx` — プレースホルダーの内容に合わせて本実装
      へ書き換え、既存hooks/ロジックはそのまま維持する
- [ ] `frontend/src/pages/TopPage.stories.tsx` / `LoginPage.stories.tsx` — プレースホルダーから
      本物のページの実importへ差し替える
- [ ] `.claude/02_design_system/02_component_design.md` — 認証・LP系ページの構成パターンを新設
- [ ] `.claude/00_project/04_development_process.md` §5.1 — ADR 12の決定を反映(既存画面の
      大きなレイアウト変更でもプレースホルダーを許可する旨に修正)
- [ ] `.claude/01_development_docs/10_frontend_design.md` §7 — 同上
- [ ] `.claude/commands/plan.md` 手順5 — 同上

## 7. DoD(完了の定義)

- [ ] `pnpm run check`(frontend)が通る。backendに変更がないため `./gradlew check` は対象外
- [ ] 受け入れケース(AUTH-AC-006〜009)が台帳で done になり、対応するVitestテストにIDが埋まっている
- [ ] 新規画面ではないため新規smoke E2Eは必須要件ではないが、既存のログインフローのE2E
      (存在する場合)がレイアウト変更後も通ることを確認する
- [ ] §6「実装PRで更新が必要な設計ドキュメント」が実装PRで更新されている
- [ ] ローカル実機で動作確認済み(`.claude/skills/verify`)。特にGoogleログインボタンの遷移先
      (`/api/v1/auth/login`)・開発用ログインボタンの本番非表示を確認する
- [ ] `vrt.yml` を手動実行してベースラインを更新した(新規レイアウトのため必須)

## 8. リスク・未確定事項

- TopPageの特徴紹介3項目の文言・アイコン選定は本プランの提案であり、実装PRのレビューで微調整が
  入る可能性がある(致命的な後戻りにはならない軽微な変更)
- 「認証・LP系ページの構成パターン」を`02_component_design.md`に新設する際、他に同種のページが
  今のところ無いため、将来別の認証関連画面(例: パスワードリセット等、現状MVPスコープ外)が
  追加されたときに再利用できる粒度で書けるか、実装PR時点で検討する
- ADR 12(既存画面レイアウト変更でもプレースホルダーを許可)自体が運用検証中の決定であり、
  今回の実装PRでの二度書き(プレースホルダー→実装)のコストが許容範囲か、次回以降の判断材料にする
