import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import PersonOffIcon from '@mui/icons-material/PersonOff';
import {
  Chip,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
} from '@mui/material';

import type { Participant } from '../../../types/participant';
import { rankLabel } from '../../../utils/labels';

export interface ParticipantTableProps {
  participants: Participant[];
  /** 追加・編集・削除可(大会状態 PREPARING) */
  canEdit: boolean;
  /** 棄権処理のみ可(大会状態 IN_PROGRESS) */
  canWithdraw: boolean;
  onEdit: (participant: Participant) => void;
  onWithdraw: (participant: Participant) => void;
  onDelete: (participant: Participant) => void;
}

export function ParticipantTable({
  participants,
  canEdit,
  canWithdraw,
  onEdit,
  onWithdraw,
  onDelete,
}: ParticipantTableProps) {
  const showActions = canEdit || canWithdraw;
  return (
    <TableContainer sx={{ overflowX: 'auto' }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>No.</TableCell>
            <TableCell>氏名</TableCell>
            <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>所属</TableCell>
            <TableCell>棋力</TableCell>
            <TableCell>状態</TableCell>
            {showActions && <TableCell align="right">操作</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {participants.map((participant) => {
            const withdrawn = participant.status === 'WITHDRAWN';
            return (
              <TableRow key={participant.id} sx={withdrawn ? { opacity: 0.55 } : undefined}>
                <TableCell>{participant.seedOrder}</TableCell>
                <TableCell>{participant.name}</TableCell>
                <TableCell sx={{ display: { xs: 'none', sm: 'table-cell' } }}>
                  {participant.organization ?? ''}
                </TableCell>
                <TableCell>{rankLabel(participant.rank)}</TableCell>
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
