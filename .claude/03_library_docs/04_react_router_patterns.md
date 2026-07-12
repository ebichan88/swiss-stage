# React Router v6 実装パターン集

## 1. ルート定義の標準形

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
- **Spring Boot側で「`/api/**` 以外の未マッチパスは `index.html` を返す」フォワード設定が必須**

```java
@Controller
class SpaController {
    // 拡張子なしの全パスをindex.htmlへ(静的アセットとAPIは除外される)
    @GetMapping(value = { "/{path:[^\\.]*}", "/**/{path:[^\\.]*}" })
    public String forward() {
        return "forward:/index.html";
    }
}
```

- Vite開発時は `vite.config.ts` の `server.proxy` で `/api` を `http://localhost:8080` へプロキシ

---

## 6. スクロール復元

- ページ遷移時にスクロール位置が残る → `<ScrollRestoration />` を router 配下に置く
