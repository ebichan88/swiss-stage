import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { Alert, Box, Button, Card, CardContent, Container, Stack, Typography } from '@mui/material';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import {
  hasReportMismatch,
  matchReportStatus,
  matchResultText,
  reportedResultLabel,
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

  // このページは個人戦専用(団体戦はSharedTeamResultPage)。rounds/standingsは常に非null
  const { tournament, rounds: individualRounds, standings: individualStandings } = data;
  const rounds = individualRounds ?? [];
  const standings = individualStandings ?? [];
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

/**
 * 現在の確定状況・申告状況の表示。
 * 申告が1件でもあれば「誰が何を申告したか」を常に表示する(送信後に自分の申告内容を
 * いつでも確認できるようにし、「本当に勝ちを申告したっけ?」というトラブルを防ぐ)
 */
function ReportStatus({ match }: { match: Match }) {
  const status = matchReportStatus(match);
  const anyReported =
    match.player1ReportedResult !== 'NONE' || match.player2ReportedResult !== 'NONE';
  const mismatch = hasReportMismatch(match);

  return (
    <>
      {status === 'DECIDED' && (
        <Typography variant="body1" sx={{ mt: 1 }}>
          現在の結果: {matchResultText(match)}
        </Typography>
      )}
      {anyReported && (
        <Box sx={{ mt: 1 }}>
          <Typography variant="body2" color="text.secondary">
            {match.player1.name}の申告: {reportedResultLabel(match, 'player1')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {match.player2?.name ?? ''}の申告: {reportedResultLabel(match, 'player2')}
          </Typography>
        </Box>
      )}
      {status === 'WAITING' && (
        <Alert severity="info" sx={{ mt: 2 }}>
          もう一方の申告をお待ちください。
        </Alert>
      )}
      {status === 'CONFLICTING' && (
        <Alert severity="warning" sx={{ mt: 2 }}>
          両者の申告が一致しませんでした。内容を確認のうえ、再度申告するか運営者に連絡してください。
        </Alert>
      )}
      {mismatch && (
        <Alert severity="warning" sx={{ mt: 2 }}>
          確定結果と自己申告の内容が異なります。内容に誤りがある場合は運営者に直接お知らせください。
        </Alert>
      )}
    </>
  );
}
