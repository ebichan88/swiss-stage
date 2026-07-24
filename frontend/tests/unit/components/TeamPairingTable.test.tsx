import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TeamPairingTable } from '../../../src/components/features/team/TeamPairingTable';
import { boardResultOf, groupOf, teamMatchOf, teamSummaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('TeamPairingTable', () => {
  it('卓番号とチーム名を表示し、全ボード決着済みの対局は○/●が付く(個人名は含めない)', () => {
    const teamA = teamSummaryOf({ id: 't1', name: 'Aチーム' });
    const teamB = teamSummaryOf({ id: 't2', name: 'Bチーム' });
    renderWithProviders(
      <TeamPairingTable
        matches={[
          teamMatchOf({
            id: 'm1',
            tableNumber: 1,
            team1: teamA,
            team2: teamB,
            boardResults: [
              boardResultOf({ boardPosition: 1, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 2, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 3, result: 'PLAYER2_WIN' }),
            ],
          }),
          teamMatchOf({
            id: 'm2',
            tableNumber: 2,
            team1: teamSummaryOf({ id: 't3', name: 'Cチーム' }),
            team2: null,
            boardResults: [],
          }),
        ]}
        editable={false}
        multiGroup={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('○ Aチーム');
    expect(rows[1]).toHaveTextContent('● Bチーム');
    expect(rows[2]).toHaveTextContent('Cチーム');
    expect(within(rows[2]).getByText('(不戦勝)')).toBeInTheDocument();
    expect(screen.queryByText('山田太郎')).not.toBeInTheDocument();
  });

  it('グループ大会の卓番号は「A-1」形式で表示する', () => {
    renderWithProviders(
      <TeamPairingTable
        matches={[teamMatchOf({ id: 'm1', tableNumber: 1, group: groupOf({ name: 'A' }) })]}
        editable={false}
        multiGroup
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('A-1');
  });
});
