import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';

import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { useSnackbar } from '../hooks/useSnackbar';
import { useDeleteTournament, useUpdateTournament } from '../hooks/useTournaments';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import type { Visibility } from '../types/enums';
import { Visibility as VisibilityValues } from '../types/enums';
import { visibilityLabels } from '../utils/labels';

interface SettingsFormValues {
  name: string;
  visibility: Visibility;
}

/** S09 大会設定(名前・公開範囲・共有URL・削除)。共有トークンの再発行はPhase 5 */
export function SettingsPage() {
  const tournament = useTournamentContext();
  const updateMutation = useUpdateTournament(tournament.id);
  const deleteMutation = useDeleteTournament(tournament.id);
  const navigate = useNavigate();
  const { showSuccess, showError } = useSnackbar();
  const [deleting, setDeleting] = useState(false);

  const { control, handleSubmit } = useForm<SettingsFormValues>({
    // 保存成功時は invalidate で tournament が更新され、values 経由でフォームにも反映される
    values: { name: tournament.name, visibility: tournament.visibility },
  });

  const onSubmit = (formValues: SettingsFormValues) => {
    updateMutation.mutate(
      {
        name: formValues.name.trim(),
        visibility: formValues.visibility,
        version: tournament.version,
      },
      {
        onSuccess: () => showSuccess('設定を保存しました'),
        onError: (error) =>
          showError(error instanceof ApiError ? error.message : '設定の保存に失敗しました'),
      },
    );
  };

  const handleDelete = () => {
    deleteMutation.mutate(undefined, {
      onSuccess: () => {
        showSuccess('大会を削除しました');
        navigate(paths.tournaments);
      },
      onError: (error) => {
        setDeleting(false);
        showError(error instanceof ApiError ? error.message : '大会の削除に失敗しました');
      },
    });
  };

  const shareUrl = tournament.shareToken
    ? `${window.location.origin}${paths.shared(tournament.shareToken)}`
    : null;

  const handleCopyShareUrl = () => {
    if (!shareUrl) return;
    navigator.clipboard.writeText(shareUrl).then(
      () => showSuccess('共有URLをコピーしました'),
      () => showError('コピーに失敗しました'),
    );
  };

  return (
    <Stack spacing={4} sx={{ maxWidth: 600 }}>
      <Card variant="outlined">
        <CardContent>
          <Typography variant="h3" component="h2" gutterBottom>
            基本設定
          </Typography>
          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <Stack spacing={2} sx={{ mt: 1 }}>
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
                name="visibility"
                control={control}
                render={({ field }) => (
                  <TextField {...field} label="公開範囲" select fullWidth>
                    {Object.values(VisibilityValues).map((visibility) => (
                      <MenuItem key={visibility} value={visibility}>
                        {visibilityLabels[visibility]}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
              <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  type="submit"
                  variant="contained"
                  disabled={updateMutation.isPending}
                  startIcon={
                    updateMutation.isPending ? (
                      <CircularProgress size={16} color="inherit" />
                    ) : undefined
                  }
                >
                  保存する
                </Button>
              </Box>
            </Stack>
          </Box>
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="h3" component="h2" gutterBottom>
            共有URL
          </Typography>
          {shareUrl ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <TextField
                value={shareUrl}
                fullWidth
                size="small"
                slotProps={{ htmlInput: { readOnly: true } }}
              />
              <Tooltip title="コピー">
                <IconButton aria-label="共有URLをコピー" onClick={handleCopyShareUrl}>
                  <ContentCopyIcon />
                </IconButton>
              </Tooltip>
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary">
              共有URLの発行・再発行は今後追加予定です。
            </Typography>
          )}
        </CardContent>
      </Card>

      <Card variant="outlined" sx={{ borderColor: 'error.main' }}>
        <CardContent>
          <Typography variant="h3" component="h2" gutterBottom color="error">
            危険な操作
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            大会を削除すると参加者・対局結果もすべて削除され、元に戻せません。
          </Typography>
          <Button
            variant="contained"
            color="error"
            onClick={() => setDeleting(true)}
            sx={{ mt: 1 }}
          >
            大会を削除する
          </Button>
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleting}
        title="大会を削除しますか?"
        message="この操作は元に戻せません。参加者・組み合わせ・結果もすべて削除されます。"
        confirmLabel="削除する"
        requiredText={tournament.name}
        loading={deleteMutation.isPending}
        onConfirm={handleDelete}
        onCancel={() => setDeleting(false)}
      />
    </Stack>
  );
}
