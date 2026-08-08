import SearchIcon from '@mui/icons-material/Search';
import { InputAdornment, MenuItem, Stack, TextField } from '@mui/material';

import { TournamentStatus } from '../../../types/enums';
import { tournamentStatusLabels } from '../../../utils/labels';

export interface TournamentSearchToolbarProps {
  searchText: string;
  onSearchTextChange: (value: string) => void;
  statusFilter: TournamentStatus | '';
  onStatusFilterChange: (value: TournamentStatus | '') => void;
}

export function TournamentSearchToolbar({
  searchText,
  onSearchTextChange,
  statusFilter,
  onStatusFilterChange,
}: TournamentSearchToolbarProps) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 3 }}>
      <TextField
        value={searchText}
        onChange={(e) => onSearchTextChange(e.target.value)}
        placeholder="大会名で検索"
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" />
              </InputAdornment>
            ),
          },
          htmlInput: { 'aria-label': '大会名で検索' },
        }}
        fullWidth
        sx={{ flex: { sm: 2 } }}
      />
      <TextField
        select
        value={statusFilter}
        onChange={(e) => onStatusFilterChange(e.target.value as TournamentStatus | '')}
        slotProps={{
          select: {
            SelectDisplayProps: { 'aria-label': '状態で絞り込み' },
          },
        }}
        fullWidth
        sx={{ flex: { sm: 1 }, minWidth: { sm: 160 } }}
      >
        <MenuItem value="">すべて</MenuItem>
        {Object.values(TournamentStatus).map((status) => (
          <MenuItem key={status} value={status}>
            {tournamentStatusLabels[status]}
          </MenuItem>
        ))}
      </TextField>
    </Stack>
  );
}
