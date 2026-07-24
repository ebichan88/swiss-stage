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
import type { TeamMatch, TeamSummary } from '../../../types/team';
import { TeamMatchResultControl } from './TeamMatchResultControl';
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

function teamText(team: TeamSummary | null, mark: string | null): string {
  if (team === null) {
    return '(不戦勝)';
  }
  return mark ? `${mark} ${team.name}` : team.name;
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
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead
          sx={{
            bgcolor: 'primary.main',
            '& .MuiTableCell-root': { color: 'primary.contrastText', fontWeight: 600 },
          }}
        >
          <TableRow>
            <TableCell>卓</TableCell>
            <TableCell>チーム1</TableCell>
            <TableCell>チーム2</TableCell>
            <TableCell>結果(主将〜)</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {matches.map((match, index) => (
            <TableRow
              key={match.id}
              sx={{ bgcolor: index % 2 === 0 ? 'background.paper' : 'background.default' }}
            >
              <TableCell>{teamTableLabel(match, multiGroup)}</TableCell>
              <TableCell>{teamText(match.team1, teamResultMark(match, 'team1'))}</TableCell>
              <TableCell>{teamText(match.team2, teamResultMark(match, 'team2'))}</TableCell>
              <TableCell>
                <TeamMatchResultControl
                  match={match}
                  editable={editable}
                  multiGroup={multiGroup}
                  saving={savingMatchId === match.id}
                  onInput={(boardResults) => onInputResult(match, boardResults)}
                />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
