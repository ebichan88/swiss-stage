import {
  Box,
  Button,
  CircularProgress,
  Container,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../services/apiClient';
import { useCreateTournament } from '../hooks/useTournaments';
import { useSnackbar } from '../hooks/useSnackbar';
import { paths } from '../routes';
import { GameType } from '../types/enums';
import { gameTypeLabels } from '../utils/labels';

interface TournamentFormValues {
  name: string;
  gameType: GameType;
  totalRounds: string;
}

/** S04 大会作成 */
export function TournamentCreatePage() {
  const createMutation = useCreateTournament();
  const navigate = useNavigate();
  const { showSuccess, showError } = useSnackbar();
  const { control, handleSubmit } = useForm<TournamentFormValues>({
    defaultValues: { name: '', gameType: GameType.GO, totalRounds: '5' },
  });

  const onSubmit = (values: TournamentFormValues) => {
    createMutation.mutate(
      {
        name: values.name.trim(),
        gameType: values.gameType,
        competitionType: 'INDIVIDUAL',
        teamSize: null,
        totalRounds: Number(values.totalRounds),
      },
      {
        onSuccess: (tournament) => {
          showSuccess('大会を作成しました');
          navigate(paths.tournament(tournament.id));
        },
        onError: (error) => {
          showError(error instanceof ApiError ? error.message : '大会の作成に失敗しました');
        },
      },
    );
  };

  return (
    <Container maxWidth="sm" sx={{ py: 3 }}>
      <Typography variant="h2" component="h1" gutterBottom>
        大会を作成
      </Typography>
      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Stack spacing={2} sx={{ mt: 2 }}>
          <Controller
            name="name"
            control={control}
            rules={{
              required: '大会名は必須です',
              maxLength: { value: 100, message: '大会名は100文字以内で入力してください' },
            }}
            render={({ field, fieldState }) => (
              <TextField
                {...field}
                label="大会名"
                required
                error={!!fieldState.error}
                helperText={fieldState.error?.message}
                fullWidth
              />
            )}
          />
          <Controller
            name="gameType"
            control={control}
            render={({ field }) => (
              <TextField {...field} label="競技" select required fullWidth>
                {Object.values(GameType).map((type) => (
                  <MenuItem key={type} value={type}>
                    {gameTypeLabels[type]}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <Controller
            name="totalRounds"
            control={control}
            rules={{
              required: 'ラウンド数は必須です',
              validate: (value) => {
                const n = Number(value);
                if (!Number.isInteger(n) || n < 1 || n > 8) {
                  return 'ラウンド数は1〜8の整数で入力してください';
                }
                return true;
              },
            }}
            render={({ field, fieldState }) => (
              <TextField
                {...field}
                label="ラウンド数"
                required
                type="number"
                slotProps={{ htmlInput: { min: 1, max: 8 } }}
                error={!!fieldState.error}
                helperText={fieldState.error?.message ?? '大会全体の対局回数(後から変更できません)'}
                fullWidth
              />
            )}
          />
          <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
            <Button variant="outlined" component={Link} to={paths.tournaments}>
              キャンセル
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={createMutation.isPending}
              startIcon={
                createMutation.isPending ? (
                  <CircularProgress size={16} color="inherit" />
                ) : undefined
              }
            >
              作成する
            </Button>
          </Box>
        </Stack>
      </Box>
    </Container>
  );
}
