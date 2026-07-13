import { Chip, MenuItem, TextField, Typography } from '@mui/material';

import type { MatchResult } from '../../../types/enums';
import type { Match } from '../../../types/round';
import { matchResultText } from './matchDisplay';

export interface MatchResultControlProps {
  match: Match;
  /** ラウンド確定後・大会終了後は false(表示のみ) */
  editable: boolean;
  saving: boolean;
  onInput: (result: MatchResult) => void;
}

/** 運営者の結果入力。BYEは自動確定のため入力不可 */
export function MatchResultControl({ match, editable, saving, onInput }: MatchResultControlProps) {
  if (match.result === 'BYE') {
    return <Chip label="不戦勝" size="small" variant="outlined" />;
  }
  if (!editable) {
    return <Typography variant="body2">{matchResultText(match)}</Typography>;
  }

  return (
    <TextField
      select
      size="small"
      value={match.result}
      onChange={(e) => onInput(e.target.value as MatchResult)}
      disabled={saving}
      // aria-label はルート要素ではなく combobox 役割を持つ表示要素に付ける
      slotProps={{
        select: { SelectDisplayProps: { 'aria-label': `卓${match.tableNumber}の結果` } },
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
  );
}
