import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TeamCrossTable } from '../../../src/components/features/team/TeamCrossTable';
import {
  boardResultOf,
  teamMatchOf,
  teamRoundOf,
  teamStandingOf,
  teamSummaryOf,
} from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('TeamCrossTable', () => {
  it('No.・チーム名・ラウンドごとの相手番号と結果・勝点・順位を表示する(個人名は含めない)', () => {
    const teamA = teamSummaryOf({ id: 't1', name: 'Aチーム', entryOrder: 1 });
    const teamB = teamSummaryOf({ id: 't2', name: 'Bチーム', entryOrder: 2 });
    const rounds = [
      teamRoundOf({
        roundNumber: 1,
        matches: [
          teamMatchOf({
            team1: teamA,
            team2: teamB,
            boardResults: [
              boardResultOf({ boardPosition: 1, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 2, result: 'PLAYER1_WIN' }),
              boardResultOf({ boardPosition: 3, result: 'PLAYER2_WIN' }),
            ],
          }),
        ],
      }),
    ];
    const standings = [
      teamStandingOf({ team: teamA, rank: 1, wins: 2 }),
      teamStandingOf({ team: teamB, rank: 2, wins: 0 }),
    ];

    renderWithProviders(<TeamCrossTable rounds={rounds} standings={standings} />);

    expect(screen.getByRole('columnheader', { name: '第1ラウンド' })).toBeInTheDocument();
    const rows = screen.getAllByRole('row');
    // ヘッダー2行 + データ2行
    expect(rows).toHaveLength(4);
    expect(rows[2]).toHaveTextContent('Aチーム');
    expect(rows[2]).toHaveTextContent('2'); // 相手はNo.2(Bチーム)
    expect(rows[2]).toHaveTextContent('○');
    expect(rows[2]).toHaveTextContent('2-1'); // ボード内訳
    expect(rows[3]).toHaveTextContent('Bチーム');
    expect(rows[3]).toHaveTextContent('●');
    expect(screen.queryByText('山田太郎')).not.toBeInTheDocument();
  });

  it('不戦勝(BYE)は「不戦勝」と表示する', () => {
    const teamA = teamSummaryOf({ id: 't1', entryOrder: 1 });
    const rounds = [
      teamRoundOf({
        roundNumber: 1,
        matches: [teamMatchOf({ team1: teamA, team2: null, boardResults: [] })],
      }),
    ];
    const standings = [teamStandingOf({ team: teamA })];

    renderWithProviders(<TeamCrossTable rounds={rounds} standings={standings} />);

    expect(screen.getByText('不戦勝')).toBeInTheDocument();
  });
});
