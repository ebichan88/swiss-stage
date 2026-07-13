import { Box, Typography } from '@mui/material';
import type { ReactNode } from 'react';

export interface EmptyStateProps {
  icon: ReactNode;
  message: string;
  /** 次のアクションボタン(02_component_design.md §3: アイコン+一文+アクションの3点セット) */
  action?: ReactNode;
}

export function EmptyState({ icon, message, action }: EmptyStateProps) {
  return (
    <Box sx={{ py: 8, textAlign: 'center', color: 'text.secondary' }}>
      <Box sx={{ fontSize: 48, mb: 1 }}>{icon}</Box>
      <Typography variant="body1" gutterBottom>
        {message}
      </Typography>
      {action && <Box sx={{ mt: 2 }}>{action}</Box>}
    </Box>
  );
}
