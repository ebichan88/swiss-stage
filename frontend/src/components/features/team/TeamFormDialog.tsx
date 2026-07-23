import {
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';

import type { Group } from '../../../types/group';
import type { Team } from '../../../types/team';

export interface TeamFormValues {
  name: string;
  groupId: string;
}

export interface TeamFormDialogProps {
  open: boolean;
  /** 編集時は対象チーム、追加時は undefined */
  team?: Team;
  /** グループ定義(1つだけの大会ではグループ選択を表示しない) */
  groups: Group[];
  loading: boolean;
  onSubmit: (values: TeamFormValues) => void;
  onClose: () => void;
}

export function TeamFormDialog({
  open,
  team,
  groups,
  loading,
  onSubmit,
  onClose,
}: TeamFormDialogProps) {
  const { control, handleSubmit, reset } = useForm<TeamFormValues>({
    values: {
      name: team?.name ?? '',
      // 追加時は先頭グループ(定義順)を初期選択する
      groupId: team?.groupId ?? groups[0]?.id ?? '',
    },
  });

  return (
    <Dialog
      open={open}
      onClose={loading ? undefined : onClose}
      maxWidth="xs"
      fullWidth
      slotProps={{ transition: { onExited: () => reset() } }}
    >
      <DialogTitle>{team ? 'チームを編集' : 'チームを追加'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Controller
              name="name"
              control={control}
              rules={{
                required: 'チーム名は必須です',
                maxLength: { value: 50, message: 'チーム名は50文字以内で入力してください' },
              }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="チーム名"
                  required
                  autoFocus
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                  fullWidth
                />
              )}
            />
            {groups.length > 1 && (
              <Controller
                name="groupId"
                control={control}
                render={({ field }) => (
                  <TextField {...field} label="グループ" select fullWidth>
                    {groups.map((group) => (
                      <MenuItem key={group.id} value={group.id}>
                        {group.name}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" onClick={onClose} disabled={loading}>
            キャンセル
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={loading}
            startIcon={loading ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            {team ? '保存する' : '追加する'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
