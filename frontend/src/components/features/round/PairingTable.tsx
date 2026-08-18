import {
  Box,
  Card,
  CardContent,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';

import type { MatchResult } from '../../../types/enums';
import type { Match } from '../../../types/round';
import type { ParticipantSummary } from '../../../types/participant';
import { MatchResultControl, ReportedResultsDetail, ReportStatusChip } from './MatchResultControl';
import { resultMark, tableLabel } from './matchDisplay';

export interface PairingTableProps {
  matches: Match[];
  editable: boolean;
  /** 複数グループ大会なら true(卓番号を「A-1」形式で表示) */
  multiGroup: boolean;
  /** 結果送信中の対局ID(該当行のみ入力を無効化) */
  savingMatchId: string | null;
  onInputResult: (match: Match, result: MatchResult) => void;
}

/** PCテーブルの固定列幅(table-layout: fixed用)。団体戦TeamPairingTableと同じ考え方でローカルに持つ */
const COLUMN_WIDTH = {
  table: 72,
  player: 260,
  result: 240,
  status: 220,
};

const ELLIPSIS_SX = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} as const;

function playerText(player: ParticipantSummary | null, mark: string | null): string {
  if (player === null) {
    return '(不戦勝)';
  }
  const name = player.organization ? `${player.name}(${player.organization})` : player.name;
  return mark ? `${mark} ${name}` : name;
}

/** 対局者名セル(PCテーブル用)。長い氏名(所属含む)は省略記号で1行に収め、Tooltipで全文表示する */
function PlayerNameCell({ text }: { text: string }) {
  return (
    <Tooltip title={text}>
      <Box component="span" sx={ELLIPSIS_SX}>
        {text}
      </Box>
    </Tooltip>
  );
}

/** 組み合わせ表(02_component_design.md §3)。PC=テーブル / スマホ=1対局1カード */
export function PairingTable({
  matches,
  editable,
  multiGroup,
  savingMatchId,
  onInputResult,
}: PairingTableProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  if (isMobile) {
    return (
      <Stack spacing={1.5}>
        {matches.map((match) => (
          <Card key={match.id} variant="outlined">
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Typography variant="h2" component="p" sx={{ minWidth: 48, textAlign: 'center' }}>
                {tableLabel(match, multiGroup)}
              </Typography>
              <Box sx={{ flexGrow: 1 }}>
                <Typography variant="body1">
                  {playerText(match.player1, resultMark(match, 'player1'))}
                </Typography>
                <Typography variant="body1">
                  {playerText(match.player2, resultMark(match, 'player2'))}
                </Typography>
                <Box sx={{ mt: 1 }}>
                  <MatchResultControl
                    match={match}
                    editable={editable}
                    multiGroup={multiGroup}
                    saving={savingMatchId === match.id}
                    onInput={(result) => onInputResult(match, result)}
                  />
                </Box>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Stack>
    );
  }

  return (
    <TableContainer component={Paper} variant="outlined" sx={{ overflowX: 'auto' }}>
      <Table size="small" sx={{ tableLayout: 'fixed' }}>
        <TableHead
          sx={{
            bgcolor: 'primary.main',
            '& .MuiTableCell-root': { color: 'primary.contrastText', fontWeight: 600 },
          }}
        >
          <TableRow>
            <TableCell scope="col" sx={{ width: COLUMN_WIDTH.table }}>
              卓
            </TableCell>
            <TableCell scope="col" sx={{ width: COLUMN_WIDTH.player }}>
              対局者1
            </TableCell>
            <TableCell scope="col" sx={{ width: COLUMN_WIDTH.player }}>
              対局者2
            </TableCell>
            <TableCell scope="col" sx={{ width: COLUMN_WIDTH.result }}>
              結果
            </TableCell>
            <TableCell scope="col" sx={{ width: COLUMN_WIDTH.status }}>
              申告ステータス
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {matches.map((match, index) => (
            <TableRow
              key={match.id}
              sx={{ bgcolor: index % 2 === 0 ? 'background.paper' : 'background.default' }}
            >
              <TableCell>{tableLabel(match, multiGroup)}</TableCell>
              <TableCell>
                <PlayerNameCell text={playerText(match.player1, resultMark(match, 'player1'))} />
              </TableCell>
              <TableCell>
                <PlayerNameCell text={playerText(match.player2, resultMark(match, 'player2'))} />
              </TableCell>
              <TableCell>
                <MatchResultControl
                  match={match}
                  editable={editable}
                  multiGroup={multiGroup}
                  saving={savingMatchId === match.id}
                  onInput={(result) => onInputResult(match, result)}
                  hideStatusChip
                />
              </TableCell>
              <TableCell>
                <ReportStatusChip match={match} />
                <ReportedResultsDetail match={match} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
