import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import GroupsIcon from '@mui/icons-material/Groups';
import PersonOffIcon from '@mui/icons-material/PersonOff';
import {
  Chip,
  IconButton,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
} from '@mui/material';

import type { Group } from '../../../types/group';
import type { Team } from '../../../types/team';

export interface TeamTableProps {
  teams: Team[];
  /** 1チームあたりの必須人数(3または5) */
  teamSize: number;
  /** グループ定義(1つだけの大会ではグループ列を表示しない) */
  groups: Group[];
  /** 追加・編集・削除可(大会状態 PREPARING) */
  canEdit: boolean;
  /** 棄権処理のみ可(大会状態 IN_PROGRESS) */
  canWithdraw: boolean;
  onEdit: (team: Team) => void;
  onManageMembers: (team: Team) => void;
  onWithdraw: (team: Team) => void;
  onDelete: (team: Team) => void;
  /** グループ割当の変更。PREPARING のみ呼ばれる */
  onChangeGroup: (team: Team, groupId: string) => void;
}

export function TeamTable({
  teams,
  teamSize,
  groups,
  canEdit,
  canWithdraw,
  onEdit,
  onManageMembers,
  onWithdraw,
  onDelete,
  onChangeGroup,
}: TeamTableProps) {
  const showActions = canEdit || canWithdraw;
  const showGroup = groups.length > 1;
  const groupName = (groupId: string) => groups.find((g) => g.id === groupId)?.name ?? '';
  const requiredCount = (team: Team) => team.members.filter((m) => m.boardPosition !== null).length;
  const reserveCount = (team: Team) => team.members.filter((m) => m.boardPosition === null).length;

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
            <TableCell>No.</TableCell>
            <TableCell>チーム名</TableCell>
            <TableCell>メンバー</TableCell>
            {showGroup && <TableCell>グループ</TableCell>}
            <TableCell>状態</TableCell>
            {showActions && <TableCell align="right">操作</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {teams.map((team, index) => {
            const withdrawn = team.status === 'WITHDRAWN';
            const required = requiredCount(team);
            const reserves = reserveCount(team);
            return (
              <TableRow
                key={team.id}
                sx={{
                  bgcolor: index % 2 === 0 ? 'background.paper' : 'background.default',
                  ...(withdrawn ? { opacity: 0.55 } : {}),
                }}
              >
                <TableCell>{team.entryOrder}</TableCell>
                <TableCell>{team.name}</TableCell>
                <TableCell>
                  <Chip
                    label={`${required}/${teamSize}人${reserves > 0 ? ` + 補欠${reserves}` : ''}`}
                    size="small"
                    color={required === teamSize ? 'success' : 'warning'}
                    variant="outlined"
                  />
                </TableCell>
                {showGroup && (
                  <TableCell>
                    {canEdit ? (
                      <TextField
                        select
                        size="small"
                        value={team.groupId}
                        onChange={(e) => onChangeGroup(team, e.target.value)}
                        slotProps={{
                          select: {
                            SelectDisplayProps: { 'aria-label': `${team.name}のグループ` },
                          },
                        }}
                        sx={{ minWidth: 100 }}
                      >
                        {groups.map((group) => (
                          <MenuItem key={group.id} value={group.id}>
                            {group.name}
                          </MenuItem>
                        ))}
                      </TextField>
                    ) : (
                      groupName(team.groupId)
                    )}
                  </TableCell>
                )}
                <TableCell>
                  {withdrawn ? (
                    <Chip label="棄権" size="small" variant="outlined" />
                  ) : (
                    <Chip label="参加中" size="small" color="success" variant="outlined" />
                  )}
                </TableCell>
                {showActions && (
                  <TableCell align="right">
                    <Tooltip title="メンバー管理">
                      <IconButton
                        size="small"
                        aria-label={`${team.name}のメンバーを管理`}
                        onClick={() => onManageMembers(team)}
                      >
                        <GroupsIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    {canEdit && (
                      <>
                        <Tooltip title="編集">
                          <IconButton
                            size="small"
                            aria-label={`${team.name}を編集`}
                            onClick={() => onEdit(team)}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="削除">
                          <IconButton
                            size="small"
                            aria-label={`${team.name}を削除`}
                            onClick={() => onDelete(team)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </>
                    )}
                    {canWithdraw && !withdrawn && (
                      <Tooltip title="棄権にする">
                        <IconButton
                          size="small"
                          aria-label={`${team.name}を棄権にする`}
                          onClick={() => onWithdraw(team)}
                        >
                          <PersonOffIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                )}
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
