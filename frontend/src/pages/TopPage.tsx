import { Box, Container, Typography } from '@mui/material';

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
      </Box>
    </Container>
  );
}
