import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { TeamMatchResultControl } from '../components/features/team/TeamMatchResultControl';
import { teamTableLabel } from '../components/features/team/teamMatchDisplay';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { ErrorState, FullPageSpinner } from '../components/ui/QueryStates';
import { useInputSharedTeamMatchResult, useSharedTournament } from '../hooks/useShared';
import { useSnackbar } from '../hooks/useSnackbar';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import type { MatchResult, MatchSide } from '../types/enums';
import { boardPositionLabel } from '../utils/labels';

/**
 * S11 共有・結果自己申告の団体戦版。
 * 「あなたはどちらのチームですか」→ 主将戦〜のボードごとに自分から見た勝敗を選ぶ →
 * 確認 → 送信、の順で自己申告する。ボードごとに両者の申告が一致すると自動確定する
 * (一部ボードのみ不一致でも他のボードの確定は妨げない)。
 * 送信成功時は組み合わせタブ(paths.shared)へ遷移する(個人戦のSharedResultPageと同じ理由)
 */
export function SharedTeamResultPage() {
  const { token = '', mid = '' } = useParams();
  const navigate = useNavigate();
  const { data, isPending, isError, refetch } = useSharedTournament(token);
  const inputMutation = useInputSharedTeamMatchResult(token);
  const { showSuccess, showError } = useSnackbar();
  const [side, setSide] = useState<MatchSide | null>(null);
  const [selections, setSelections] = useState<Record<number, MatchResult>>({});
  const [confirmOpen, setConfirmOpen] = useState(false);

  if (isPending) {
    return <FullPageSpinner />;
  }
  if (isError) {
    return <ErrorState message="対局情報の取得に失敗しました" onRetry={() => void refetch()} />;
  }

  const { tournament, teamRounds, teamStandings } = data;
  const rounds = teamRounds ?? [];
  const standings = teamStandings ?? [];
  const round = rounds.find((r) => r.matches.some((m) => m.id === mid)) ?? null;
  const match = round?.matches.find((m) => m.id === mid) ?? null;
  const multiGroup = standings.length > 1;

  const backButton = (
    <Button variant="text" startIcon={<ArrowBackIcon />} component={Link} to={paths.shared(token)}>
      組み合わせへ戻る
    </Button>
  );

  if (round === null || match === null || match.team2 === null) {
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

  const allSelected = match.boardResults.every((b) => selections[b.boardPosition] !== undefined);

  const handlePickSide = (newSide: MatchSide) => {
    setSide(newSide);
    setSelections({});
  };

  const handlePickBoard = (boardPosition: number, result: MatchResult) => {
    setSelections((prev) => ({ ...prev, [boardPosition]: result }));
  };

  const summaryText = match.boardResults
    .map((b) => {
      const label = outcomeOptions.find((o) => o.result === selections[b.boardPosition])?.label;
      return `${boardPositionLabel(b.boardPosition)}: ${label ?? ''}`;
    })
    .join(' / ');

  const handleConfirm = () => {
    if (side === null || !allSelected) return;
    const boardResults: MatchResult[] = match.boardResults.map(
      (b) => selections[b.boardPosition] ?? 'NONE',
    );
    inputMutation.mutate(
      { matchId: match.id, input: { reportedBy: side, boardResults, version: match.version } },
      {
        onSuccess: () => {
          showSuccess('申告を送信しました');
          navigate(paths.shared(token));
        },
        onError: (error) => {
          setConfirmOpen(false);
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
            第{round.roundNumber}ラウンド・{teamTableLabel(match, multiGroup)}卓
          </Typography>
          <Typography variant="h2" component="h1" sx={{ mt: 1 }}>
            {match.team1.name} vs {match.team2.name}
          </Typography>

          <Box sx={{ mt: 1.5 }}>
            <TeamMatchResultControl
              match={match}
              editable={false}
              multiGroup={multiGroup}
              saving={false}
              onInput={() => {}}
            />
          </Box>

          {inputClosed ? (
            <Typography variant="body1" color="text.secondary" sx={{ mt: 2 }}>
              この対局の結果入力は締め切られています。修正が必要な場合は運営者に連絡してください。
            </Typography>
          ) : side === null ? (
            <Box sx={{ mt: 3 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                あなたはどちらのチームですか?
              </Typography>
              <Stack spacing={1.5} sx={{ mt: 1 }}>
                <Button
                  variant="outlined"
                  size="large"
                  onClick={() => handlePickSide('PLAYER1')}
                  disabled={inputMutation.isPending}
                >
                  {match.team1.name}
                </Button>
                <Button
                  variant="outlined"
                  size="large"
                  onClick={() => handlePickSide('PLAYER2')}
                  disabled={inputMutation.isPending}
                >
                  {match.team2.name}
                </Button>
              </Stack>
            </Box>
          ) : (
            <Box sx={{ mt: 3 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                各ボードの結果を選んでください
              </Typography>
              <Stack spacing={1.5} sx={{ mt: 1 }}>
                {match.boardResults.map((board) => (
                  <TextField
                    key={board.boardPosition}
                    select
                    label={boardPositionLabel(board.boardPosition)}
                    value={selections[board.boardPosition] ?? ''}
                    onChange={(e) =>
                      handlePickBoard(board.boardPosition, e.target.value as MatchResult)
                    }
                    disabled={inputMutation.isPending}
                  >
                    <MenuItem value="" disabled>
                      未選択
                    </MenuItem>
                    {outcomeOptions.map((option) => (
                      <MenuItem key={option.result} value={option.result}>
                        {option.label}
                      </MenuItem>
                    ))}
                  </TextField>
                ))}
              </Stack>
              <Stack direction="row" spacing={1.5} sx={{ mt: 2 }}>
                <Button
                  variant="contained"
                  disabled={!allSelected || inputMutation.isPending}
                  onClick={() => setConfirmOpen(true)}
                >
                  確認して申告する
                </Button>
                <Button
                  variant="text"
                  onClick={() => {
                    setSide(null);
                    setSelections({});
                  }}
                  disabled={inputMutation.isPending}
                >
                  選び直す
                </Button>
              </Stack>
            </Box>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={confirmOpen}
        title="結果を申告しますか?"
        message={`次の内容で申告します: ${summaryText}。相手チームの申告と一致したボードから確定します。`}
        confirmLabel="申告する"
        confirmColor="primary"
        loading={inputMutation.isPending}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmOpen(false)}
      />
    </Container>
  );
}
