import { Box, Chip, MenuItem, Stack, TextField, Typography } from '@mui/material';

import type { MatchResult } from '../../../types/enums';
import type { Match } from '../../../types/round';
import {
  hasReportMismatch,
  matchReportStatus,
  matchResultText,
  reportedResultLabel,
  tableLabel,
} from './matchDisplay';

/** 自己申告状態のChip(一致待ち・不一致・確定後の食い違いのみ表示。確定済み・未申告は何も出さない) */
function ReportStatusChip({ match }: { match: Match }) {
  const status = matchReportStatus(match);
  if (status === 'WAITING') {
    return <Chip label="申告待ち" size="small" variant="outlined" />;
  }
  if (status === 'CONFLICTING') {
    return <Chip label="申告不一致" size="small" color="warning" />;
  }
  if (hasReportMismatch(match)) {
    return <Chip label="確定結果と申告が異なる" size="small" color="warning" />;
  }
  return null;
}

/**
 * 両者が具体的に何を申告したかの明示表示(誰が勝ち/負けを申告したか一目で分かるようにする)。
 * 申告が1件もない対局・確定済みで食い違いがない対局では何も出さない
 */
function ReportedResultsDetail({ match }: { match: Match }) {
  const status = matchReportStatus(match);
  const shouldShow = status === 'WAITING' || status === 'CONFLICTING' || hasReportMismatch(match);
  if (!shouldShow) {
    return null;
  }
  return (
    <Stack sx={{ mt: 0.5 }}>
      <Typography variant="caption" color="text.secondary">
        {match.player1.name}の申告: {reportedResultLabel(match, 'player1')}
      </Typography>
      <Typography variant="caption" color="text.secondary">
        {match.player2?.name ?? ''}の申告: {reportedResultLabel(match, 'player2')}
      </Typography>
    </Stack>
  );
}

export interface MatchResultControlProps {
  match: Match;
  /** ラウンド確定後・大会終了後は false(表示のみ) */
  editable: boolean;
  /** 複数グループ大会なら true(卓番号を「A-1」形式で表示) */
  multiGroup: boolean;
  saving: boolean;
  onInput: (result: MatchResult) => void;
}

/** 運営者の結果入力。BYEは自動確定のため入力不可 */
export function MatchResultControl({
  match,
  editable,
  multiGroup,
  saving,
  onInput,
}: MatchResultControlProps) {
  if (match.result === 'BYE') {
    return <Chip label="不戦勝" size="small" variant="outlined" />;
  }
  if (!editable) {
    return (
      <Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="body2">{matchResultText(match)}</Typography>
          <ReportStatusChip match={match} />
        </Box>
        <ReportedResultsDetail match={match} />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <TextField
          select
          size="small"
          value={match.result}
          onChange={(e) => onInput(e.target.value as MatchResult)}
          disabled={saving}
          // aria-label はルート要素ではなく combobox 役割を持つ表示要素に付ける
          slotProps={{
            select: {
              SelectDisplayProps: { 'aria-label': `卓${tableLabel(match, multiGroup)}の結果` },
            },
          }}
          sx={{ minWidth: 180 }}
        >
          <MenuItem value="NONE" disabled>
            未入力
          </MenuItem>
          <MenuItem value="PLAYER1_WIN">○ {match.player1.name} の勝ち</MenuItem>
          <MenuItem value="PLAYER2_WIN">○ {match.player2?.name ?? ''} の勝ち</MenuItem>
          <MenuItem value="DRAW">△ 引き分け</MenuItem>
          <MenuItem value="BOTH_LOSE">● 両者負け</MenuItem>
        </TextField>
        <ReportStatusChip match={match} />
      </Box>
      <ReportedResultsDetail match={match} />
    </Box>
  );
}
