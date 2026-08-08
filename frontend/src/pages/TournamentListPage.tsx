import AddIcon from '@mui/icons-material/Add';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import SearchOffIcon from '@mui/icons-material/SearchOff';
import { Box, Button, Container, Grid, Typography } from '@mui/material';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { TournamentCard } from '../components/features/tournament/TournamentCard';
import { TournamentSearchToolbar } from '../components/features/tournament/TournamentSearchToolbar';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useTournaments } from '../hooks/useTournaments';
import { paths } from '../routes';
import type { TournamentStatus } from '../types/enums';

/** S03 大会一覧(ダッシュボード) */
export function TournamentListPage() {
  const { data: tournaments, isPending, isError, refetch } = useTournaments();
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<TournamentStatus | ''>('');

  const filteredTournaments = useMemo(() => {
    if (!tournaments) {
      return undefined;
    }
    const normalizedSearchText = searchText.trim().toLowerCase();
    return tournaments.filter(
      (tournament) =>
        tournament.name.toLowerCase().includes(normalizedSearchText) &&
        (statusFilter === '' || tournament.status === statusFilter),
    );
  }, [tournaments, searchText, statusFilter]);

  const handleClearFilter = () => {
    setSearchText('');
    setStatusFilter('');
  };

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
      {tournaments && tournaments.length > 0 && filteredTournaments && (
        <>
          <TournamentSearchToolbar
            searchText={searchText}
            onSearchTextChange={setSearchText}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
          />
          {filteredTournaments.length === 0 ? (
            <EmptyState
              icon={<SearchOffIcon fontSize="inherit" />}
              message="条件に一致する大会がありません"
              action={
                <Button variant="outlined" onClick={handleClearFilter}>
                  検索条件をクリア
                </Button>
              }
            />
          ) : (
            <Grid container spacing={2}>
              {filteredTournaments.map((tournament) => (
                <Grid key={tournament.id} size={{ xs: 12, sm: 6, md: 4 }}>
                  <TournamentCard tournament={tournament} />
                </Grid>
              ))}
            </Grid>
          )}
        </>
      )}
    </Container>
  );
}
