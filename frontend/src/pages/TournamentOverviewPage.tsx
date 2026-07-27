import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import PrintIcon from '@mui/icons-material/Print';
import StopIcon from '@mui/icons-material/Stop';
import { Alert, Box, Button, Card, CardContent, Grid, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { Link } from 'react-router-dom';

import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { useParticipants } from '../hooks/useParticipants';
import { useSnackbar } from '../hooks/useSnackbar';
import { useTeams } from '../hooks/useTeams';
import { useFinishTournament, useStartTournament } from '../hooks/useTournaments';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import type { Tournament } from '../types/tournament';
import { formatDateTime, formatEventDate } from '../utils/format';
import { gameTypeLabels } from '../utils/labels';

/**
 * S05 大会管理(概要)。団体戦(competitionType=TEAM)はチーム数、個人戦は参加者数で開始条件を判定する。
 * hooksを条件分岐なしで呼ぶため、本体は競技形式ごとに別コンポーネントに分ける
 */
export function TournamentOverviewPage() {
  const tournament = useTournamentContext();
  if (tournament.competitionType === 'TEAM') {
    return <TeamTournamentOverview tournament={tournament} />;
  }
  return <IndividualTournamentOverview tournament={tournament} />;
}

function TeamTournamentOverview({ tournament }: { tournament: Tournament }) {
  const { data: teams } = useTeams(tournament.id);
  const activeCount = teams?.filter((t) => t.status === 'ACTIVE').length ?? null;
  return (
    <TournamentOverviewView
      tournament={tournament}
      activeCount={activeCount}
      entryLabel="チーム"
      entryUnit="チーム"
      startRequirementMessage="大会の開始には2チーム以上が必要です。"
      registerActionLabel="チームを登録する"
    />
  );
}

function IndividualTournamentOverview({ tournament }: { tournament: Tournament }) {
  const { data: participants } = useParticipants(tournament.id);
  const activeCount = participants?.filter((p) => p.status === 'ACTIVE').length ?? null;
  return (
    <TournamentOverviewView
      tournament={tournament}
      activeCount={activeCount}
      entryLabel="参加者"
      entryUnit="名"
      startRequirementMessage="大会の開始には参加者が2名以上必要です。"
      registerActionLabel="参加者を登録する"
    />
  );
}

function TournamentOverviewView({
  tournament,
  activeCount,
  entryLabel,
  entryUnit,
  startRequirementMessage,
  registerActionLabel,
}: {
  tournament: Tournament;
  activeCount: number | null;
  entryLabel: string;
  entryUnit: string;
  startRequirementMessage: string;
  registerActionLabel: string;
}) {
  const startMutation = useStartTournament(tournament.id);
  const finishMutation = useFinishTournament(tournament.id);
  const { showSuccess, showError } = useSnackbar();
  const [confirming, setConfirming] = useState<'start' | 'finish' | null>(null);

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
    { label: entryLabel, value: activeCount === null ? '-' : `${activeCount}${entryUnit}` },
    { label: '開催日', value: formatEventDate(tournament.eventDate) || '未設定' },
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

      <Card variant="outlined">
        <CardContent>
          <Typography variant="h3" component="h2" gutterBottom>
            帳票印刷
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            スマホを持たない参加者向けに、名簿・対局カード・戦績一覧表を紙で印刷できます。
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 1 }}>
            <Button
              variant="outlined"
              startIcon={<PrintIcon />}
              component={Link}
              to={paths.printRoster(tournament.id)}
            >
              名簿を印刷
            </Button>
            <Button
              variant="outlined"
              startIcon={<PrintIcon />}
              component={Link}
              to={paths.printCrossTable(tournament.id)}
            >
              戦績一覧表を印刷
            </Button>
            <Button
              variant="outlined"
              startIcon={<PrintIcon />}
              component={Link}
              to={paths.printMatchCards(tournament.id)}
            >
              対局カードを印刷
            </Button>
          </Box>
        </CardContent>
      </Card>

      {tournament.status === 'PREPARING' && (
        <Box>
          {activeCount !== null && activeCount < 2 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              {startRequirementMessage}
              <Button
                component={Link}
                to={paths.participants(tournament.id)}
                size="small"
                sx={{ ml: 1 }}
              >
                {registerActionLabel}
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
