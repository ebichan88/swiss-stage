import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { Box, Button, Card, CardContent, Container, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { matchResultText, tableLabel } from '../components/features/round/matchDisplay';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { ErrorState, FullPageSpinner } from '../components/ui/QueryStates';
import { useInputSharedResult, useSharedTournament } from '../hooks/useShared';
import { useSnackbar } from '../hooks/useSnackbar';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import type { MatchResult } from '../types/enums';

/**
 * S11 共有・結果入力(スマホ優先)。結果をタップ → 確認 → 完了の2タップで登録する。
 * 送信失敗(電波状況等)はスナックバーで通知し、同じ画面から再試行できる
 */
export function SharedResultPage() {
  const { token = '', mid = '' } = useParams();
  const { data, isPending, isError, refetch } = useSharedTournament(token);
  const inputMutation = useInputSharedResult(token);
  const navigate = useNavigate();
  const { showSuccess, showError } = useSnackbar();
  const [selected, setSelected] = useState<MatchResult | null>(null);

  if (isPending) {
    return <FullPageSpinner />;
  }
  if (isError) {
    return <ErrorState message="対局情報の取得に失敗しました" onRetry={() => void refetch()} />;
  }

  const { tournament, rounds } = data;
  const round = rounds.find((r) => r.matches.some((m) => m.id === mid)) ?? null;
  const match = round?.matches.find((m) => m.id === mid) ?? null;

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

  const handleConfirm = () => {
    if (selected === null) return;
    inputMutation.mutate(
      { matchId: match.id, input: { result: selected, version: match.version } },
      {
        onSuccess: () => {
          setSelected(null);
          showSuccess('結果を登録しました');
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

  const options: { result: MatchResult; label: string }[] = [
    { result: 'PLAYER1_WIN', label: `${match.player1.name} の勝ち` },
    { result: 'PLAYER2_WIN', label: `${match.player2.name} の勝ち` },
    { result: 'DRAW', label: '引き分け' },
    { result: 'BOTH_LOSE', label: '両者負け' },
  ];
  const selectedLabel = options.find((o) => o.result === selected)?.label ?? '';

  return (
    <Container maxWidth="sm" sx={{ py: 4 }}>
      {backButton}
      <Card variant="outlined" sx={{ mt: 2 }}>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            第{round.roundNumber}ラウンド・{tableLabel(match)}卓
          </Typography>
          <Typography variant="h2" component="h1" sx={{ mt: 1 }}>
            {match.player1.name} vs {match.player2.name}
          </Typography>
          {match.result !== 'NONE' && (
            <Typography variant="body1" sx={{ mt: 1 }}>
              現在の結果: {matchResultText(match)}
            </Typography>
          )}
          {inputClosed ? (
            <Typography variant="body1" color="text.secondary" sx={{ mt: 2 }}>
              この対局の結果入力は締め切られています。修正が必要な場合は運営者に連絡してください。
            </Typography>
          ) : (
            <Box sx={{ mt: 3 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                結果を選んでください
              </Typography>
              <Stack spacing={1.5} sx={{ mt: 1 }}>
                {options.map((option) => (
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
            </Box>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={selected !== null}
        title="結果を登録しますか?"
        message={`「${selectedLabel}」で登録します。`}
        confirmLabel="登録する"
        confirmColor="primary"
        loading={inputMutation.isPending}
        onConfirm={handleConfirm}
        onCancel={() => setSelected(null)}
      />
    </Container>
  );
}
