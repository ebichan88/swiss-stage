import { Fragment } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';

import { buildTeamMatchResultsTableRows } from './teamMatchResultsTableData';
import { TableHeaderTooltip } from '../../ui/TableHeaderTooltip';
import { formatPoints } from '../../../utils/format';
import type { TeamRound, TeamStanding } from '../../../types/team';

export interface TeamMatchResultsTableProps {
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
 * チームの対戦結果表(チーム×ラウンドの対戦相手・結果を1画面に集約)。個人名は含めない。
 * 相手列は氏名の代わりにNo.(entryOrder)を表示し、Tooltipでチーム名を補足する。
 * 結果列はマーク(○/●/△)に加えボード内訳(例: 2-1)を併記する
 */
export function TeamMatchResultsTable({ rounds, standings }: TeamMatchResultsTableProps) {
  const rows = buildTeamMatchResultsTableRows(rounds, standings);
  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead
          sx={{
            bgcolor: 'primary.main',
            '& .MuiTableCell-root': { color: 'primary.contrastText', fontWeight: 600 },
          }}
        >
          <TableRow>
            <TableCell rowSpan={2} scope="col">
              No.
            </TableCell>
            <TableCell rowSpan={2} scope="col">
              チーム名
            </TableCell>
            {rounds.map((round) => (
              <TableCell key={round.roundNumber} align="center" colSpan={2} scope="colgroup">
                第{round.roundNumber}ラウンド
              </TableCell>
            ))}
            <TableCell rowSpan={2} align="right" scope="col">
              勝点
            </TableCell>
            <TableCell rowSpan={2} align="right" scope="col">
              <TableHeaderTooltip
                label="SOS"
                tooltip="Sum of Opponents' Scores：対戦相手の勝点を合計したもの"
              />
            </TableCell>
            <TableCell rowSpan={2} align="right" scope="col">
              <TableHeaderTooltip
                label="SOSOS"
                tooltip="Sum of Opponents' SOS：対戦相手の SOS を合計したもの"
              />
            </TableCell>
            <TableCell rowSpan={2} scope="col">
              順位
            </TableCell>
          </TableRow>
          <TableRow>
            {rounds.map((round) => (
              <Fragment key={round.roundNumber}>
                <TableCell align="center" scope="col">
                  相手
                </TableCell>
                <TableCell align="center" scope="col">
                  結果
                </TableCell>
              </Fragment>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map(({ standing, cells }, index) => (
            <TableRow
              key={standing.team.id}
              sx={{ bgcolor: index % 2 === 0 ? 'background.paper' : 'background.default' }}
            >
              <TableCell>{standing.team.entryOrder}</TableCell>
              <TableCell>{standing.team.name}</TableCell>
              {cells.map((cell, i) => (
                <Fragment key={rounds[i].roundNumber}>
                  <TableCell align="center">
                    {cell.opponent ? (
                      <Tooltip title={cell.opponent.name}>
                        <span>{cell.opponent.entryOrder}</span>
                      </Tooltip>
                    ) : cell.isBye ? (
                      '不戦勝'
                    ) : (
                      '―'
                    )}
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
    </TableContainer>
  );
}
