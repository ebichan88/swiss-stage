import AddIcon from '@mui/icons-material/Add';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import { Box, Button, Container, Grid, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

import { TournamentCard } from '../components/features/tournament/TournamentCard';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useTournaments } from '../hooks/useTournaments';
import { paths } from '../routes';

/** S03 大会一覧(ダッシュボード) */
export function TournamentListPage() {
  const { data: tournaments, isPending, isError, refetch } = useTournaments();

  return (
    <Container maxWidth="lg" sx={{ py: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h2" component="h1">
          大会一覧
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          component={Link}
          to={paths.tournamentNew}
        >
          大会を作成
        </Button>
      </Box>
      {isPending && <LoadingState />}
      {isError && (
        <ErrorState message="大会一覧の取得に失敗しました" onRetry={() => void refetch()} />
      )}
      {tournaments && tournaments.length === 0 && (
        <EmptyState
          icon={<EmojiEventsIcon fontSize="inherit" />}
          message="大会がまだありません"
          action={
            <Button variant="outlined" component={Link} to={paths.tournamentNew}>
              最初の大会を作成する
            </Button>
          }
        />
      )}
      {tournaments && tournaments.length > 0 && (
        <Grid container spacing={2}>
          {tournaments.map((tournament) => (
            <Grid key={tournament.id} size={{ xs: 12, sm: 6, md: 4 }}>
              <TournamentCard tournament={tournament} />
            </Grid>
          ))}
        </Grid>
      )}
    </Container>
  );
}
