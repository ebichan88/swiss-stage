# フロントエンド設計書

## 1. ディレクトリ構造と責任

```
frontend/src/
├── components/
│   ├── ui/              # 汎用コンポーネント(MUIの薄いラッパー: AppButton, ConfirmDialog等)
│   ├── features/        # 機能別コンポーネント(tournament/, participant/, match/, standing/)
│   └── layouts/         # レイアウト(AppLayout, TournamentLayout, SharedLayout)
├── pages/               # ルーティング単位(1画面1ファイル、ロジックは持たない)
├── services/            # API通信(apiClient + リソース別service)
├── hooks/               # カスタムHooks(データ取得・状態管理)
├── types/               # 型定義(schema/openapi.yamlから生成した型のre-export)
├── utils/               # 純粋関数ユーティリティ
└── theme/               # MUIテーマ定義(デザインシステムの実装)
```

### 依存の方向(厳守)

```
pages → features → ui
  │        │
  └─▶ hooks → services → types
```

- `ui/` は `features/` や `services/` に依存しない
- `pages/` はレイアウト・データ取得Hooksの組み立てのみ。JSXが長くなったら `features/` へ切り出す

---

## 2. コンポーネント設計ルール

1. **1ファイル1コンポーネント**、200行を超えたら分割を検討
2. **propsは明示的なinterface**で定義(`XxxProps`)。propsのバケツリレーが3階層を超えたらContextを検討
3. **MUIコンポーネントは直接使ってよい**。ただしプロジェクト固有の見た目・挙動を持つものは `ui/` にラップする
   - 例: `ConfirmDialog`(破壊的操作確認)、`ResultButton`(勝敗入力ボタン)、`StatusBadge`(大会状態)
4. **ローディング/エラー/空状態を必ず実装**: 一覧系コンポーネントは「読み込み中」「エラー(再試行付き)」「0件」の3状態を持つ
5. 条件分岐が複雑な表示ロジックは Hooks or utils に切り出してテストする

---

## 3. 状態管理

| 状態の種類 | 管理方法 |
|-----------|---------|
| サーバーデータ(大会・参加者・対局) | **TanStack Query (React Query)** を採用。手書きのuseEffectフェッチ禁止 |
| フォーム状態 | React Hook Form |
| UIローカル状態(ダイアログ開閉等) | useState |
| グローバルUI状態(ログインユーザー、Snackbar) | React Context(小規模なので Redux/Zustand は導入しない) |

### TanStack Query の運用ルール

- queryKey は `['tournaments', id, 'rounds']` のように階層で統一(`hooks/queryKeys.ts` で一元管理)
- 大会当日に使う画面(共有ページ・ラウンド管理)は `refetchInterval: 30_000` でポーリング(WebSocketはMVPでは使わない)
- 更新系は mutation 成功時に関連 queryKey を invalidate する

---

## 4. データ取得パターン

```typescript
// services/tournamentService.ts — API呼び出しの実体
export async function fetchTournament(id: string): Promise<Tournament> {
  return apiClient.get<Tournament>(`/tournaments/${id}`);
}

// hooks/useTournament.ts — コンポーネントとの接続
export function useTournament(id: string) {
  return useQuery({
    queryKey: queryKeys.tournament(id),
    queryFn: () => fetchTournament(id),
  });
}
```

- コンポーネントから直接 `fetch` / `apiClient` を呼ばない(必ず services → hooks 経由)
- `apiClient` は統一レスポンス(`ApiResponse<T>`)を解釈し、失敗時 `ApiError` を throw(`06_error_handling_design.md`)

---

## 5. ルーティング(React Router v7)

- ルート定義は `App.tsx` に集約(`createBrowserRouter`)
- 認証ガード: `RequireAuth` コンポーネントで運営者画面をラップ。未認証は `/login` へリダイレクト
- 共有ページ(`/s/:token`)は認証不要の別レイアウト(`SharedLayout`)
- 詳細パターンは `.claude/03_library_docs/04_react_router_patterns.md` を参照

---

## 6. スタイリング

- MUIテーマ(`theme/index.ts`)にデザイントークンを集約(`.claude/02_design_system/` と同期)
- コンポーネント個別のスタイルは `sx` prop を使用。CSSファイルの追加は原則禁止
- **スマホファースト**: 共有ページ・結果入力は幅375pxを基準にデザインし、PCは余白で調整
- **印刷CSS(`@page`/`@media print`)も同様にCSSファイルを追加しない**。MUIの `GlobalStyles`(theme
  を受け取る関数形式)で実装する(`components/features/print/PrintGlobalStyles.tsx`)。ルート単位で1帳票=1向きが
  確定しているため、各印刷ページが自身の向きでこれを1回レンダリングする(`.claude/02_design_system/04_layout_system.md` §5)

---

## 7. Storybook(ページレベル)

**実装前にUIの合意を取り、実機で動かしてからの手戻りを防ぐための仕組み。**
`components/ui/` 単体のカタログ化はしない(過剰な保守コストを避けるため)。ページのみを対象にする。

### 対象範囲

- 新規画面、または既存画面のレイアウトを大きく変える場合に作成する
- 既存画面の小さな修正では作らない
- 対象は `.claude/02_design_system/00_basic_design.md` §4 の優先度に従う
  (共有ページ > 結果入力 > 順位表 > 運営者管理画面)

### 配置とファイル名

- `src/pages/XxxPage.stories.tsx` にコロケーション(`tests/` 配下には置かない)。
  `tsconfig.app.json` の `include: ["src"]` に自然に入り、`pnpm run type-check` で
  ストーリーの腐敗(propsの変更漏れ等)が自動検出されるため
- ページは本番と同じ経路(直import・propsなし・hooks経由)でレンダリングする。
  ストーリー専用のダミー画面は作らない

### Provider・データ

- `.storybook/decorators.tsx` の `withProviders` が `tests/testUtils.tsx` の
  `renderWithProviders` と同じProvider構成(Theme/QueryClient/Snackbar/Router)を提供する
- `useOutletContext<Tournament>()` に依存するページは `parameters.tournament` で供給する
  (`tests/unit/pages/SettingsPage.test.tsx` と同じOutlet contextパターン)
- APIモックは `msw-storybook-addon` 経由。ハンドラは `tests/msw/handlers.ts` の
  `handlersFor` に用途別(filled/empty/error等)で定義し、`tests/fixtures.ts` の
  データビルダーを再利用する。**既存のVitestテストの `server.use()` 方式は変更しない**
- ストーリー内で `new Date()` / `Date.now()` / ランダムなULID生成を使わない
  (将来のVisual Regression Testが毎回差分になるため。`tests/fixtures.ts` の固定値を使う)

### 実行

```bash
pnpm run storybook        # 開発サーバー(:6006)
pnpm run build-storybook  # 静的ビルド(storybook-static/、コミットしない)
```
