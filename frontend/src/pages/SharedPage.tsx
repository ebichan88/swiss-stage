import EditIcon from '@mui/icons-material/Edit';
import EventSeatIcon from '@mui/icons-material/EventSeat';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  MenuItem,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import {
  hasReportMismatch,
  matchReportStatus,
  matchResultText,
  matchSections,
  resultMark,
  tableLabel,
} from '../components/features/round/matchDisplay';
import { CrossTable } from '../components/features/standing/CrossTable';
import { StandingsTable } from '../components/features/standing/StandingsTable';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, FullPageSpinner } from '../components/ui/QueryStates';
import { StatusBadge } from '../components/ui/StatusBadge';
import { useSharedTournament } from '../hooks/useShared';
import { ApiError } from '../services/apiClient';
import { paths } from '../routes';
import type { Match, Round } from '../types/round';
import type { ParticipantSummary } from '../types/participant';

/**
 * 対局カードの状態テキスト。申告待ち・不一致・確定後の食い違いは結果と区別して案内する。
 * 「結果入力から確認できます」という案内は、実際に結果入力の導線(canReview)がある場合のみ付ける
 * (ラウンド確定後は結果入力ボタンが出ないため、案内だけ残ると誤誘導になる)
 */
function matchStatusText(match: Match, canReview: boolean): string {
  switch (matchReportStatus(match)) {
    case 'WAITING':
      return '申告待ち(片方のみ申告済み)';
    case 'CONFLICTING':
      return canReview
        ? '申告が一致しません(結果入力から内容を確認できます)'
        : '申告が一致しません';
    default:
      if (hasReportMismatch(match)) {
        return canReview
          ? `${matchResultText(match)}(申告が一致しません・結果入力から内容を確認できます)`
          : `${matchResultText(match)}(申告が一致しません)`;
      }
      return matchResultText(match);
  }
}

function playerLabel(player: ParticipantSummary | null, mark: string | null): string {
  if (player === null) {
    return '(不戦勝)';
  }
  const name = player.organization ? `${player.name}(${player.organization})` : player.name;
  return mark ? `${mark} ${name}` : name;
}

interface SharedMatchCardProps {
  token: string;
  match: Match;
  multiGroup: boolean;
  canInput: boolean;
}

/** 共有ページの1対局カード(スマホ優先: 卓番号・対戦・結果・入力導線) */
function SharedMatchCard({ token, match, multiGroup, canInput }: SharedMatchCardProps) {
  // 「結果入力」ボタンの表示条件と揃える(BYEはボタンが出ないため案内文でも触れない)
  const canReview = canInput && match.player2 !== null;
  return (
    <Card variant="outlined">
      <CardContent
        sx={{ display: 'flex', alignItems: 'center', gap: 2, '&:last-child': { pb: 2 } }}
      >
        <Typography variant="h3" component="span" sx={{ flexShrink: 0 }}>
          {tableLabel(match, multiGroup)}卓
        </Typography>
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="body1">
            {playerLabel(match.player1, resultMark(match, 'player1'))}
          </Typography>
          <Typography variant="body1">
            {playerLabel(match.player2, resultMark(match, 'player2'))}
          </Typography>
          <Typography
            variant="body2"
            color={
              matchReportStatus(match) === 'CONFLICTING' || hasReportMismatch(match)
                ? 'warning.main'
                : 'text.secondary'
            }
          >
            {matchStatusText(match, canReview)}
          </Typography>
        </Box>
        {canReview && (
          <Button
            variant={match.result === 'NONE' ? 'contained' : 'outlined'}
            size="small"
            startIcon={<EditIcon />}
            component={Link}
            to={paths.sharedMatch(token, match.id)}
            sx={{ flexShrink: 0 }}
          >
            結果入力
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

/**
 * S10 共有ページ(参加者向け・スマホ優先)。
 * ヘッダーに大会名と現在ラウンドを常時表示し、組み合わせ/順位表をタブで切り替える
 */
export function SharedPage() {
  const { token = '' } = useParams();
  const { data, isPending, isError, error, refetch } = useSharedTournament(token);
  const [tab, setTab] = useState<'pairings' | 'standings' | 'crosstable'>('pairings');
  const [selectedRound, setSelectedRound] = useState<number | null>(null);

  if (isPending) {
    return <FullPageSpinner />;
  }
  if (isError) {
    if (error instanceof ApiError && error.code === 'INVALID_SHARE_TOKEN') {
      return (
        <Container maxWidth="sm" sx={{ py: 8, textAlign: 'center' }}>
          <Typography variant="h2" component="h1" gutterBottom>
            このURLは無効です
          </Typography>
          <Typography variant="body1" color="text.secondary">
            共有URLが再発行されたか、大会が非公開になった可能性があります。運営者に確認してください。
          </Typography>
        </Container>
      );
    }
    return <ErrorState message="大会情報の取得に失敗しました" onRetry={() => void refetch()} />;
  }

  const { tournament, rounds, standings } = data;
  const latestRound: Round | null = rounds.length > 0 ? rounds[rounds.length - 1] : null;
  const currentRound = rounds.find((round) => round.roundNumber === selectedRound) ?? latestRound;
  // グループが1つだけの大会は表示上グループを見せない(見出し・卓番号プレフィックスなし)
  const multiGroup = standings.length > 1;
  const canInput =
    tournament.resultInputEnabled &&
    tournament.status === 'IN_PROGRESS' &&
    currentRound !== null &&
    currentRound.status !== 'CONFIRMED';

  return (
    <Container maxWidth="md" sx={{ py: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap' }}>
        <Typography variant="h2" component="h1">
          {tournament.name}
        </Typography>
        <StatusBadge status={tournament.status} />
      </Box>
      {tournament.currentRound > 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          第{tournament.currentRound}ラウンド / 全{tournament.totalRounds}ラウンド
        </Typography>
      )}

      <Tabs
        value={tab}
        onChange={(_event, newTab: 'pairings' | 'standings' | 'crosstable') => setTab(newTab)}
        sx={{ mt: 2, mb: 3, borderBottom: 1, borderColor: 'divider' }}
        variant="fullWidth"
      >
        <Tab label="組み合わせ" value="pairings" />
        <Tab label="順位表" value="standings" />
        <Tab label="戦績一覧" value="crosstable" />
      </Tabs>

      {tab === 'pairings' &&
        (currentRound === null ? (
          <EmptyState
            icon={<HourglassEmptyIcon fontSize="inherit" />}
            message={
              tournament.status === 'PREPARING'
                ? '大会開始前です。組み合わせが発表されるまでお待ちください。'
                : '組み合わせはまだありません。'
            }
          />
        ) : (
          <Stack spacing={2}>
            {rounds.length > 1 && (
              <TextField
                select
                size="small"
                label="ラウンド"
                value={currentRound.roundNumber}
                onChange={(e) => setSelectedRound(Number(e.target.value))}
                sx={{ alignSelf: 'flex-start', minWidth: 160 }}
              >
                {rounds.map((round) => (
                  <MenuItem key={round.roundNumber} value={round.roundNumber}>
                    第{round.roundNumber}ラウンド
                  </MenuItem>
                ))}
              </TextField>
            )}
            {canInput && (
              <Typography variant="body2" color="text.secondary">
                対局が終わったら「結果入力」から勝敗を登録してください。
              </Typography>
            )}
            {currentRound.matches.length === 0 ? (
              <EmptyState
                icon={<EventSeatIcon fontSize="inherit" />}
                message="このラウンドの対局はありません。"
              />
            ) : (
              matchSections(currentRound.matches).map(({ group, matches }) => (
                <Stack key={group.id} spacing={1.5}>
                  {multiGroup && (
                    <Typography variant="h4" component="h2">
                      {group.name}
                    </Typography>
                  )}
                  {matches.map((match) => (
                    <SharedMatchCard
                      key={match.id}
                      token={token}
                      match={match}
                      multiGroup={multiGroup}
                      canInput={canInput}
                    />
                  ))}
                </Stack>
              ))
            )}
          </Stack>
        ))}

      {tab === 'standings' &&
        (standings.every((g) => g.standings.length === 0) ? (
          <EmptyState
            icon={<HourglassEmptyIcon fontSize="inherit" />}
            message="順位はまだありません。"
          />
        ) : (
          standings.map(({ group, standings: groupStandings }) => (
            <Box key={group.id} sx={{ mb: 3 }}>
              {multiGroup && (
                <Typography variant="h4" component="h2" sx={{ mb: 1 }}>
                  {group.name}
                </Typography>
              )}
              <StandingsTable standings={groupStandings} />
            </Box>
          ))
        ))}

      {tab === 'crosstable' &&
        (standings.every((g) => g.standings.length === 0) ? (
          <EmptyState
            icon={<HourglassEmptyIcon fontSize="inherit" />}
            message="戦績はまだありません。"
          />
        ) : (
          standings.map(({ group, standings: groupStandings }) => (
            <Box key={group.id} sx={{ mb: 3 }}>
              {multiGroup && (
                <Typography variant="h4" component="h2" sx={{ mb: 1 }}>
                  {group.name}
                </Typography>
              )}
              <CrossTable
                rounds={rounds.map((round) => ({
                  ...round,
                  matches: round.matches.filter((m) => m.group.id === group.id),
                }))}
                standings={groupStandings}
              />
            </Box>
          ))
        ))}
    </Container>
  );
}
