import { Table, TableBody, TableCell, TableHead, TableRow } from '@mui/material';

import { buildRosterRows } from './printRosterData';
import type { Group } from '../../../types/group';
import type { Participant } from '../../../types/participant';

export interface PrintRosterProps {
  participants: Participant[];
  groups: Group[];
}

/** 参加者名簿(印刷)。No./氏名/所属/段級位/(複数グループ大会のみ)グループ/出欠/備考(受付で手書き記入) */
export function PrintRoster({ participants, groups }: PrintRosterProps) {
  const rows = buildRosterRows(participants, groups);
  const showGroupColumn = groups.length > 1;

  return (
    <Table size="small" sx={(theme) => ({ fontSize: theme.print.tableFontSize })}>
      <TableHead sx={(theme) => ({ bgcolor: theme.print.headerBg })}>
        <TableRow>
          <TableCell sx={{ width: '40px' }}>No.</TableCell>
          <TableCell>氏名</TableCell>
          <TableCell>所属</TableCell>
          <TableCell sx={{ width: '64px' }}>段級位</TableCell>
          {showGroupColumn && <TableCell>グループ</TableCell>}
          <TableCell sx={{ width: '64px' }}>出欠</TableCell>
          <TableCell>備考</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {rows.map((row) => (
          <TableRow key={row.entryOrder}>
            <TableCell>{row.entryOrder}</TableCell>
            <TableCell>{row.name}</TableCell>
            <TableCell>{row.organization ?? ''}</TableCell>
            <TableCell>{row.rankText}</TableCell>
            {showGroupColumn && <TableCell>{row.groupName}</TableCell>}
            <TableCell />
            <TableCell />
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
