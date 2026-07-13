import {
  Box,
  Card,
  CardContent,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';

import type { MatchResult } from '../../../types/enums';
import type { Match } from '../../../types/round';
import type { ParticipantSummary } from '../../../types/participant';
import { MatchResultControl } from './MatchResultControl';
import { resultMark } from './matchDisplay';

export interface PairingTableProps {
  matches: Match[];
  editable: boolean;
  /** 結果送信中の対局ID(該当行のみ入力を無効化) */
  savingMatchId: string | null;
  onInputResult: (match: Match, result: MatchResult) => void;
}

function playerText(player: ParticipantSummary | null, mark: string | null): string {
  if (player === null) {
    return '(不戦勝)';
  }
  const name = player.organization ? `${player.name}(${player.organization})` : player.name;
  return mark ? `${mark} ${name}` : name;
}

/** 組み合わせ表(02_component_design.md §3)。PC=テーブル / スマホ=1対局1カード */
export function PairingTable({
  matches,
  editable,
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
                {match.tableNumber}
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
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>卓</TableCell>
            <TableCell>対局者1</TableCell>
            <TableCell>対局者2</TableCell>
            <TableCell>結果</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {matches.map((match) => (
            <TableRow key={match.id}>
              <TableCell>
                <Typography variant="h3" component="span">
                  {match.tableNumber}
                </Typography>
              </TableCell>
              <TableCell>{playerText(match.player1, resultMark(match, 'player1'))}</TableCell>
              <TableCell>{playerText(match.player2, resultMark(match, 'player2'))}</TableCell>
              <TableCell>
                <MatchResultControl
                  match={match}
                  editable={editable}
                  saving={savingMatchId === match.id}
                  onInput={(result) => onInputResult(match, result)}
                />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
