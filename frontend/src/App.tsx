import { createBrowserRouter, RouterProvider, ScrollRestoration, Outlet } from 'react-router-dom';

import { FullPageSpinner } from './components/ui/QueryStates';
import { paths } from './routes';

function Root() {
  return (
    <>
      <ScrollRestoration />
      <Outlet />
    </>
  );
}

// ルート定義はここに集約する(.claude/03_library_docs/04_react_router_patterns.md)。
// ルート単位でコード分割し、共有ページ(参加者のスマホ)が運営者画面のコードを
// 読み込まないようにする(14_performance_optimization.md §4)
const router = createBrowserRouter([
  {
    element: <Root />,
    // lazyルートの初回ロード中に表示する(無指定だと空白画面+警告)
    hydrateFallbackElement: <FullPageSpinner />,
    children: [
      {
        path: paths.top,
        lazy: () => import('./pages/TopPage').then((m) => ({ Component: m.TopPage })),
      },
      {
        path: paths.login,
        lazy: () => import('./pages/LoginPage').then((m) => ({ Component: m.LoginPage })),
      },
      // 共有ページ(S10/S11)はログイン不要(トークンがアクセス制御を担う)
      {
        path: '/s/:token',
        lazy: () => import('./pages/SharedPage').then((m) => ({ Component: m.SharedPage })),
      },
      {
        path: '/s/:token/matches/:mid',
        lazy: () =>
          import('./pages/SharedResultPage').then((m) => ({ Component: m.SharedResultPage })),
      },
      {
        lazy: () =>
          import('./components/layouts/RequireAuth').then((m) => ({ Component: m.RequireAuth })),
        children: [
          {
            lazy: () =>
              import('./components/layouts/AppLayout').then((m) => ({ Component: m.AppLayout })),
            children: [
              {
                path: paths.tournaments,
                lazy: () =>
                  import('./pages/TournamentListPage').then((m) => ({
                    Component: m.TournamentListPage,
                  })),
              },
              {
                path: paths.tournamentNew,
                lazy: () =>
                  import('./pages/TournamentCreatePage').then((m) => ({
                    Component: m.TournamentCreatePage,
                  })),
              },
              {
                path: '/tournaments/:id',
                lazy: () =>
                  import('./components/layouts/TournamentLayout').then((m) => ({
                    Component: m.TournamentLayout,
                  })),
                children: [
                  {
                    index: true,
                    lazy: () =>
                      import('./pages/TournamentOverviewPage').then((m) => ({
                        Component: m.TournamentOverviewPage,
                      })),
                  },
                  {
                    path: 'participants',
                    lazy: () =>
                      import('./pages/ParticipantsPage').then((m) => ({
                        Component: m.ParticipantsPage,
                      })),
                  },
                  {
                    path: 'rounds',
                    lazy: () =>
                      import('./pages/RoundsPage').then((m) => ({ Component: m.RoundsPage })),
                  },
                  {
                    path: 'standings',
                    lazy: () =>
                      import('./pages/StandingsPage').then((m) => ({
                        Component: m.StandingsPage,
                      })),
                  },
                  {
                    path: 'cross-table',
                    lazy: () =>
                      import('./pages/CrossTablePage').then((m) => ({
                        Component: m.CrossTablePage,
                      })),
                  },
                  {
                    path: 'settings',
                    lazy: () =>
                      import('./pages/SettingsPage').then((m) => ({ Component: m.SettingsPage })),
                  },
                ],
              },
            ],
          },
        ],
      },
      {
        path: '*',
        lazy: () => import('./pages/NotFoundPage').then((m) => ({ Component: m.NotFoundPage })),
      },
    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
