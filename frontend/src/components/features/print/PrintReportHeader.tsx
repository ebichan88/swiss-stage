import { Box, Typography } from '@mui/material';

import { formatEventDate } from '../../../utils/format';

export interface PrintReportHeaderProps {
  tournamentName: string;
  eventDate: string | null;
  reportTitle: string;
  /** 複数グループ大会のときのみ渡す(単一グループ大会は見出しを省略する) */
  groupName?: string | null;
}

/** 全帳票共通のヘッダー(大会名・帳票名・開催日・グループ名) */
export function PrintReportHeader({
  tournamentName,
  eventDate,
  reportTitle,
  groupName,
}: PrintReportHeaderProps) {
  return (
    <Box sx={{ mb: 2 }}>
      <Typography variant="h2" component="h1">
        {tournamentName}
      </Typography>
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'baseline',
          flexWrap: 'wrap',
          gap: 1,
        }}
      >
        <Typography variant="h3" component="h2">
          {reportTitle}
          {groupName ? `(${groupName})` : ''}
        </Typography>
        <Typography variant="body2">開催日: {formatEventDate(eventDate)}</Typography>
      </Box>
    </Box>
  );
}
