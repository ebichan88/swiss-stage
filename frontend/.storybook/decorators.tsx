import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import type { Decorator, StoryFn } from '@storybook/react-vite';

import { SnackbarProvider } from '../src/hooks/useSnackbar';
import { theme } from '../src/theme';
import type { Tournament } from '../src/types/tournament';

interface StoryProvidersProps {
  Story: StoryFn;
  route: string;
  routePath: string;
  tournament: Tournament | undefined;
}

/**
 * アプリ本体(src/main.tsx)と同じProvider構成でストーリーをラップする実体。
 * decorator本体(withProviders)から呼ぶ(react-hooks(rules-of-hooks)は
 * 大文字始まりのコンポーネント内でのみhook呼び出しを許すため、関数を分けている)。
 */
function StoryProviders({ Story, route, routePath, tournament }: StoryProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: { queries: { retry: false, refetchInterval: false } },
      }),
  );

  const page = tournament ? (
    <Route element={<Outlet context={tournament} />}>
      <Route path={routePath} element={<Story />} />
    </Route>
  ) : (
    <Route path={routePath} element={<Story />} />
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <SnackbarProvider>
          <MemoryRouter initialEntries={[route]}>
            <Routes>{page}</Routes>
          </MemoryRouter>
        </SnackbarProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}

/**
 * - `parameters.route`: MemoryRouterの初期パス(実際のURL。例 '/s/test-token')。省略時 '/'
 * - `parameters.routePath`: <Route path> に渡すパターン(例 '/s/:token')。
 *   省略時は route と同じ(パラメータを含まない画面の場合)
 * - `parameters.tournament`: useOutletContext<Tournament>() 依存ページ用に
 *   Outlet context を供給する(tests/unit/pages/SettingsPage.test.tsx と同じパターン)
 */
export const withProviders: Decorator = (Story, context) => {
  const route = (context.parameters.route as string | undefined) ?? '/';
  const routePath = (context.parameters.routePath as string | undefined) ?? route;
  const tournament = context.parameters.tournament as Tournament | undefined;

  return <StoryProviders Story={Story} route={route} routePath={routePath} tournament={tournament} />;
};
