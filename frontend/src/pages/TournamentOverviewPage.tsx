import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import { Alert, Box, Button, Card, CardContent, Grid, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { Link } from 'react-router-dom';

import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { useParticipants } from '../hooks/useParticipants';
import { useSnackbar } from '../hooks/useSnackbar';
import { useFinishTournament, useStartTournament } from '../hooks/useTournaments';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import { formatDateTime } from '../utils/format';
import { gameTypeLabels } from '../utils/labels';

/** S05 大会管理(概要) */
export function TournamentOverviewPage() {
  const tournament = useTournamentContext();
  const { data: participants } = useParticipants(tournament.id);
  const startMutation = useStartTournament(tournament.id);
  const finishMutation = useFinishTournament(tournament.id);
  const { showSuccess, showError } = useSnackbar();
  const [confirming, setConfirming] = useState<'start' | 'finish' | null>(null);

  const activeCount = participants?.filter((p) => p.status === 'ACTIVE').length ?? null;

  const handleStart = () => {
    startMutation.mutate(undefined, {
      onSuccess: () => {
        setConfirming(null);
        showSuccess('大会を開始しました');
      },
      onError: (error) => {
        setConfirming(null);
        showError(error instanceof ApiError ? error.message : '大会の開始に失敗しました');
      },
    });
  };

  const handleFinish = () => {
    finishMutation.mutate(undefined, {
      onSuccess: () => {
        setConfirming(null);
        showSuccess('大会を終了しました');
      },
      onError: (error) => {
        setConfirming(null);
        showError(error instanceof ApiError ? error.message : '大会の終了に失敗しました');
      },
    });
  };

  const items = [
    { label: '競技', value: gameTypeLabels[tournament.gameType] },
    {
      label: '進行',
      value:
        tournament.currentRound === 0
          ? `未開始(全${tournament.totalRounds}ラウンド)`
          : `第${tournament.currentRound} / 全${tournament.totalRounds}ラウンド`,
    },
    { label: '参加者', value: activeCount === null ? '-' : `${activeCount}名` },
    { label: '作成日時', value: formatDateTime(tournament.createdAt) },
  ];

  return (
    <Stack spacing={4}>
      <Card variant="outlined">
        <CardContent>
          <Grid container spacing={2}>
            {items.map((item) => (
              <Grid key={item.label} size={{ xs: 6, md: 3 }}>
                <Typography variant="body2" color="text.secondary">
                  {item.label}
                </Typography>
                <Typography variant="h3" component="p">
                  {item.value}
                </Typography>
              </Grid>
            ))}
          </Grid>
        </CardContent>
      </Card>

      {tournament.status === 'PREPARING' && (
        <Box>
          {activeCount !== null && activeCount < 2 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              大会の開始には参加者が2名以上必要です。
              <Button
                component={Link}
                to={paths.participants(tournament.id)}
                size="small"
                sx={{ ml: 1 }}
              >
                参加者を登録する
              </Button>
            </Alert>
          )}
          <Button
            variant="contained"
            size="large"
            startIcon={<PlayArrowIcon />}
            onClick={() => setConfirming('start')}
            disabled={activeCount !== null && activeCount < 2}
          >
            大会を開始する
          </Button>
        </Box>
      )}

      {tournament.status === 'IN_PROGRESS' && (
        <Box>
          <Button
            variant="outlined"
            color="error"
            startIcon={<StopIcon />}
            onClick={() => setConfirming('finish')}
          >
            大会を終了する
          </Button>
        </Box>
      )}

      <ConfirmDialog
        open={confirming === 'start'}
        title="大会を開始しますか?"
        message="開始すると参加者の追加・削除はできなくなります(棄権処理は可能です)。"
        confirmLabel="開始する"
        confirmColor="primary"
        loading={startMutation.isPending}
        onConfirm={handleStart}
        onCancel={() => setConfirming(null)}
      />
      <ConfirmDialog
        open={confirming === 'finish'}
        title="大会を終了しますか?"
        message="終了すると組み合わせ生成や結果入力はできなくなります。"
        confirmLabel="終了する"
        loading={finishMutation.isPending}
        onConfirm={handleFinish}
        onCancel={() => setConfirming(null)}
      />
    </Stack>
  );
}
