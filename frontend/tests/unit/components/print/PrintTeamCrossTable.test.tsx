import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PrintTeamCrossTable } from '../../../../src/components/features/print/PrintTeamCrossTable';
import {
  boardResultOf,
  teamMatchOf,
  teamRoundOf,
  teamStandingOf,
  teamSummaryOf,
} from '../../../fixtures';
import { renderWithProviders } from '../../../testUtils';

describe('PrintTeamCrossTable', () => {
  it('画面版(TeamCrossTable)と同じ入力から同じ値(相手No.・結果・ボード内訳・勝点・順位)を出力する', () => {
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

    renderWithProviders(<PrintTeamCrossTable rounds={rounds} standings={standings} />);

    const rows = screen.getAllByRole('row');
    // ヘッダー2行 + データ2行
    expect(rows).toHaveLength(4);
    expect(rows[2]).toHaveTextContent('Aチーム');
    expect(rows[2]).toHaveTextContent('2'); // 相手はNo.2(Bチーム)
    expect(rows[2]).toHaveTextContent('○');
    expect(rows[2]).toHaveTextContent('2-1'); // ボード内訳
    expect(rows[3]).toHaveTextContent('Bチーム');
    expect(rows[3]).toHaveTextContent('●');
  });

  it('メンバーの個人名を一切含めない(チーム名のみ)', () => {
    const teamA = teamSummaryOf({ id: 't1', entryOrder: 1 });
    const rounds = [
      teamRoundOf({
        roundNumber: 1,
        matches: [teamMatchOf({ team1: teamA, team2: null, boardResults: [] })],
      }),
    ];
    const standings = [teamStandingOf({ team: teamA })];

    renderWithProviders(<PrintTeamCrossTable rounds={rounds} standings={standings} />);

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

    renderWithProviders(<PrintTeamCrossTable rounds={rounds} standings={standings} />);

    expect(screen.getByText('不戦勝')).toBeInTheDocument();
  });
});
