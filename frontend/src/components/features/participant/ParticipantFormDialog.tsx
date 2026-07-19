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

import type { Rank } from '../../../types/enums';
import { RANKS_STRONGEST_FIRST } from '../../../types/enums';
import type { Group } from '../../../types/group';
import type { Participant } from '../../../types/participant';
import { rankLabel } from '../../../utils/labels';

/** rank は Select で扱うため未入力を空文字で表現する */
export interface ParticipantFormValues {
  name: string;
  organization: string;
  rank: Rank | '';
  groupId: string;
}

export interface ParticipantFormDialogProps {
  open: boolean;
  /** 編集時は対象参加者、追加時は undefined */
  participant?: Participant;
  /** グループ定義(1つだけの大会ではグループ選択を表示しない) */
  groups: Group[];
  loading: boolean;
  onSubmit: (values: ParticipantFormValues) => void;
  onClose: () => void;
}

export function ParticipantFormDialog({
  open,
  participant,
  groups,
  loading,
  onSubmit,
  onClose,
}: ParticipantFormDialogProps) {
  const { control, handleSubmit, reset } = useForm<ParticipantFormValues>({
    values: {
      name: participant?.name ?? '',
      organization: participant?.organization ?? '',
      rank: participant?.rank ?? '',
      // 追加時は先頭グループ(定義順)を初期選択する
      groupId: participant?.groupId ?? groups[0]?.id ?? '',
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
      <DialogTitle>{participant ? '参加者を編集' : '参加者を追加'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Controller
              name="name"
              control={control}
              rules={{
                required: '氏名は必須です',
                maxLength: { value: 50, message: '氏名は50文字以内で入力してください' },
              }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="氏名"
                  required
                  autoFocus
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                  fullWidth
                />
              )}
            />
            <Controller
              name="organization"
              control={control}
              rules={{ maxLength: { value: 100, message: '所属は100文字以内で入力してください' } }}
              render={({ field, fieldState }) => (
                <TextField
                  {...field}
                  label="所属"
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                  fullWidth
                />
              )}
            />
            <Controller
              name="rank"
              control={control}
              render={({ field }) => (
                <TextField {...field} label="棋力(段級位)" select fullWidth>
                  <MenuItem value="">未入力</MenuItem>
                  {RANKS_STRONGEST_FIRST.map((rank) => (
                    <MenuItem key={rank} value={rank}>
                      {rankLabel(rank)}
                    </MenuItem>
                  ))}
                </TextField>
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
            {participant ? '保存する' : '追加する'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
