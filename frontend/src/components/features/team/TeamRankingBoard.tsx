import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import MilitaryTechIcon from '@mui/icons-material/MilitaryTech';
import WorkspacePremiumIcon from '@mui/icons-material/WorkspacePremium';
import { Avatar, Box, Card, CardContent, Chip, Fade, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

import type { TeamStanding } from '../../../types/team';
import { formatPoints } from '../../../utils/format';

export interface TeamRankingBoardProps {
  standings: TeamStanding[];
}

const TOP_RANK_ICON: Record<1 | 2 | 3, ReactNode> = {
  1: <EmojiEventsIcon />,
  2: <MilitaryTechIcon />,
  3: <WorkspacePremiumIcon />,
};

const TOP_RANK_PALETTE_KEY: Record<1 | 2 | 3, 'gold' | 'silver' | 'bronze'> = {
  1: 'gold',
  2: 'silver',
  3: 'bronze',
};

// 段階フェードインは初回マウント時のみ(03_animation_system.md: 更新時の再フェードは禁止)
const FADE_TIMEOUT_MS = 250;
const FADE_STAGGER_MS = 60;

interface TopRankCardProps {
  standing: TeamStanding;
  delayMs: number;
}

function TopRankCard({ standing, delayMs }: TopRankCardProps) {
  const rank = standing.rank as 1 | 2 | 3;
  const paletteKey = TOP_RANK_PALETTE_KEY[rank];

  return (
    <Fade in timeout={FADE_TIMEOUT_MS} style={{ transitionDelay: `${delayMs}ms` }}>
      <Card
        role="listitem"
        data-testid="team-standing-row"
        variant="outlined"
        sx={{
          flex: '1 1 200px',
          bgcolor: `rank.${paletteKey}.background`,
          borderColor: `rank.${paletteKey}.main`,
        }}
      >
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Avatar sx={{ bgcolor: `rank.${paletteKey}.main`, width: 28, height: 28 }}>
                {TOP_RANK_ICON[rank]}
              </Avatar>
              <Typography variant="h3" component="span" data-testid="team-standing-rank">
                {rank}位
              </Typography>
            </Box>
            <Chip
              size="small"
              color="success"
              label={`${formatPoints(standing.wins)} pt`}
              data-testid="team-standing-wins"
            />
          </Box>
          <Typography variant="body1" sx={{ mt: 1, fontWeight: 600 }}>
            {standing.team.name}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            SOS: <span data-testid="team-standing-sos">{formatPoints(standing.sos)}</span> | SOSOS:{' '}
            <span data-testid="team-standing-sosos">{formatPoints(standing.sosos)}</span>
          </Typography>
        </CardContent>
      </Card>
    </Fade>
  );
}

interface RankRowProps {
  standing: TeamStanding;
  delayMs: number;
}

function RankRow({ standing, delayMs }: RankRowProps) {
  return (
    <Fade in timeout={FADE_TIMEOUT_MS} style={{ transitionDelay: `${delayMs}ms` }}>
      <Card
        role="listitem"
        data-testid="team-standing-row"
        variant="outlined"
        sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 1.5 }}
      >
        <Avatar
          sx={{ bgcolor: 'background.default', color: 'text.primary', width: 32, height: 32 }}
        >
          <Typography variant="body2" component="span" data-testid="team-standing-rank">
            {standing.rank}
          </Typography>
        </Avatar>
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="body1">{standing.team.name}</Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ flexShrink: 0 }}>
          SOS: <span data-testid="team-standing-sos">{formatPoints(standing.sos)}</span> | SOSOS:{' '}
          <span data-testid="team-standing-sosos">{formatPoints(standing.sosos)}</span>
        </Typography>
        <Chip
          size="small"
          color="success"
          label={`${formatPoints(standing.wins)} pt`}
          data-testid="team-standing-wins"
          sx={{ flexShrink: 0 }}
        />
      </Card>
    </Fade>
  );
}

/**
 * 団体戦の順位カード表示(共有ページ・管理画面の順位メニューで共用)。個人名は含めない。
 * 1〜3位はメダルカード、4位以降はリスト行。初期表示のみ段階フェードイン(03_animation_system.md)
 */
export function TeamRankingBoard({ standings }: TeamRankingBoardProps) {
  const topRanks = standings.filter((s) => s.rank <= 3);
  const restRanks = standings.filter((s) => s.rank > 3);

  return (
    <Box role="list" aria-label="順位">
      {topRanks.length > 0 && (
        <Stack direction="row" spacing={2} sx={{ mb: 2, flexWrap: 'wrap' }}>
          {topRanks.map((standing, index) => (
            <TopRankCard
              key={standing.team.id}
              standing={standing}
              delayMs={index * FADE_STAGGER_MS}
            />
          ))}
        </Stack>
      )}
      <Stack spacing={1}>
        {restRanks.map((standing, index) => (
          <RankRow
            key={standing.team.id}
            standing={standing}
            delayMs={(topRanks.length + index) * FADE_STAGGER_MS}
          />
        ))}
      </Stack>
    </Box>
  );
}
