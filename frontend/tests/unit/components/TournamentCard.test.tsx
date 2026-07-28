import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TournamentCard } from '../../../src/components/features/tournament/TournamentCard';
import { tournamentOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('TournamentCard', () => {
  it('TRN-AC-015: 開催日が設定されていれば開催日を表示する', () => {
    renderWithProviders(<TournamentCard tournament={tournamentOf({ eventDate: '2026-08-15' })} />);
    expect(screen.getByText(/2026\/8\/15開催/)).toBeInTheDocument();
  });

  it('TRN-AC-015: 開催日が未設定なら開催日を表示しない', () => {
    renderWithProviders(<TournamentCard tournament={tournamentOf({ eventDate: null })} />);
    expect(screen.queryByText(/開催/)).not.toBeInTheDocument();
  });
});
