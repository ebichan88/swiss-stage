import { Chip } from '@mui/material';

import type { RoundStatus, TournamentStatus } from '../../types/enums';
import { roundStatusLabels, tournamentStatusLabels } from '../../utils/labels';

export interface StatusBadgeProps {
  status: TournamentStatus;
}

/** 大会状態: 準備中=default / 開催中=success / 終了=default(outlined) */
export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <Chip
      label={tournamentStatusLabels[status]}
      size="small"
      color={status === 'IN_PROGRESS' ? 'success' : 'default'}
      variant={status === 'FINISHED' ? 'outlined' : 'filled'}
    />
  );
}

export interface RoundStatusBadgeProps {
  status: RoundStatus;
}

/** ラウンド状態: 組み合わせ中=warning / 対局中=info / 確定=success */
export function RoundStatusBadge({ status }: RoundStatusBadgeProps) {
  const colors = { PAIRING: 'warning', PLAYING: 'info', CONFIRMED: 'success' } as const;
  return <Chip label={roundStatusLabels[status]} size="small" color={colors[status]} />;
}
