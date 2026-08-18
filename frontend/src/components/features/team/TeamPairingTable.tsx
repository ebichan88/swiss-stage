import {
  Box,
  Card,
  CardContent,
  Chip,
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
import type { TeamMatch, TeamSummary } from '../../../types/team';
import {
  TeamBoardResultField,
  TeamBoardStatusCell,
  TeamMatchResultControl,
} from './TeamMatchResultControl';
import { teamResultMark, teamTableLabel } from './teamMatchDisplay';

export interface TeamPairingTableProps {
  matches: TeamMatch[];
  editable: boolean;
  /** 複数グループ大会なら true(卓番号を「A-1」形式で表示) */
  multiGroup: boolean;
  /** 結果送信中の対局ID(該当行のみ入力を無効化) */
  savingMatchId: string | null;
  onInputResult: (match: TeamMatch, boardResults: MatchResult[]) => void;
}

/**
 * 結果欄の最小行高。「対局中」のTextField select(size="small"、MUI標準アウトライン小サイズ)の
 * 実高さ(40px)に、BYE時のChipを揃えるための基準値。
 * frontend/src/components/features/round/MatchResultControl.tsx と同じ値
 */
const RESULT_MIN_HEIGHT = 40;

/** PCテーブルの固定列幅(table-layout: fixed用)。個人戦PairingTableと同じ考え方でローカルに持つ */
const COLUMN_WIDTH = {
  table: 72,
  team: 220,
  // 「対局中」のTeamBoardResultField(ラベルminWidth:56 + gap:8 + TextField minWidth:180 = 244px)
  // + TableCellの左右padding(16px*2)が収まる幅
  result: 280,
  status: 220,
};

const ELLIPSIS_SX = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} as const;

function teamText(team: TeamSummary | null, mark: string | null): string {
  if (team === null) {
    return '(不戦勝)';
  }
  return mark ? `${mark} ${team.name}` : team.name;
}

/** チーム名セル(PCテーブル用)。長いチーム名は省略記号で1行に収め、Tooltipで全文表示する */
function TeamNameCell({ text }: { text: string }) {
  return (
    <Tooltip title={text}>
      <Box component="span" sx={ELLIPSIS_SX}>
        {text}
      </Box>
    </Tooltip>
  );
}

/** 団体戦の組み合わせ表(02_component_design.md §3)。PC=テーブル / スマホ=1対局1カード */
export function TeamPairingTable({
  matches,
  editable,
  multiGroup,
  savingMatchId,
  onInputResult,
}: TeamPairingTableProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  if (isMobile) {
    return (
      <Stack spacing={1.5}>
        {matches.map((match) => (
          <Card key={match.id} variant="outlined">
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Typography variant="h2" component="p" sx={{ minWidth: 48, textAlign: 'center' }}>
                {teamTableLabel(match, multiGroup)}
              </Typography>
              <Box sx={{ flexGrow: 1 }}>
                <Typography variant="body1">
                  {teamText(match.team1, teamResultMark(match, 'team1'))}
                </Typography>
                <Typography variant="body1">
                  {teamText(match.team2, teamResultMark(match, 'team2'))}
                </Typography>
                <Box sx={{ mt: 1 }}>
                  <TeamMatchResultControl
                    match={match}
                    editable={editable}
                    multiGroup={multiGroup}
                    saving={savingMatchId === match.id}
                    onInput={(boardResults) => onInputResult(match, boardResults)}
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
            <TableCell scope="col" sx={{ width: COLUMN_WIDTH.team }}>
              チーム1
            </TableCell>
            <TableCell scope="col" sx={{ width: COLUMN_WIDTH.team }}>
              チーム2
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
          {matches.map((match, index) => {
            const rowBg = index % 2 === 0 ? 'background.paper' : 'background.default';
            const boards = match.boardResults;

            if (match.team2 === null || boards.length === 0) {
              return (
                <TableRow key={match.id} sx={{ bgcolor: rowBg }}>
                  <TableCell>{teamTableLabel(match, multiGroup)}</TableCell>
                  <TableCell>
                    <TeamNameCell text={teamText(match.team1, teamResultMark(match, 'team1'))} />
                  </TableCell>
                  <TableCell>
                    <TeamNameCell text={teamText(match.team2, teamResultMark(match, 'team2'))} />
                  </TableCell>
                  <TableCell>
                    <Box
                      sx={{ display: 'flex', alignItems: 'center', minHeight: RESULT_MIN_HEIGHT }}
                    >
                      <Chip label="不戦勝" size="small" variant="outlined" />
                    </Box>
                  </TableCell>
                  <TableCell />
                </TableRow>
              );
            }

            const handleBoardChange = (boardIndex: number, value: MatchResult) => {
              onInputResult(
                match,
                match.boardResults.map((b, i) => (i === boardIndex ? value : b.result)),
              );
            };

            return boards.map((board, boardIndex) => (
              <TableRow key={`${match.id}-${board.boardPosition}`} sx={{ bgcolor: rowBg }}>
                {boardIndex === 0 && (
                  <>
                    <TableCell rowSpan={boards.length}>
                      {teamTableLabel(match, multiGroup)}
                    </TableCell>
                    <TableCell rowSpan={boards.length}>
                      <TeamNameCell text={teamText(match.team1, teamResultMark(match, 'team1'))} />
                    </TableCell>
                    <TableCell rowSpan={boards.length}>
                      <TeamNameCell text={teamText(match.team2, teamResultMark(match, 'team2'))} />
                    </TableCell>
                  </>
                )}
                <TableCell>
                  <TeamBoardResultField
                    match={match}
                    board={board}
                    boardIndex={boardIndex}
                    editable={editable}
                    multiGroup={multiGroup}
                    saving={savingMatchId === match.id}
                    onChange={handleBoardChange}
                  />
                </TableCell>
                <TableCell>
                  <TeamBoardStatusCell match={match} board={board} />
                </TableCell>
              </TableRow>
            ));
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
