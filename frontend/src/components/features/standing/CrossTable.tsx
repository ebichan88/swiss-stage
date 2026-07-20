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

import { buildCrossTableRows } from './crossTableData';
import { formatPoints } from '../../../utils/format';
import { rankLabel } from '../../../utils/labels';
import type { ParticipantSummary } from '../../../types/participant';
import type { Round } from '../../../types/round';
import type { Standing } from '../../../types/standing';

export interface CrossTableProps {
  rounds: Round[];
  standings: Standing[];
}

function opponentLabel(participant: ParticipantSummary): string {
  return participant.organization
    ? `${participant.name}(${participant.organization})`
    : participant.name;
}

/**
 * 戦績一覧表(参加者×ラウンドの対戦相手・結果を1画面に集約)。
 * 相手列は氏名の代わりにNo.(entryOrder)を表示し、Tooltipで氏名を補足する
 */
export function CrossTable({ rounds, standings }: CrossTableProps) {
  const rows = buildCrossTableRows(rounds, standings);
  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell rowSpan={2}>No.</TableCell>
            <TableCell rowSpan={2}>氏名(所属)</TableCell>
            <TableCell rowSpan={2}>段級位</TableCell>
            {rounds.map((round) => (
              <TableCell key={round.roundNumber} align="center" colSpan={2}>
                第{round.roundNumber}ラウンド
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
            <TableRow key={standing.participant.id}>
              <TableCell>{standing.participant.entryOrder}</TableCell>
              <TableCell>
                {standing.participant.name}
                {standing.participant.organization && (
                  <Typography variant="body2" color="text.secondary" component="span">
                    ({standing.participant.organization})
                  </Typography>
                )}
              </TableCell>
              <TableCell>{rankLabel(standing.participant.rank)}</TableCell>
              {cells.map((cell, i) => (
                <Fragment key={rounds[i].roundNumber}>
                  <TableCell align="center">
                    {cell.opponent ? (
                      <Tooltip title={opponentLabel(cell.opponent)}>
                        <span>{cell.opponent.entryOrder}</span>
                      </Tooltip>
                    ) : cell.isBye ? (
                      '不戦勝'
                    ) : (
                      '―'
                    )}
                  </TableCell>
                  <TableCell align="center">{cell.mark ?? ''}</TableCell>
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
