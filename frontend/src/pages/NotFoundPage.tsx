import { Box, Button, Container, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

import { paths } from '../routes';

export function NotFoundPage() {
  return (
    <Container maxWidth="sm">
      <Box sx={{ py: 8, textAlign: 'center' }}>
        <Typography variant="h2" gutterBottom>
          ページが見つかりません
        </Typography>
        <Button component={Link} to={paths.top} variant="outlined" sx={{ mt: 2 }}>
          トップへ戻る
        </Button>
      </Box>
    </Container>
  );
}
