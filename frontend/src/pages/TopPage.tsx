import { Box, Button, Container, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

import { paths } from '../routes';

export function TopPage() {
  return (
    <Container maxWidth="sm">
      <Box sx={{ py: 8, textAlign: 'center' }}>
        <Typography variant="h1" gutterBottom>
          Swiss Stage
        </Typography>
        <Typography variant="body1" color="text.secondary">
          囲碁・将棋大会の運営をもっとシンプルに
        </Typography>
        <Button variant="contained" component={Link} to={paths.login} sx={{ mt: 4 }}>
          運営者ログイン
        </Button>
      </Box>
    </Container>
  );
}
