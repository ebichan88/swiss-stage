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
import { Link } from 'react-router-dom';

import { TeamMatchResultsTable } from '../components/features/team/TeamMatchResultsTable';
import { TeamRankingBoard } from '../components/features/team/TeamRankingBoard';
import {
  boardReportStatus,
  teamAggregatePoints,
  teamMatchHasReportMismatch,
  teamMatchSections,
  teamResultMark,
  teamTableLabel,
} from '../components/features/team/teamMatchDisplay';
import { EmptyState } from '../components/ui/EmptyState';
import { StatusBadge } from '../components/ui/StatusBadge';
import { paths } from '../routes';
import type { SharedTournament } from '../types/shared';
import type { TeamMatch, TeamRound, TeamSummary } from '../types/team';

/** 対局全体が確定済み(全ボードが入力済み)かどうか。個人戦の match.result === 'NONE' に相当する判定 */
function isFullyDecided(match: TeamMatch): boolean {
  return match.boardResults.every((b) => b.result !== 'NONE');
}

/** いずれかのボードが申告不一致(両者申告済みで内容が食い違う)か */
function hasConflictingBoard(match: TeamMatch): boolean {
  return match.boardResults.some((b) => boardReportStatus(b) === 'CONFLICTING');
}

/** いずれかのボードが片方のみ申告済みか */
function hasWaitingBoard(match: TeamMatch): boolean {
  return match.boardResults.some((b) => boardReportStatus(b) === 'WAITING');
}

/**
 * 団体戦対局カードの状態テキスト。個人戦のmatchStatusTextと同じ考え方で、
 * ボード単位の申告待ち・不一致は結果と区別して案内する
 */
function teamMatchStatusText(match: TeamMatch, canReview: boolean): string {
  if (match.team2 === null) {
    return '不戦勝';
  }
  if (!isFullyDecided(match)) {
    if (hasConflictingBoard(match)) {
      return canReview
        ? '申告が一致しないボードがあります(結果入力から内容を確認できます)'
        : '申告が一致しないボードがあります';
    }
    if (hasWaitingBoard(match)) {
      return '申告待ち(片方のみ申告済みのボードがあります)';
    }
    // 個人戦の「未入力」に相当。一部ボードだけ入力済みで残りが未入力の場合は「対局中」で区別する
    const anyBoardDecided = match.boardResults.some((b) => b.result !== 'NONE');
    return anyBoardDecided ? '対局中' : '未入力';
  }
  const { team1, team2 } = teamAggregatePoints(match);
  // teamAggregatePoints は2倍値(勝ち=2点)で返すため、盤数の内訳表示は2で割る(teamMatchResultsTableData.tsと同じ変換)
  const breakdown = `${team1 / 2}-${team2 / 2}`;
  if (teamMatchHasReportMismatch(match)) {
    return canReview
      ? `${breakdown}(申告が一致しないボードがあります・結果入力から内容を確認できます)`
      : `${breakdown}(申告が一致しないボードがあります)`;
  }
  return breakdown;
}

function teamLabel(team: TeamSummary | null, mark: string | null): string {
  if (team === null) {
    return '(不戦勝)';
  }
  return mark ? `${mark} ${team.name}` : team.name;
}

interface SharedTeamMatchCardProps {
  token: string;
  match: TeamMatch;
  multiGroup: boolean;
  canInput: boolean;
}

/** 共有ページの1団体戦対局カード(スマホ優先: 卓番号・対戦・結果・入力導線)。個人名は含めない */
function SharedTeamMatchCard({ token, match, multiGroup, canInput }: SharedTeamMatchCardProps) {
  const canReview = canInput && match.team2 !== null;
  return (
    <Card variant="outlined">
      <CardContent
        sx={{ display: 'flex', alignItems: 'center', gap: 2, '&:last-child': { pb: 2 } }}
      >
        <Typography variant="h3" component="span" sx={{ flexShrink: 0 }}>
          {teamTableLabel(match, multiGroup)}卓
        </Typography>
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="body1">
            {teamLabel(match.team1, teamResultMark(match, 'team1'))}
          </Typography>
          <Typography variant="body1">
            {teamLabel(match.team2, teamResultMark(match, 'team2'))}
          </Typography>
          <Typography
            variant="body2"
            color={
              hasConflictingBoard(match) || teamMatchHasReportMismatch(match)
                ? 'warning.main'
                : 'text.secondary'
            }
          >
            {teamMatchStatusText(match, canReview)}
          </Typography>
        </Box>
        {canReview && (
          <Button
            variant={!isFullyDecided(match) ? 'contained' : 'outlined'}
            size="small"
            startIcon={<EditIcon />}
            component={Link}
            to={paths.sharedTeamMatch(token, match.id)}
            sx={{ flexShrink: 0 }}
          >
            結果入力
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

interface TeamSharedPageProps {
  token: string;
  data: SharedTournament;
}

/** S10 共有ページの団体戦版。個人戦のSharedPageと同じ構成(タブ・ラウンド選択)を踏襲する */
export function TeamSharedPage({ token, data }: TeamSharedPageProps) {
  const [tab, setTab] = useState<'pairings' | 'standings' | 'matchResults'>('pairings');
  const [selectedRound, setSelectedRound] = useState<number | null>(null);

  const { tournament, teamRounds, teamStandings } = data;
  const rounds = teamRounds ?? [];
  const standings = teamStandings ?? [];
  const latestRound: TeamRound | null = rounds.length > 0 ? rounds[rounds.length - 1] : null;
  const currentRound = rounds.find((round) => round.roundNumber === selectedRound) ?? latestRound;
  const multiGroup = standings.length > 1;
  // ラウンド1確定前は全員rank=1で返るため、確定済みラウンドが1つもない間は順位表を出さない
  const round1Confirmed = rounds.some((round) => round.status === 'CONFIRMED');
  const canInput =
    tournament.resultInputEnabled &&
    tournament.status === 'IN_PROGRESS' &&
    currentRound !== null &&
    currentRound.status !== 'CONFIRMED';

  return (
    <Container component="main" maxWidth="md" sx={{ py: 3 }}>
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
        onChange={(_event, newTab: 'pairings' | 'standings' | 'matchResults') => setTab(newTab)}
        sx={{ mt: 2, mb: 3, borderBottom: 1, borderColor: 'divider' }}
        variant="fullWidth"
      >
        <Tab label="組み合わせ" value="pairings" />
        <Tab label="順位表" value="standings" />
        <Tab label="対戦結果" value="matchResults" />
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
                対局が終わったら「結果入力」から各ボードの勝敗を登録してください。
              </Typography>
            )}
            {currentRound.matches.length === 0 ? (
              <EmptyState
                icon={<EventSeatIcon fontSize="inherit" />}
                message="このラウンドの対局はありません。"
              />
            ) : (
              teamMatchSections(currentRound.matches).map(({ group, matches }) => (
                <Stack key={group.id} spacing={1.5}>
                  {multiGroup && (
                    <Typography variant="h4" component="h2">
                      {group.name}
                    </Typography>
                  )}
                  {matches.map((match) => (
                    <SharedTeamMatchCard
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
        (!round1Confirmed || standings.every((g) => g.standings.length === 0) ? (
          <EmptyState
            icon={<HourglassEmptyIcon fontSize="inherit" />}
            message="順位はまだありません。ラウンドを確定すると表示されます。"
          />
        ) : (
          standings.map(({ group, standings: groupStandings }) => (
            <Box key={group.id} sx={{ mb: 3 }}>
              {multiGroup && (
                <Typography variant="h4" component="h2" sx={{ mb: 1 }}>
                  {group.name}
                </Typography>
              )}
              <TeamRankingBoard standings={groupStandings} />
            </Box>
          ))
        ))}

      {tab === 'matchResults' &&
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
              <TeamMatchResultsTable
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
