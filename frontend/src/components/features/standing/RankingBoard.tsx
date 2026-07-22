import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import MilitaryTechIcon from '@mui/icons-material/MilitaryTech';
import WorkspacePremiumIcon from '@mui/icons-material/WorkspacePremium';
import { Avatar, Box, Card, CardContent, Chip, Fade, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

import type { Standing } from '../../../types/standing';
import { formatPoints } from '../../../utils/format';

export interface RankingBoardProps {
  standings: Standing[];
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

function participantLabel(standing: Standing): { name: string; organization: string | null } {
  return {
    name: standing.participant.name,
    organization: standing.participant.organization ?? null,
  };
}

interface TopRankCardProps {
  standing: Standing;
  delayMs: number;
}

function TopRankCard({ standing, delayMs }: TopRankCardProps) {
  const rank = standing.rank as 1 | 2 | 3;
  const { name, organization } = participantLabel(standing);
  const paletteKey = TOP_RANK_PALETTE_KEY[rank];

  return (
    <Fade in timeout={FADE_TIMEOUT_MS} style={{ transitionDelay: `${delayMs}ms` }}>
      <Card
        role="listitem"
        data-testid="standing-row"
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
              <Typography variant="h3" component="span" data-testid="standing-rank">
                {rank}位
              </Typography>
            </Box>
            <Chip
              size="small"
              color="success"
              label={`${formatPoints(standing.wins)} pt`}
              data-testid="standing-wins"
            />
          </Box>
          <Typography variant="body1" sx={{ mt: 1, fontWeight: 600 }}>
            {name}
          </Typography>
          {organization && (
            <Typography variant="body2" color="text.secondary">
              {organization}
            </Typography>
          )}
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            SOS: <span data-testid="standing-sos">{formatPoints(standing.sos)}</span> | SOSOS:{' '}
            <span data-testid="standing-sosos">{formatPoints(standing.sosos)}</span>
          </Typography>
        </CardContent>
      </Card>
    </Fade>
  );
}

interface RankRowProps {
  standing: Standing;
  delayMs: number;
}

function RankRow({ standing, delayMs }: RankRowProps) {
  const { name, organization } = participantLabel(standing);

  return (
    <Fade in timeout={FADE_TIMEOUT_MS} style={{ transitionDelay: `${delayMs}ms` }}>
      <Card
        role="listitem"
        data-testid="standing-row"
        variant="outlined"
        sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 1.5 }}
      >
        <Avatar
          sx={{ bgcolor: 'background.default', color: 'text.primary', width: 32, height: 32 }}
        >
          <Typography variant="body2" component="span" data-testid="standing-rank">
            {standing.rank}
          </Typography>
        </Avatar>
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="body1">{name}</Typography>
          {organization && (
            <Typography variant="body2" color="text.secondary">
              {organization}
            </Typography>
          )}
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ flexShrink: 0 }}>
          SOS: <span data-testid="standing-sos">{formatPoints(standing.sos)}</span> | SOSOS:{' '}
          <span data-testid="standing-sosos">{formatPoints(standing.sosos)}</span>
        </Typography>
        <Chip
          size="small"
          color="success"
          label={`${formatPoints(standing.wins)} pt`}
          data-testid="standing-wins"
          sx={{ flexShrink: 0 }}
        />
      </Card>
    </Fade>
  );
}

/**
 * 順位カード表示(共有ページ・管理画面の順位メニューで共用)。
 * 1〜3位はメダルカード、4位以降はリスト行。初期表示のみ段階フェードイン(03_animation_system.md)
 */
export function RankingBoard({ standings }: RankingBoardProps) {
  const topRanks = standings.filter((s) => s.rank <= 3);
  const restRanks = standings.filter((s) => s.rank > 3);

  return (
    <Box role="list" aria-label="順位">
      {topRanks.length > 0 && (
        <Stack direction="row" spacing={2} sx={{ mb: 2, flexWrap: 'wrap' }}>
          {topRanks.map((standing, index) => (
            <TopRankCard
              key={standing.participant.id}
              standing={standing}
              delayMs={index * FADE_STAGGER_MS}
            />
          ))}
        </Stack>
      )}
      <Stack spacing={1}>
        {restRanks.map((standing, index) => (
          <RankRow
            key={standing.participant.id}
            standing={standing}
            delayMs={(topRanks.length + index) * FADE_STAGGER_MS}
          />
        ))}
      </Stack>
    </Box>
  );
}
