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
import type { TeamMember } from '../../../types/team';
import { boardPositionLabel, rankLabel } from '../../../utils/labels';

/** rank/boardPosition は Select で扱うため未入力を空文字で表現する(空文字 = 補欠) */
export interface TeamMemberFormValues {
  name: string;
  rank: Rank | '';
  boardPosition: '' | `${number}`;
}

export interface TeamMemberFormDialogProps {
  open: boolean;
  /** 編集時は対象メンバー、追加時は undefined */
  member?: TeamMember;
  /** 3または5(チーム制) */
  teamSize: number;
  loading: boolean;
  onSubmit: (values: TeamMemberFormValues) => void;
  onClose: () => void;
}

export function TeamMemberFormDialog({
  open,
  member,
  teamSize,
  loading,
  onSubmit,
  onClose,
}: TeamMemberFormDialogProps) {
  const { control, handleSubmit, reset } = useForm<TeamMemberFormValues>({
    values: {
      name: member?.name ?? '',
      rank: member?.rank ?? '',
      boardPosition:
        member?.boardPosition != null ? (`${member.boardPosition}` as `${number}`) : '',
    },
  });
  const positions = Array.from({ length: teamSize }, (_, i) => i + 1);

  return (
    <Dialog
      open={open}
      onClose={loading ? undefined : onClose}
      maxWidth="xs"
      fullWidth
      slotProps={{ transition: { onExited: () => reset() } }}
    >
      <DialogTitle>{member ? 'メンバーを編集' : 'メンバーを追加'}</DialogTitle>
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
            <Controller
              name="boardPosition"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="役割"
                  select
                  fullWidth
                  helperText="主将〜は必須ポジション(重複不可)。補欠は人数に上限があります"
                >
                  {positions.map((position) => (
                    <MenuItem key={position} value={`${position}`}>
                      {boardPositionLabel(position)}
                    </MenuItem>
                  ))}
                  <MenuItem value="">{boardPositionLabel(null)}</MenuItem>
                </TextField>
              )}
            />
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
            {member ? '保存する' : '追加する'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
