import { Box, Card, CardActionArea, CardContent, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

import { paths } from '../../../routes';
import type { Tournament } from '../../../types/tournament';
import { formatDateTime } from '../../../utils/format';
import { gameTypeLabels } from '../../../utils/labels';
import { StatusBadge } from '../../ui/StatusBadge';

export interface TournamentCardProps {
  tournament: Tournament;
}

export function TournamentCard({ tournament }: TournamentCardProps) {
  return (
    <Card variant="outlined">
      <CardActionArea component={Link} to={paths.tournament(tournament.id)}>
        <CardContent>
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              gap: 1,
            }}
          >
            <Typography variant="h3" component="h2" gutterBottom sx={{ wordBreak: 'break-word' }}>
              {tournament.name}
            </Typography>
            <StatusBadge status={tournament.status} />
          </Box>
          <Typography variant="body2" color="text.secondary">
            {gameTypeLabels[tournament.gameType]} ・ 全{tournament.totalRounds}ラウンド
            {tournament.status === 'IN_PROGRESS' && ` ・ 第${tournament.currentRound}ラウンド`}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            作成: {formatDateTime(tournament.createdAt)}
          </Typography>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}
