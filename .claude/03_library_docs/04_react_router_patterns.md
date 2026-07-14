# React Router v7 実装パターン集

## 1. ルート定義の標準形

実際の `src/App.tsx` はルート単位のコード分割のため各ルートを `lazy:` で定義する
(`lazy: () => import('./pages/TopPage').then((m) => ({ Component: m.TopPage }))`)。
共有ページ(参加者のスマホ)が運営者画面のコードを読み込まないようにするのが目的
(14_performance_optimization.md §4)。初回ロード中の表示はルートルートの
`hydrateFallbackElement: <FullPageSpinner />` で指定する。以下は構造の骨子:

```tsx
// src/App.tsx — ルート定義はここに集約する
const router = createBrowserRouter([
  { path: '/', element: <TopPage /> },
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,            // 認証ガード(Outletパターン)
    children: [
      { path: '/tournaments', element: <TournamentListPage /> },
      { path: '/tournaments/new', element: <TournamentCreatePage /> },
      {
        path: '/tournaments/:id',
        element: <TournamentLayout />,   // 大会共通レイアウト
        children: [
          { index: true, element: <TournamentOverviewPage /> },
          { path: 'participants', element: <ParticipantsPage /> },
          { path: 'rounds', element: <RoundsPage /> },
          { path: 'standings', element: <StandingsPage /> },
          { path: 'settings', element: <SettingsPage /> },
        ],
      },
    ],
  },
  {
    path: '/s/:token',
    element: <SharedLayout />,           // 認証不要
    children: [
      { index: true, element: <SharedPage /> },
      { path: 'matches/:matchId', element: <SharedResultInputPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]);
```

---

## 2. 認証ガード(RequireAuth)

```tsx
export function RequireAuth() {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) return <FullPageSpinner />;   // 判定前にリダイレクトしない(重要)
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />;
  return <Outlet />;
}
```

### ハマりポイント

- **認証状態のロード完了前に `<Navigate>` を返すと、リロードのたびにログイン画面に飛ばされる**。必ず isLoading を待つ
- ログイン成功後は `location.state.from` に戻す(ディープリンク維持)

---

## 3. URLパラメータの扱い

```tsx
// パラメータは必ず存在チェック(型上は string | undefined)
const { id } = useParams();
if (!id) throw new Error('route misconfiguration');

// クエリパラメータ(タブ状態などをURLに残す)
const [searchParams, setSearchParams] = useSearchParams();
const tab = searchParams.get('tab') ?? 'pairing';
```

- タブ・フィルタ等の画面状態はできるだけURL(searchParams)に置く(リロード・共有で復元可能に)

---

## 4. ナビゲーションのルール

- 遷移は `<Link>` / `useNavigate` のみ。`window.location.href` 禁止(SPA状態が飛ぶ)
- パスの文字列組み立てを散らばらせない。`routes.ts` にパス生成関数を集約:

```typescript
export const paths = {
  tournament: (id: string) => `/tournaments/${id}`,
  rounds: (id: string) => `/tournaments/${id}/rounds`,
  shared: (token: string) => `/s/${token}`,
} as const;
```

- 保存前の離脱警告が必要なフォームは `useBlocker`(v6.19+)を使用

---

## 5. SPAのサーバー側設定(重要)

- `/tournaments/xxx` へ直アクセスすると静的サーバーは404を返す
- **Spring Boot側で「`/api/**` 以外の未マッチパスは `index.html` を返す」フォールバックが必須**
- 実装: `presentation/WebMvcConfig#addResourceHandlers`。`PathResourceResolver` を継承した
  フォールバックリゾルバで、存在しないパス(`api/` 以外)を `index.html` に解決する
  (Spring Boot 3のPathPatternParserでは `/**/{path}` 形式のコントローラマッピングが使えないため、
  リソースハンドラ方式を採用。デフォルトの静的マッピングは `spring.web.resources.add-mappings=false` で無効化)
- Cache-Control もここで設定: `/assets/**`(ハッシュ付き)= 1年 + immutable / それ以外 = no-cache
- ルート `/` は `addViewControllers` で `forward:/index.html`
- 契約テスト: `contract/SpaFallbackApiTest`(テスト用 index.html は `src/test/resources/static/`)
- Vite開発時は `vite.config.ts` の `server.proxy` で `/api` を `http://localhost:8080` へプロキシ

---

## 6. スクロール復元

- ページ遷移時にスクロール位置が残る → `<ScrollRestoration />` を router 配下に置く
