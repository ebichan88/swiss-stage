import { Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';

import { buildTeamRosterRows } from './printTeamRosterData';
import type { Group } from '../../../types/group';
import type { Team } from '../../../types/team';

export interface PrintTeamRosterProps {
  teams: Team[];
  groups: Group[];
}

/**
 * 団体戦の参加者名簿(印刷・運営専用)。チーム名・メンバー氏名・段級位・役割を1メンバー1行で出力する。
 * 対局結果を見せる画面とは異なり運営専用の帳票なので個人名を出してよい(04_screen_transition_design.md §5)
 */
export function PrintTeamRoster({ teams, groups }: PrintTeamRosterProps) {
  const rows = buildTeamRosterRows(teams, groups);
  const showGroupColumn = groups.length > 1;

  return (
    <>
      <Typography variant="body2" color="error" sx={{ mb: 1, fontWeight: 600 }}>
        運営用(掲示・配布しないでください)
      </Typography>
      <Table size="small" sx={(theme) => ({ fontSize: theme.print.tableFontSize })}>
        <TableHead sx={(theme) => ({ bgcolor: theme.print.headerBg })}>
          <TableRow>
            <TableCell>チーム名</TableCell>
            <TableCell>氏名</TableCell>
            <TableCell>段級位</TableCell>
            <TableCell>役割</TableCell>
            {showGroupColumn && <TableCell>グループ</TableCell>}
            <TableCell>状態</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row, index) => (
            // 同名チーム・同名メンバーがあり得るため一意な複合キーが作れず、indexをkeyにする
            <TableRow key={index}>
              <TableCell>{row.teamName}</TableCell>
              <TableCell>{row.memberName}</TableCell>
              <TableCell>{row.rankText}</TableCell>
              <TableCell>{row.positionLabel}</TableCell>
              {showGroupColumn && <TableCell>{row.groupName}</TableCell>}
              <TableCell>{row.withdrawn ? '棄権' : ''}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </>
  );
}
