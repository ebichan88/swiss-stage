import { Fragment } from 'react';
import { Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';

import { buildPrintMatchResultsTableRows } from './printMatchResultsTableData';
import { writableGridSx } from './printSx';
import type { Participant } from '../../../types/participant';

export interface PrintMatchResultsTableProps {
  participants: Participant[];
  totalRounds: number;
}

/**
 * 対戦結果表(印刷)。大会開始前に印刷し、ラウンドの進行に合わせて対戦相手・結果・勝点・SOS・SOSOS・
 * 順位を手書きで記入する運用のため、これらの列は常に空欄で出力する。ラウンド列は生成済みラウンド数に
 * よらず totalRounds 分を最初からすべて出す
 */
export function PrintMatchResultsTable({ participants, totalRounds }: PrintMatchResultsTableProps) {
  const rows = buildPrintMatchResultsTableRows(participants);
  const rounds = Array.from({ length: totalRounds }, (_, i) => i + 1);

  return (
    <>
      <Table
        size="small"
        sx={[(theme) => ({ fontSize: theme.print.tableFontSize }), writableGridSx]}
      >
        <TableHead sx={(theme) => ({ bgcolor: theme.print.headerBg })}>
          <TableRow>
            <TableCell rowSpan={2}>No.</TableCell>
            <TableCell rowSpan={2}>名前</TableCell>
            <TableCell rowSpan={2}>段級位</TableCell>
            {rounds.map((round) => (
              <TableCell key={round} colSpan={2}>
                {round}回戦
              </TableCell>
            ))}
            <TableCell rowSpan={2}>勝点</TableCell>
            <TableCell rowSpan={2}>SOS</TableCell>
            <TableCell rowSpan={2}>SOSOS</TableCell>
            <TableCell rowSpan={2}>順位</TableCell>
          </TableRow>
          <TableRow>
            {rounds.map((round) => (
              <Fragment key={round}>
                <TableCell>相手</TableCell>
                <TableCell>結果</TableCell>
              </Fragment>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow
              key={row.entryOrder}
              sx={(theme) => ({ height: theme.print.writableRowHeight })}
            >
              <TableCell>{row.entryOrder}</TableCell>
              <TableCell>{row.name}</TableCell>
              <TableCell>{row.rankText}</TableCell>
              {rounds.map((round) => (
                <Fragment key={round}>
                  <TableCell />
                  <TableCell />
                </Fragment>
              ))}
              <TableCell />
              <TableCell />
              <TableCell />
              <TableCell />
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Typography variant="caption" sx={{ mt: 1, display: 'block' }}>
        SOS = 対戦相手の勝点の合計 / SOSOS = 対戦相手のSOSの合計
      </Typography>
    </>
  );
}
