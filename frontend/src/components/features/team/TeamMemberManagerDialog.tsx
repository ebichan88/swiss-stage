import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Tooltip,
  Typography,
} from '@mui/material';
import { useState } from 'react';

import {
  useAddTeamMember,
  useDeleteTeamMember,
  useUpdateTeamMember,
} from '../../../hooks/useTeams';
import { useSnackbar } from '../../../hooks/useSnackbar';
import { ApiError } from '../../../services/apiClient';
import type { Team, TeamMember } from '../../../types/team';
import { boardPositionLabel, rankLabel } from '../../../utils/labels';
import { ConfirmDialog } from '../../ui/ConfirmDialog';
import type { TeamMemberFormValues } from './TeamMemberFormDialog';
import { TeamMemberFormDialog } from './TeamMemberFormDialog';

export interface TeamMemberManagerDialogProps {
  open: boolean;
  tournamentId: string;
  /** 3または5(チーム制) */
  teamSize: number;
  /** 対象チーム(nullの間はダイアログを開かない) */
  team: Team | null;
  /** 大会開始前のみ編集可 */
  canEdit: boolean;
  onClose: () => void;
}

type DialogState =
  | { kind: 'add' }
  | { kind: 'edit'; member: TeamMember }
  | { kind: 'delete'; member: TeamMember }
  | null;

/** チームのメンバー(主将〜・補欠)管理。PREPARING中のみ追加・編集・削除可 */
export function TeamMemberManagerDialog({
  open,
  tournamentId,
  teamSize,
  team,
  canEdit,
  onClose,
}: TeamMemberManagerDialogProps) {
  const addMutation = useAddTeamMember(tournamentId);
  const updateMutation = useUpdateTeamMember(tournamentId);
  const deleteMutation = useDeleteTeamMember(tournamentId);
  const { showSuccess, showError } = useSnackbar();
  const [dialog, setDialog] = useState<DialogState>(null);

  const errorMessage = (error: unknown, fallback: string) =>
    error instanceof ApiError ? error.message : fallback;

  const members = [...(team?.members ?? [])].sort((a, b) => {
    if (a.boardPosition !== null && b.boardPosition !== null) {
      return a.boardPosition - b.boardPosition;
    }
    if (a.boardPosition !== null) return -1;
    if (b.boardPosition !== null) return 1;
    return 0;
  });

  const handleSubmit = (values: TeamMemberFormValues) => {
    if (!team) return;
    const boardPosition = values.boardPosition === '' ? null : Number(values.boardPosition);
    if (dialog?.kind === 'add') {
      addMutation.mutate(
        {
          teamId: team.id,
          input: { name: values.name.trim(), rank: values.rank || null, boardPosition },
        },
        {
          onSuccess: () => {
            setDialog(null);
            showSuccess('メンバーを追加しました');
          },
          onError: (error) => showError(errorMessage(error, 'メンバーの追加に失敗しました')),
        },
      );
    } else if (dialog?.kind === 'edit') {
      updateMutation.mutate(
        {
          teamId: team.id,
          memberId: dialog.member.id,
          input: {
            name: values.name.trim(),
            ...(values.rank === '' ? { clearRank: true } : { rank: values.rank }),
            ...(values.boardPosition === ''
              ? { clearBoardPosition: true }
              : { boardPosition: Number(values.boardPosition) }),
          },
        },
        {
          onSuccess: () => {
            setDialog(null);
            showSuccess('メンバーを更新しました');
          },
          onError: (error) => showError(errorMessage(error, 'メンバーの更新に失敗しました')),
        },
      );
    }
  };

  const handleDelete = () => {
    if (dialog?.kind !== 'delete' || !team) return;
    deleteMutation.mutate(
      { teamId: team.id, memberId: dialog.member.id },
      {
        onSuccess: () => {
          setDialog(null);
          showSuccess('メンバーを削除しました');
        },
        onError: (error) => showError(errorMessage(error, 'メンバーの削除に失敗しました')),
      },
    );
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
        <DialogTitle>{team ? `${team.name}のメンバー` : 'メンバー'}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            主将〜のポジションは全員分の登録が必須です(大会開始前まで)。個人名は対局結果には表示されません。
          </Typography>
          {members.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              メンバーがまだいません
            </Typography>
          ) : (
            <List dense disablePadding>
              {members.map((member) => (
                <ListItem
                  key={member.id}
                  disableGutters
                  secondaryAction={
                    canEdit && (
                      <>
                        <Tooltip title="編集">
                          <IconButton
                            size="small"
                            aria-label={`${member.name}を編集`}
                            onClick={() => setDialog({ kind: 'edit', member })}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="削除">
                          <IconButton
                            size="small"
                            aria-label={`${member.name}を削除`}
                            onClick={() => setDialog({ kind: 'delete', member })}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </>
                    )
                  }
                >
                  <Chip
                    label={boardPositionLabel(member.boardPosition)}
                    size="small"
                    color={member.boardPosition === null ? 'default' : 'primary'}
                    variant="outlined"
                    sx={{ mr: 1.5, minWidth: 56 }}
                  />
                  <ListItemText primary={member.name} secondary={rankLabel(member.rank)} />
                </ListItem>
              ))}
            </List>
          )}
          {canEdit && (
            <Box sx={{ mt: 2 }}>
              <Button
                variant="outlined"
                startIcon={<PersonAddIcon />}
                onClick={() => setDialog({ kind: 'add' })}
              >
                メンバーを追加
              </Button>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button variant="contained" onClick={onClose}>
            閉じる
          </Button>
        </DialogActions>
      </Dialog>

      <TeamMemberFormDialog
        open={dialog?.kind === 'add' || dialog?.kind === 'edit'}
        member={dialog?.kind === 'edit' ? dialog.member : undefined}
        teamSize={teamSize}
        loading={addMutation.isPending || updateMutation.isPending}
        onSubmit={handleSubmit}
        onClose={() => setDialog(null)}
      />
      <ConfirmDialog
        open={dialog?.kind === 'delete'}
        title="メンバーを削除しますか?"
        message={dialog?.kind === 'delete' ? `${dialog.member.name}さんを削除します。` : ''}
        confirmLabel="削除する"
        loading={deleteMutation.isPending}
        onConfirm={handleDelete}
        onCancel={() => setDialog(null)}
      />
    </>
  );
}
