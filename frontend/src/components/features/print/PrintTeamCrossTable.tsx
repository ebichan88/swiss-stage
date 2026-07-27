import { Fragment } from 'react';
import { Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';

import { buildTeamCrossTableRows } from '../team/teamCrossTableData';
import { formatPoints } from '../../../utils/format';
import type { TeamRound, TeamStanding } from '../../../types/team';

export interface PrintTeamCrossTableProps {
  rounds: TeamRound[];
  standings: TeamStanding[];
}

/** ○=success/●=error。△は勝敗色分けの対象トークンが無いため無色のまま(01_design_principles.md) */
function markColor(mark: string | null): string | undefined {
  if (mark === '○') {
    return 'success.main';
  }
  if (mark === '●') {
    return 'error.main';
  }
  return undefined;
}

/**
 * 団体戦の戦績一覧表(印刷)。画面版(TeamCrossTable)と同じ `buildTeamCrossTableRows` を使うため
 * 画面と数値がズレない。個人名は含めない(チーム名のみ)。紙にホバー操作は無いためTooltipは使わない
 */
export function PrintTeamCrossTable({ rounds, standings }: PrintTeamCrossTableProps) {
  const rows = buildTeamCrossTableRows(rounds, standings);
  return (
    <>
      <Table size="small" sx={(theme) => ({ fontSize: theme.print.tableFontSize })}>
        <TableHead sx={(theme) => ({ bgcolor: theme.print.headerBg })}>
          <TableRow>
            <TableCell rowSpan={2}>No.</TableCell>
            <TableCell rowSpan={2}>チーム名</TableCell>
            {rounds.map((round) => (
              <TableCell key={round.roundNumber} align="center" colSpan={2}>
                第{round.roundNumber}
              </TableCell>
            ))}
            <TableCell rowSpan={2} align="right">
              勝点
            </TableCell>
            <TableCell rowSpan={2} align="right">
              SOS
            </TableCell>
            <TableCell rowSpan={2} align="right">
              SOSOS
            </TableCell>
            <TableCell rowSpan={2}>順位</TableCell>
          </TableRow>
          <TableRow>
            {rounds.map((round) => (
              <Fragment key={round.roundNumber}>
                <TableCell align="center">相手</TableCell>
                <TableCell align="center">結果</TableCell>
              </Fragment>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map(({ standing, cells }) => (
            <TableRow key={standing.team.id}>
              <TableCell>{standing.team.entryOrder}</TableCell>
              <TableCell>{standing.team.name}</TableCell>
              {cells.map((cell, i) => (
                <Fragment key={rounds[i].roundNumber}>
                  <TableCell align="center">
                    {cell.opponent ? cell.opponent.entryOrder : cell.isBye ? '不戦勝' : '―'}
                  </TableCell>
                  <TableCell align="center">
                    <Typography component="span" color={markColor(cell.mark)}>
                      {cell.mark ?? ''}
                      {cell.breakdown && ` ${cell.breakdown}`}
                    </Typography>
                  </TableCell>
                </Fragment>
              ))}
              <TableCell align="right">{formatPoints(standing.wins)}</TableCell>
              <TableCell align="right">{formatPoints(standing.sos)}</TableCell>
              <TableCell align="right">{formatPoints(standing.sosos)}</TableCell>
              <TableCell>{standing.rank}</TableCell>
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
