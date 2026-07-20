import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
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
import type { Participant } from '../../../types/participant';
import { rankLabel } from '../../../utils/labels';

export interface ParticipantTableProps {
  participants: Participant[];
  /** グループ定義(1つだけの大会ではグループ列を表示しない) */
  groups: Group[];
  /** 追加・編集・削除可(大会状態 PREPARING) */
  canEdit: boolean;
  /** 棄権処理のみ可(大会状態 IN_PROGRESS) */
  canWithdraw: boolean;
  onEdit: (participant: Participant) => void;
  onWithdraw: (participant: Participant) => void;
  onDelete: (participant: Participant) => void;
  /** グループ割当の変更。PREPARING のみ呼ばれる */
  onChangeGroup: (participant: Participant, groupId: string) => void;
}

export function ParticipantTable({
  participants,
  groups,
  canEdit,
  canWithdraw,
  onEdit,
  onWithdraw,
  onDelete,
  onChangeGroup,
}: ParticipantTableProps) {
  const showActions = canEdit || canWithdraw;
  const showGroup = groups.length > 1;
  const groupName = (groupId: string) => groups.find((g) => g.id === groupId)?.name ?? '';
  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>No.</TableCell>
            <TableCell>氏名</TableCell>
            <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>所属</TableCell>
            <TableCell>棋力</TableCell>
            {showGroup && <TableCell>グループ</TableCell>}
            <TableCell>状態</TableCell>
            {showActions && <TableCell align="right">操作</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {participants.map((participant) => {
            const withdrawn = participant.status === 'WITHDRAWN';
            return (
              <TableRow key={participant.id} sx={withdrawn ? { opacity: 0.55 } : undefined}>
                <TableCell>{participant.entryOrder}</TableCell>
                <TableCell>{participant.name}</TableCell>
                <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>
                  {participant.organization ?? ''}
                </TableCell>
                <TableCell>{rankLabel(participant.rank)}</TableCell>
                {showGroup && (
                  <TableCell>
                    {canEdit ? (
                      <TextField
                        select
                        size="small"
                        value={participant.groupId}
                        onChange={(e) => onChangeGroup(participant, e.target.value)}
                        slotProps={{
                          select: {
                            SelectDisplayProps: {
                              'aria-label': `${participant.name}のグループ`,
                            },
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
                      groupName(participant.groupId)
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
                    {canEdit && (
                      <>
                        <Tooltip title="編集">
                          <IconButton
                            size="small"
                            aria-label={`${participant.name}を編集`}
                            onClick={() => onEdit(participant)}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="削除">
                          <IconButton
                            size="small"
                            aria-label={`${participant.name}を削除`}
                            onClick={() => onDelete(participant)}
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
                          aria-label={`${participant.name}を棄権にする`}
                          onClick={() => onWithdraw(participant)}
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
