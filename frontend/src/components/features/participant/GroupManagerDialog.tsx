import AddIcon from '@mui/icons-material/Add';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  List,
  ListItem,
  ListItemText,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { useState } from 'react';

import {
  useAutoAssignGroups,
  useCreateGroup,
  useDeleteGroup,
  useGroups,
  useRenameGroup,
} from '../../../hooks/useGroups';
import { useSnackbar } from '../../../hooks/useSnackbar';
import { ApiError } from '../../../services/apiClient';
import type { Group } from '../../../types/group';
import { ConfirmDialog } from '../../ui/ConfirmDialog';

export interface GroupManagerDialogProps {
  open: boolean;
  tournamentId: string;
  onClose: () => void;
  /** 段級位による自動振り分け機能を表示するか(団体戦では棋力情報を持たないため非表示にする)。省略時はtrue */
  showAutoAssign?: boolean;
}

/**
 * グループ管理(PREPARING中のみ開ける)。
 * 棋力帯グループの定義(追加・改名・削除)と「段級位で自動振り分け」を行う。
 * 大会作成時にデフォルトグループ「A」が作成済み。グループは常に1つ以上(最後の1つは削除不可)。
 * グループは強い帯から順に定義する(自動振り分けは定義順に強い側から割り当てる)
 */
export function GroupManagerDialog({
  open,
  tournamentId,
  onClose,
  showAutoAssign = true,
}: GroupManagerDialogProps) {
  const { data: groups } = useGroups(tournamentId);
  const createMutation = useCreateGroup(tournamentId);
  const renameMutation = useRenameGroup(tournamentId);
  const deleteMutation = useDeleteGroup(tournamentId);
  const autoAssignMutation = useAutoAssignGroups(tournamentId);
  const { showSuccess, showError } = useSnackbar();

  const [newName, setNewName] = useState('');
  const [editing, setEditing] = useState<{ id: string; name: string } | null>(null);
  const [confirming, setConfirming] = useState<'auto-assign' | { delete: Group } | null>(null);

  const errorMessage = (error: unknown, fallback: string) =>
    error instanceof ApiError ? error.message : fallback;

  const handleCreate = () => {
    const name = newName.trim();
    if (name === '') return;
    createMutation.mutate(
      { name },
      {
        onSuccess: () => setNewName(''),
        onError: (error) => showError(errorMessage(error, 'グループの作成に失敗しました')),
      },
    );
  };

  const handleRename = () => {
    if (editing === null) return;
    const name = editing.name.trim();
    if (name === '') return;
    renameMutation.mutate(
      { groupId: editing.id, input: { name } },
      {
        onSuccess: () => setEditing(null),
        onError: (error) => showError(errorMessage(error, 'グループの改名に失敗しました')),
      },
    );
  };

  const handleDelete = () => {
    if (confirming === null || confirming === 'auto-assign') return;
    deleteMutation.mutate(confirming.delete.id, {
      onSuccess: () => {
        setConfirming(null);
        showSuccess(`グループ「${confirming.delete.name}」を削除しました`);
      },
      onError: (error) => {
        setConfirming(null);
        showError(errorMessage(error, 'グループの削除に失敗しました'));
      },
    });
  };

  const handleAutoAssign = () => {
    autoAssignMutation.mutate(undefined, {
      onSuccess: () => {
        setConfirming(null);
        showSuccess('段級位でグループを振り分けました。参加者一覧で個別調整できます');
      },
      onError: (error) => {
        setConfirming(null);
        showError(errorMessage(error, '自動振り分けに失敗しました'));
      },
    });
  };

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
        <DialogTitle>グループ管理</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            棋力帯ごとにグループ分けする場合は「A」に続けて、強い帯から順に(B→C→…)作成してください。
            グループ分けしない場合はそのままで構いません。
          </Typography>
          <List dense disablePadding>
            {(groups ?? []).map((group) =>
              editing?.id === group.id ? (
                <ListItem key={group.id} disableGutters>
                  <TextField
                    value={editing.name}
                    onChange={(e) => setEditing({ id: group.id, name: e.target.value })}
                    size="small"
                    fullWidth
                    autoFocus
                    slotProps={{ htmlInput: { 'aria-label': 'グループ名を編集', maxLength: 50 } }}
                  />
                  <Tooltip title="保存">
                    <IconButton
                      size="small"
                      aria-label="グループ名を保存"
                      onClick={handleRename}
                      disabled={renameMutation.isPending}
                    >
                      <CheckIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="キャンセル">
                    <IconButton
                      size="small"
                      aria-label="編集をキャンセル"
                      onClick={() => setEditing(null)}
                    >
                      <CloseIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </ListItem>
              ) : (
                <ListItem
                  key={group.id}
                  disableGutters
                  secondaryAction={
                    <>
                      <Tooltip title="改名">
                        <IconButton
                          size="small"
                          aria-label={`${group.name}を改名`}
                          onClick={() => setEditing({ id: group.id, name: group.name })}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip
                        title={
                          (groups ?? []).length <= 1 ? '最後のグループは削除できません' : '削除'
                        }
                      >
                        <span>
                          <IconButton
                            size="small"
                            aria-label={`${group.name}を削除`}
                            onClick={() => setConfirming({ delete: group })}
                            disabled={(groups ?? []).length <= 1}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </>
                  }
                >
                  <ListItemText primary={group.name} />
                </ListItem>
              ),
            )}
          </List>
          <Box component="form" sx={{ display: 'flex', gap: 1, mt: 1 }}>
            <TextField
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              size="small"
              fullWidth
              placeholder="グループ名(例: B)"
              slotProps={{ htmlInput: { 'aria-label': '新しいグループ名', maxLength: 50 } }}
            />
            <Button
              type="submit"
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={(e) => {
                e.preventDefault();
                handleCreate();
              }}
              disabled={newName.trim() === '' || createMutation.isPending}
              sx={{ flexShrink: 0 }}
            >
              追加
            </Button>
          </Box>
        </DialogContent>
        <DialogActions>
          {showAutoAssign && (
            <Button
              variant="outlined"
              startIcon={
                autoAssignMutation.isPending ? (
                  <CircularProgress size={16} color="inherit" />
                ) : (
                  <AutoFixHighIcon />
                )
              }
              onClick={() => setConfirming('auto-assign')}
              disabled={(groups ?? []).length === 0 || autoAssignMutation.isPending}
              sx={{ mr: 'auto' }}
            >
              段級位で自動振り分け
            </Button>
          )}
          <Button variant="contained" onClick={onClose}>
            閉じる
          </Button>
        </DialogActions>
      </Dialog>

      {showAutoAssign && (
        <ConfirmDialog
          open={confirming === 'auto-assign'}
          title="段級位で自動振り分けしますか?"
          message="棋力の強い順に、グループの定義順(A→B→…)へできるだけ均等に振り分けます。現在の割当は上書きされます。適用後に参加者一覧で個別調整できます。"
          confirmLabel="振り分ける"
          confirmColor="primary"
          loading={autoAssignMutation.isPending}
          onConfirm={handleAutoAssign}
          onCancel={() => setConfirming(null)}
        />
      )}
      <ConfirmDialog
        open={confirming !== null && confirming !== 'auto-assign'}
        title="グループを削除しますか?"
        message={
          confirming !== null && confirming !== 'auto-assign'
            ? `グループ「${confirming.delete.name}」を削除します。割当済みの参加者は隣のグループへ移動します。`
            : ''
        }
        confirmLabel="削除する"
        loading={deleteMutation.isPending}
        onConfirm={handleDelete}
        onCancel={() => setConfirming(null)}
      />
    </>
  );
}
