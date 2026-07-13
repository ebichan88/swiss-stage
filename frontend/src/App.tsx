import { createBrowserRouter, RouterProvider, ScrollRestoration, Outlet } from 'react-router-dom';

import { AppLayout } from './components/layouts/AppLayout';
import { RequireAuth } from './components/layouts/RequireAuth';
import { TournamentLayout } from './components/layouts/TournamentLayout';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { ParticipantsPage } from './pages/ParticipantsPage';
import { RoundsPage } from './pages/RoundsPage';
import { SettingsPage } from './pages/SettingsPage';
import { StandingsPage } from './pages/StandingsPage';
import { TopPage } from './pages/TopPage';
import { TournamentCreatePage } from './pages/TournamentCreatePage';
import { TournamentListPage } from './pages/TournamentListPage';
import { TournamentOverviewPage } from './pages/TournamentOverviewPage';
import { paths } from './routes';

function Root() {
  return (
    <>
      <ScrollRestoration />
      <Outlet />
    </>
  );
}

// ルート定義はここに集約する(.claude/03_library_docs/04_react_router_patterns.md)
const router = createBrowserRouter([
  {
    element: <Root />,
    children: [
      { path: paths.top, element: <TopPage /> },
      { path: paths.login, element: <LoginPage /> },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppLayout />,
            children: [
              { path: paths.tournaments, element: <TournamentListPage /> },
              { path: paths.tournamentNew, element: <TournamentCreatePage /> },
              {
                path: '/tournaments/:id',
                element: <TournamentLayout />,
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
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
