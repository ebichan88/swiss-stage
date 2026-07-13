import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';

import type { Standing } from '../../../types/standing';
import { formatPoints } from '../../../utils/format';

export interface StandingsTableProps {
  standings: Standing[];
}

/**
 * 順位表(02_component_design.md §3)。上位3位は行背景で強調。
 * SOS/SOSOS はスマホでは非表示(横スクロールでページを壊さない)
 */
export function StandingsTable({ standings }: StandingsTableProps) {
  const detailCellSx = { display: { xs: 'none', sm: 'table-cell' } };
  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>順位</TableCell>
            <TableCell>氏名(所属)</TableCell>
            <TableCell align="right">勝点</TableCell>
            <TableCell align="right">敗数</TableCell>
            <TableCell align="right" sx={detailCellSx}>
              SOS
            </TableCell>
            <TableCell align="right" sx={detailCellSx}>
              SOSOS
            </TableCell>
            <TableCell sx={detailCellSx}>不戦勝</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {standings.map((standing) => (
            <TableRow
              key={standing.participant.id}
              sx={standing.rank <= 3 ? { bgcolor: 'primary.light' } : undefined}
            >
              <TableCell>
                <Typography variant="h3" component="span">
                  {standing.rank}
                </Typography>
              </TableCell>
              <TableCell>
                {standing.participant.name}
                {standing.participant.organization && (
                  <Typography variant="body2" color="text.secondary" component="span">
                    ({standing.participant.organization})
                  </Typography>
                )}
              </TableCell>
              <TableCell align="right">{formatPoints(standing.wins)}</TableCell>
              <TableCell align="right">{standing.losses}</TableCell>
              <TableCell align="right" sx={detailCellSx}>
                {formatPoints(standing.sos)}
              </TableCell>
              <TableCell align="right" sx={detailCellSx}>
                {formatPoints(standing.sosos)}
              </TableCell>
              <TableCell sx={detailCellSx}>{standing.hadBye ? 'あり' : ''}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
