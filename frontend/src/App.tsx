import { createBrowserRouter, RouterProvider } from 'react-router-dom';

import { NotFoundPage } from './pages/NotFoundPage';
import { TopPage } from './pages/TopPage';
import { paths } from './routes';

// ルート定義はここに集約する(.claude/03_library_docs/04_react_router_patterns.md)
const router = createBrowserRouter([
  { path: paths.top, element: <TopPage /> },
  { path: '*', element: <NotFoundPage /> },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
