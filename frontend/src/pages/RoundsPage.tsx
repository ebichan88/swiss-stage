import AddIcon from '@mui/icons-material/Add';
import FormatListNumberedIcon from '@mui/icons-material/FormatListNumbered';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Link as MuiLink,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import { PairingTable } from '../components/features/round/PairingTable';
import {
  hasReportMismatch,
  matchReportStatus,
  matchSections,
} from '../components/features/round/matchDisplay';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { RoundStatusBadge } from '../components/ui/StatusBadge';
import { useGroups } from '../hooks/useGroups';
import {
  useConfirmRound,
  useGenerateNextRound,
  useInputMatchResult,
  useRounds,
} from '../hooks/useRounds';
import { useSnackbar } from '../hooks/useSnackbar';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import type { MatchResult } from '../types/enums';
import type { Match } from '../types/round';
import { relaxationLabel } from '../utils/labels';

/** S07 ラウンド管理(組み合わせ生成・結果入力・ラウンド確定) */
export function RoundsPage() {
  const tournament = useTournamentContext();
  const { data: rounds, isPending, isError, refetch } = useRounds(tournament.id);
  const { data: groups } = useGroups(tournament.id);
  const generateMutation = useGenerateNextRound(tournament.id);
  const confirmMutation = useConfirmRound(tournament.id);
  const inputResultMutation = useInputMatchResult(tournament.id);
  const { showSuccess, showError } = useSnackbar();
  const [searchParams, setSearchParams] = useSearchParams();
  const [confirmingRound, setConfirmingRound] = useState<number | null>(null);
  // 直近の生成で緩和された制約(REMATCH等)。該当ラウンド表示中は警告を出し続ける
  const [relaxations, setRelaxations] = useState<{ roundNumber: number; codes: string[] } | null>(
    null,
  );

  const errorMessage = (error: unknown, fallback: string) =>
    error instanceof ApiError ? error.message : fallback;

  if (isPending) {
    return <LoadingState />;
  }
  if (isError) {
    return <ErrorState message="ラウンド一覧の取得に失敗しました" onRetry={() => void refetch()} />;
  }

  const latestRoundNumber = rounds.length > 0 ? rounds[rounds.length - 1].roundNumber : 0;
  const requestedRound = Number(searchParams.get('round'));
  const selectedRoundNumber = rounds.some((r) => r.roundNumber === requestedRound)
    ? requestedRound
    : latestRoundNumber;
  const selectedRound = rounds.find((r) => r.roundNumber === selectedRoundNumber) ?? null;

  const canGenerateNext =
    tournament.status === 'IN_PROGRESS' &&
    tournament.currentRound < tournament.totalRounds &&
    rounds.every((r) => r.status === 'CONFIRMED');

  const handleGenerate = () => {
    generateMutation.mutate(undefined, {
      onSuccess: (generated) => {
        setRelaxations({
          roundNumber: generated.round.roundNumber,
          codes: generated.relaxations,
        });
        setSearchParams({ round: String(generated.round.roundNumber) });
        showSuccess(`第${generated.round.roundNumber}ラウンドの組み合わせを生成しました`);
      },
      onError: (error) => showError(errorMessage(error, '組み合わせの生成に失敗しました')),
    });
  };

  const handleConfirm = () => {
    if (confirmingRound === null) return;
    confirmMutation.mutate(confirmingRound, {
      onSuccess: (round) => {
        setConfirmingRound(null);
        showSuccess(`第${round.roundNumber}ラウンドを確定しました`);
      },
      onError: (error) => {
        setConfirmingRound(null);
        showError(errorMessage(error, 'ラウンドの確定に失敗しました'));
      },
    });
  };

  const handleInputResult = (match: Match, result: MatchResult) => {
    inputResultMutation.mutate(
      { matchId: match.id, input: { result, version: match.version } },
      {
        onError: (error) => showError(errorMessage(error, '結果の入力に失敗しました')),
      },
    );
  };

  if (tournament.status === 'PREPARING') {
    return (
      <Alert severity="info">
        大会を開始すると組み合わせを生成できます。
        <MuiLink component={Link} to={paths.tournament(tournament.id)} sx={{ ml: 1 }}>
          概要へ
        </MuiLink>
      </Alert>
    );
  }

  const generateButton = (
    <Button
      variant="contained"
      startIcon={
        generateMutation.isPending ? <CircularProgress size={16} color="inherit" /> : <AddIcon />
      }
      onClick={handleGenerate}
      disabled={generateMutation.isPending}
    >
      第{tournament.currentRound + 1}ラウンドの組み合わせを生成
    </Button>
  );

  if (rounds.length === 0) {
    return (
      <Box>
        <EmptyState
          icon={<FormatListNumberedIcon fontSize="inherit" />}
          message="ラウンドがまだありません"
          action={canGenerateNext ? generateButton : undefined}
        />
      </Box>
    );
  }

  // ラウンド確定をブロックするのは「運営者・参加者のいずれも一切触れていない」対局のみ。
  // 片方のみ申告・申告不一致(needsAttentionCount)はブロックせず警告のみ(運営者の裁量)
  const untouchedCount =
    selectedRound?.matches.filter(
      (m) =>
        m.result === 'NONE' &&
        m.player1ReportedResult === 'NONE' &&
        m.player2ReportedResult === 'NONE',
    ).length ?? 0;
  const needsAttentionCount =
    selectedRound?.matches.filter((m) => {
      const status = matchReportStatus(m);
      return status === 'WAITING' || status === 'CONFLICTING';
    }).length ?? 0;
  const mismatchCount = selectedRound?.matches.filter((m) => hasReportMismatch(m)).length ?? 0;
  const isEditable = tournament.status === 'IN_PROGRESS' && selectedRound?.status !== 'CONFIRMED';
  const sections = selectedRound ? matchSections(selectedRound.matches) : [];
  // 表示判定は大会に定義されたグループ総数で行う(そのラウンドに対局があるグループ数ではない)。
  // 全員棄権でスキップされたグループがあっても他画面(順位表・共有)と表示形式を揃える
  const multiGroup = (groups ?? []).length > 1;

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 1,
          flexWrap: 'wrap',
        }}
      >
        <Tabs
          value={selectedRoundNumber}
          onChange={(_e, value: number) => setSearchParams({ round: String(value) })}
          variant="scrollable"
          scrollButtons="auto"
        >
          {rounds.map((round) => (
            <Tab
              key={round.roundNumber}
              value={round.roundNumber}
              label={`第${round.roundNumber}R`}
            />
          ))}
        </Tabs>
        {canGenerateNext && generateButton}
      </Box>

      {selectedRound && (
        <Box sx={{ mt: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2, flexWrap: 'wrap' }}>
            <Typography variant="h3" component="h2">
              第{selectedRound.roundNumber}ラウンド
            </Typography>
            <RoundStatusBadge status={selectedRound.status} />
            {isEditable && untouchedCount > 0 && (
              <Typography variant="body2" color="text.secondary">
                未入力 {untouchedCount}件
              </Typography>
            )}
            {isEditable && (
              <Button
                variant="contained"
                sx={{ ml: 'auto' }}
                onClick={() => setConfirmingRound(selectedRound.roundNumber)}
                disabled={untouchedCount > 0}
              >
                第{selectedRound.roundNumber}ラウンドを確定する
              </Button>
            )}
          </Box>

          {relaxations &&
            relaxations.roundNumber === selectedRound.roundNumber &&
            relaxations.codes.length > 0 && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                制約を緩和して組み合わせを生成しました:
                <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
                  {relaxations.codes.map((code) => (
                    <li key={code}>{relaxationLabel(code)}</li>
                  ))}
                </Box>
              </Alert>
            )}

          {isEditable && untouchedCount > 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>
              全対局の結果を入力するとラウンドを確定できます。確定すると次のラウンドを生成できます。
            </Alert>
          )}

          {isEditable && needsAttentionCount > 0 && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              参加者の申告待ち・申告不一致の対局が{needsAttentionCount}
              件あります。内容を確認してから
              結果を入力・確定してください(確定のブロックはしません)。
            </Alert>
          )}

          {isEditable && mismatchCount > 0 && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              確定済みですが参加者の申告と異なる対局が{mismatchCount}
              件あります。対局の申告内容を確認し、必要なら結果を修正してください。
            </Alert>
          )}

          {sections.map(({ group, matches }) => (
            <Box key={group.id} sx={{ mb: 3 }}>
              {multiGroup && (
                <Typography variant="h4" component="h3" sx={{ mb: 1 }}>
                  {group.name}
                </Typography>
              )}
              <PairingTable
                matches={matches}
                editable={isEditable}
                multiGroup={multiGroup}
                savingMatchId={
                  inputResultMutation.isPending
                    ? (inputResultMutation.variables?.matchId ?? null)
                    : null
                }
                onInputResult={handleInputResult}
              />
            </Box>
          ))}
        </Box>
      )}

      <ConfirmDialog
        open={confirmingRound !== null}
        title={`第${confirmingRound ?? ''}ラウンドを確定しますか?`}
        message="確定後はこのラウンドの結果を変更できません。順位に反映され、次のラウンドを生成できるようになります。"
        confirmLabel="確定する"
        confirmColor="primary"
        loading={confirmMutation.isPending}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmingRound(null)}
      />
    </Box>
  );
}
