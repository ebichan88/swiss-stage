import { Box, Chip, MenuItem, Stack, TextField, Typography } from '@mui/material';

import type { MatchResult } from '../../../types/enums';
import type { BoardResult, TeamMatch } from '../../../types/team';
import { boardPositionLabel } from '../../../utils/labels';
import {
  boardHasReportMismatch,
  boardReportedResultLabel,
  boardReportStatus,
  boardResultText,
  teamTableLabel,
} from './teamMatchDisplay';

/** 自己申告状態のChip(一致待ち・不一致・確定後の食い違いのみ表示。確定済み・未申告は何も出さない) */
function BoardReportStatusChip({ board }: { board: BoardResult }) {
  const status = boardReportStatus(board);
  if (status === 'WAITING') {
    return <Chip label="申告待ち" size="small" variant="outlined" />;
  }
  if (status === 'CONFLICTING') {
    return <Chip label="申告不一致" size="small" color="warning" />;
  }
  if (boardHasReportMismatch(board)) {
    return <Chip label="確定結果と申告が異なる" size="small" color="warning" />;
  }
  return null;
}

/** 両者が具体的に何を申告したかの明示表示(誰が勝ち/負けを申告したか一目で分かるようにする) */
function BoardReportedResultsDetail({ match, board }: { match: TeamMatch; board: BoardResult }) {
  const status = boardReportStatus(board);
  const shouldShow =
    status === 'WAITING' || status === 'CONFLICTING' || boardHasReportMismatch(board);
  if (!shouldShow) {
    return null;
  }
  return (
    <Stack sx={{ mt: 0.5, mb: 0.5 }}>
      <Typography variant="caption" color="text.secondary">
        {match.team1.name}の申告: {boardReportedResultLabel(match, board, 'team1')}
      </Typography>
      <Typography variant="caption" color="text.secondary">
        {match.team2?.name ?? ''}の申告: {boardReportedResultLabel(match, board, 'team2')}
      </Typography>
    </Stack>
  );
}

export interface TeamMatchResultControlProps {
  match: TeamMatch;
  /** ラウンド確定後・大会終了後は false(表示のみ) */
  editable: boolean;
  /** 複数グループ大会なら true(卓番号を「A-1」形式で表示) */
  multiGroup: boolean;
  saving: boolean;
  /** ボード配列をまとめて送信する(1ボードの変更でも全ボード分の配列を渡す) */
  onInput: (boardResults: MatchResult[]) => void;
  hideStatusChip?: boolean;
}

/** 運営者の主将戦・副将戦…の結果一括入力。BYEは自動確定のため入力不可 */
export function TeamMatchResultControl({
  match,
  editable,
  multiGroup,
  saving,
  onInput,
  hideStatusChip = false,
}: TeamMatchResultControlProps) {
  if (match.team2 === null) {
    return <Chip label="不戦勝" size="small" variant="outlined" />;
  }

  if (!editable) {
    return (
      <Stack spacing={0.5}>
        {match.boardResults.map((board) => (
          <Box key={board.boardPosition}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="body2" sx={{ minWidth: 56 }}>
                {boardPositionLabel(board.boardPosition)}
              </Typography>
              <Typography variant="body2">{boardResultText(match, board)}</Typography>
              {!hideStatusChip && <BoardReportStatusChip board={board} />}
            </Box>
            <BoardReportedResultsDetail match={match} board={board} />
          </Box>
        ))}
      </Stack>
    );
  }

  const handleChange = (index: number, value: MatchResult) => {
    onInput(match.boardResults.map((b, i) => (i === index ? value : b.result)));
  };

  return (
    <Stack spacing={0.5}>
      {match.boardResults.map((board, index) => (
        <Box key={board.boardPosition}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" sx={{ minWidth: 56 }}>
              {boardPositionLabel(board.boardPosition)}
            </Typography>
            <TextField
              select
              size="small"
              value={board.result}
              onChange={(e) => handleChange(index, e.target.value as MatchResult)}
              disabled={saving}
              slotProps={{
                select: {
                  SelectDisplayProps: {
                    'aria-label': `卓${teamTableLabel(match, multiGroup)} ${boardPositionLabel(board.boardPosition)}の結果`,
                  },
                },
              }}
              sx={{ minWidth: 180 }}
            >
              <MenuItem value="NONE" disabled>
                未入力
              </MenuItem>
              <MenuItem value="PLAYER1_WIN">○ {match.team1.name}の勝ち</MenuItem>
              <MenuItem value="PLAYER2_WIN">○ {match.team2?.name ?? ''}の勝ち</MenuItem>
              <MenuItem value="DRAW">△ 引き分け</MenuItem>
              <MenuItem value="BOTH_LOSE">● 両者負け</MenuItem>
            </TextField>
            {!hideStatusChip && <BoardReportStatusChip board={board} />}
          </Box>
          <BoardReportedResultsDetail match={match} board={board} />
        </Box>
      ))}
    </Stack>
  );
}
