import AutorenewIcon from '@mui/icons-material/Autorenew';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import LinkIcon from '@mui/icons-material/Link';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  FormControlLabel,
  IconButton,
  MenuItem,
  Stack,
  Switch,
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
import {
  useDeleteTournament,
  useRegenerateShareToken,
  useUpdateTournament,
} from '../hooks/useTournaments';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import type { Visibility } from '../types/enums';
import { Visibility as VisibilityValues } from '../types/enums';
import { visibilityLabels } from '../utils/labels';

interface SettingsFormValues {
  name: string;
  visibility: Visibility;
  resultInputEnabled: boolean;
}

/** S09 大会設定(名前・公開範囲・結果入力許可・共有URL・削除) */
export function SettingsPage() {
  const tournament = useTournamentContext();
  const updateMutation = useUpdateTournament(tournament.id);
  const deleteMutation = useDeleteTournament(tournament.id);
  const regenerateMutation = useRegenerateShareToken(tournament.id);
  const navigate = useNavigate();
  const { showSuccess, showError } = useSnackbar();
  const [deleting, setDeleting] = useState(false);
  const [regenerating, setRegenerating] = useState(false);

  const { control, handleSubmit } = useForm<SettingsFormValues>({
    // 保存成功時は invalidate で tournament が更新され、values 経由でフォームにも反映される
    values: {
      name: tournament.name,
      visibility: tournament.visibility,
      resultInputEnabled: tournament.resultInputEnabled,
    },
  });

  const onSubmit = (formValues: SettingsFormValues) => {
    updateMutation.mutate(
      {
        name: formValues.name.trim(),
        visibility: formValues.visibility,
        resultInputEnabled: formValues.resultInputEnabled,
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

  const handleRegenerate = () => {
    regenerateMutation.mutate(undefined, {
      onSuccess: () => {
        setRegenerating(false);
        showSuccess(shareUrl ? '共有URLを再発行しました' : '共有URLを発行しました');
      },
      onError: (error) => {
        setRegenerating(false);
        showError(error instanceof ApiError ? error.message : '共有URLの発行に失敗しました');
      },
    });
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
              <Controller
                name="resultInputEnabled"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    control={
                      <Switch
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                    }
                    label="参加者による結果入力を許可(共有URL経由)"
                  />
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
          <Typography variant="body2" color="text.secondary" gutterBottom>
            参加者はこのURLから組み合わせ・順位を閲覧できます(公開範囲が「非公開」の間は無効)。
          </Typography>
          {shareUrl ? (
            <Stack spacing={2} sx={{ mt: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <TextField
                  value={shareUrl}
                  fullWidth
                  size="small"
                  slotProps={{ htmlInput: { readOnly: true, 'aria-label': '共有URL' } }}
                />
                <Tooltip title="コピー">
                  <IconButton aria-label="共有URLをコピー" onClick={handleCopyShareUrl}>
                    <ContentCopyIcon />
                  </IconButton>
                </Tooltip>
              </Box>
              <Box>
                <Button
                  variant="outlined"
                  startIcon={<AutorenewIcon />}
                  onClick={() => setRegenerating(true)}
                >
                  再発行する
                </Button>
              </Box>
            </Stack>
          ) : (
            <Button
              variant="contained"
              startIcon={
                regenerateMutation.isPending ? (
                  <CircularProgress size={16} color="inherit" />
                ) : (
                  <LinkIcon />
                )
              }
              onClick={handleRegenerate}
              disabled={regenerateMutation.isPending}
              sx={{ mt: 1 }}
            >
              共有URLを発行する
            </Button>
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
        open={regenerating}
        title="共有URLを再発行しますか?"
        message="現在のURLは無効になり、配布済みのQRコード・リンクからはアクセスできなくなります。"
        confirmLabel="再発行する"
        loading={regenerateMutation.isPending}
        onConfirm={handleRegenerate}
        onCancel={() => setRegenerating(false)}
      />
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
