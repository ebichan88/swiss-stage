import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { useAuth } from '../../hooks/useAuth';
import { paths } from '../../routes';
import { FullPageSpinner } from '../ui/QueryStates';

/**
 * 運営者画面の認証ガード(04_react_router_patterns.md §2)。
 * 認証状態のロード完了前にリダイレクトしない(リロード時のログイン画面飛ばされ防止)
 */
export function RequireAuth() {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <FullPageSpinner />;
  }
  if (!user) {
    return <Navigate to={paths.login} state={{ from: location }} replace />;
  }
  return <Outlet />;
}
