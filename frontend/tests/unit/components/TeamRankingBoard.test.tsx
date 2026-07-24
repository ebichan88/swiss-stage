import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TeamRankingBoard } from '../../../src/components/features/team/TeamRankingBoard';
import { teamStandingOf, teamSummaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('TeamRankingBoard', () => {
  it('1〜3位はカード、4位以降はリスト行で表示する(チーム名のみ・個人名は含めない)', () => {
    renderWithProviders(
      <TeamRankingBoard
        standings={[
          teamStandingOf({ rank: 1, team: teamSummaryOf({ id: 'a', name: 'Aチーム' }), wins: 4 }),
          teamStandingOf({ rank: 2, team: teamSummaryOf({ id: 'b', name: 'Bチーム' }), wins: 3 }),
          teamStandingOf({ rank: 3, team: teamSummaryOf({ id: 'c', name: 'Cチーム' }), wins: 2 }),
          teamStandingOf({ rank: 4, team: teamSummaryOf({ id: 'd', name: 'Dチーム' }), wins: 1 }),
        ]}
      />,
    );

    const rows = screen.getAllByTestId('team-standing-row');
    expect(rows).toHaveLength(4);

    expect(within(rows[0]).getByTestId('team-standing-rank')).toHaveTextContent('1位');
    expect(rows[0]).toHaveTextContent('Aチーム');
    expect(within(rows[0]).getByTestId('team-standing-wins')).toHaveTextContent('4 pt');

    expect(within(rows[3]).getByTestId('team-standing-rank')).toHaveTextContent('4');
    expect(rows[3]).toHaveTextContent('Dチーム');
    expect(screen.queryByText('山田太郎')).not.toBeInTheDocument();
  });

  it('SOS/SOSOSラベルで表示する', () => {
    renderWithProviders(
      <TeamRankingBoard standings={[teamStandingOf({ rank: 1, sos: 10, sosos: 24.5 })]} />,
    );

    const row = screen.getByTestId('team-standing-row');
    expect(within(row).getByTestId('team-standing-sos')).toHaveTextContent('10');
    expect(within(row).getByTestId('team-standing-sosos')).toHaveTextContent('24.5');
    expect(row).toHaveTextContent('SOSOS');
  });
});
