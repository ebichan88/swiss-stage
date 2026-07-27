import { Table, TableBody, TableCell, TableHead, TableRow } from '@mui/material';

import { buildRosterRows } from './printRosterData';
import type { Group } from '../../../types/group';
import type { Participant } from '../../../types/participant';

export interface PrintRosterProps {
  participants: Participant[];
  groups: Group[];
}

/** 参加者名簿(印刷)。No./氏名/所属/段級位/(複数グループ大会のみ)グループ/状態 */
export function PrintRoster({ participants, groups }: PrintRosterProps) {
  const rows = buildRosterRows(participants, groups);
  const showGroupColumn = groups.length > 1;

  return (
    <Table size="small" sx={(theme) => ({ fontSize: theme.print.tableFontSize })}>
      <TableHead sx={(theme) => ({ bgcolor: theme.print.headerBg })}>
        <TableRow>
          <TableCell>No.</TableCell>
          <TableCell>氏名</TableCell>
          <TableCell>所属</TableCell>
          <TableCell>段級位</TableCell>
          {showGroupColumn && <TableCell>グループ</TableCell>}
          <TableCell>状態</TableCell>
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
            <TableCell>{row.withdrawn ? '棄権' : ''}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
