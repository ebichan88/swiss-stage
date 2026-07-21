import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { Alert, Box, Button, Card, CardContent, Container, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import {
  matchReportStatus,
  matchResultText,
  tableLabel,
} from '../components/features/round/matchDisplay';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { ErrorState, FullPageSpinner } from '../components/ui/QueryStates';
import { useInputSharedResult, useSharedTournament } from '../hooks/useShared';
import { useSnackbar } from '../hooks/useSnackbar';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import type { MatchResult, MatchSide } from '../types/enums';
import type { Match } from '../types/round';

/**
 * S11 共有・結果入力(スマホ優先)。
 * 「あなたはどちらですか」→ 自分から見た勝敗を選ぶ → 確認 → 送信、の順で自己申告する。
 * 両者の申告が一致すると対局結果が自動確定する(片方だけでは確定しない)。
 * 送信失敗(電波状況等)はスナックバーで通知し、同じ画面から再試行できる。
 * 送信成功時は組み合わせタブ(paths.shared)へ遷移する(この画面に戻すと選択ミスで
 * 差し戻されたと誤解されるため)。
 */
export function SharedResultPage() {
  const { token = '', mid = '' } = useParams();
  const navigate = useNavigate();
  const { data, isPending, isError, refetch } = useSharedTournament(token);
  const inputMutation = useInputSharedResult(token);
  const { showSuccess, showError } = useSnackbar();
  const [side, setSide] = useState<MatchSide | null>(null);
  const [selected, setSelected] = useState<MatchResult | null>(null);

  if (isPending) {
    return <FullPageSpinner />;
  }
  if (isError) {
    return <ErrorState message="対局情報の取得に失敗しました" onRetry={() => void refetch()} />;
  }

  const { tournament, rounds, standings } = data;
  const round = rounds.find((r) => r.matches.some((m) => m.id === mid)) ?? null;
  const match = round?.matches.find((m) => m.id === mid) ?? null;
  const multiGroup = standings.length > 1;

  const backButton = (
    <Button variant="text" startIcon={<ArrowBackIcon />} component={Link} to={paths.shared(token)}>
      組み合わせへ戻る
    </Button>
  );

  if (round === null || match === null || match.player2 === null) {
    return (
      <Container maxWidth="sm" sx={{ py: 4 }}>
        {backButton}
        <Typography variant="body1" sx={{ mt: 2 }}>
          対局が見つかりません。組み合わせを確認してください。
        </Typography>
      </Container>
    );
  }

  const inputClosed =
    !tournament.resultInputEnabled ||
    tournament.status !== 'IN_PROGRESS' ||
    round.status === 'CONFIRMED';

  const outcomeOptions: { result: MatchResult; label: string }[] =
    side === 'PLAYER2'
      ? [
          { result: 'PLAYER2_WIN', label: '勝ち' },
          { result: 'PLAYER1_WIN', label: '負け' },
          { result: 'DRAW', label: '引き分け' },
          { result: 'BOTH_LOSE', label: '両者負け' },
        ]
      : [
          { result: 'PLAYER1_WIN', label: '勝ち' },
          { result: 'PLAYER2_WIN', label: '負け' },
          { result: 'DRAW', label: '引き分け' },
          { result: 'BOTH_LOSE', label: '両者負け' },
        ];
  const selectedLabel = outcomeOptions.find((o) => o.result === selected)?.label ?? '';

  const handleConfirm = () => {
    if (side === null || selected === null) return;
    inputMutation.mutate(
      { matchId: match.id, input: { reportedBy: side, result: selected, version: match.version } },
      {
        onSuccess: () => {
          showSuccess('申告を送信しました');
          navigate(paths.shared(token));
        },
        onError: (error) => {
          setSelected(null);
          showError(
            error instanceof ApiError ? error.message : '送信に失敗しました。再度お試しください',
          );
        },
      },
    );
  };

  return (
    <Container maxWidth="sm" sx={{ py: 4 }}>
      {backButton}
      <Card variant="outlined" sx={{ mt: 2 }}>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            第{round.roundNumber}ラウンド・{tableLabel(match, multiGroup)}卓
          </Typography>
          <Typography variant="h2" component="h1" sx={{ mt: 1 }}>
            {match.player1.name} vs {match.player2.name}
          </Typography>

          <ReportStatus match={match} />

          {inputClosed ? (
            <Typography variant="body1" color="text.secondary" sx={{ mt: 2 }}>
              この対局の結果入力は締め切られています。修正が必要な場合は運営者に連絡してください。
            </Typography>
          ) : side === null ? (
            <Box sx={{ mt: 3 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                あなたはどちらですか?
              </Typography>
              <Stack spacing={1.5} sx={{ mt: 1 }}>
                <Button
                  variant="outlined"
                  size="large"
                  onClick={() => setSide('PLAYER1')}
                  disabled={inputMutation.isPending}
                >
                  {match.player1.name}
                </Button>
                <Button
                  variant="outlined"
                  size="large"
                  onClick={() => setSide('PLAYER2')}
                  disabled={inputMutation.isPending}
                >
                  {match.player2.name}
                </Button>
              </Stack>
            </Box>
          ) : (
            <Box sx={{ mt: 3 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                あなたの結果を選んでください
              </Typography>
              <Stack spacing={1.5} sx={{ mt: 1 }}>
                {outcomeOptions.map((option) => (
                  <Button
                    key={option.result}
                    variant="outlined"
                    size="large"
                    onClick={() => setSelected(option.result)}
                    disabled={inputMutation.isPending}
                  >
                    {option.label}
                  </Button>
                ))}
              </Stack>
              <Button
                variant="text"
                size="small"
                sx={{ mt: 1.5 }}
                onClick={() => setSide(null)}
                disabled={inputMutation.isPending}
              >
                選び直す
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={selected !== null}
        title="結果を申告しますか?"
        message={`「${selectedLabel}」で申告します。相手の申告と一致すると結果が確定します。`}
        confirmLabel="申告する"
        confirmColor="primary"
        loading={inputMutation.isPending}
        onConfirm={handleConfirm}
        onCancel={() => setSelected(null)}
      />
    </Container>
  );
}

/** 現在の確定状況・申告状況の表示 */
function ReportStatus({ match }: { match: Match }) {
  const status = matchReportStatus(match);
  if (status === 'DECIDED') {
    return (
      <Typography variant="body1" sx={{ mt: 1 }}>
        現在の結果: {matchResultText(match)}
      </Typography>
    );
  }
  if (status === 'WAITING') {
    const reportedName =
      match.player1ReportedResult !== 'NONE' ? match.player1.name : match.player2?.name;
    return (
      <Alert severity="info" sx={{ mt: 2 }}>
        {reportedName}が申告しました。もう一方の申告をお待ちください。
      </Alert>
    );
  }
  if (status === 'CONFLICTING') {
    return (
      <Alert severity="warning" sx={{ mt: 2 }}>
        両者の申告が一致しませんでした。内容を確認のうえ、再度申告するか運営者に連絡してください。
      </Alert>
    );
  }
  return null;
}
