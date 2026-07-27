import { useOutletContext } from 'react-router-dom';

import type { Tournament } from '../../types/tournament';

/**
 * 配下ページから大会情報を参照するためのOutletコンテキスト。
 * `TournamentLayout` と `PrintLayout` の両方がこのcontextで `Tournament` を渡す。
 */
export function useTournamentContext(): Tournament {
  return useOutletContext<Tournament>();
}
