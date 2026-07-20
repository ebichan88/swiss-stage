import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { CrossTable } from '../../../src/components/features/standing/CrossTable';
import { matchOf, roundOf, standingOf, summaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('CrossTable', () => {
  it('No.・氏名・段級位・ラウンドごとの相手番号と結果・勝点・順位を表示する', () => {
    const taro = summaryOf({ id: 'p1', name: '架空 太郎', entryOrder: 1, rank: 'DAN_3' });
    const hanako = summaryOf({
      id: 'p2',
      name: '仮名 花子',
      organization: null,
      entryOrder: 2,
      rank: null,
    });
    const rounds = [
      roundOf({
        roundNumber: 1,
        matches: [matchOf({ player1: taro, player2: hanako, result: 'PLAYER1_WIN' })],
      }),
    ];
    const standings = [
      standingOf({ rank: 1, participant: taro, wins: 1 }),
      standingOf({ rank: 2, participant: hanako, wins: 0 }),
    ];

    renderWithProviders(<CrossTable rounds={rounds} standings={standings} />);

    expect(screen.getByRole('columnheader', { name: '第1ラウンド' })).toBeInTheDocument();
    const rows = screen.getAllByRole('row');
    // ヘッダー2行 + データ2行
    expect(rows).toHaveLength(4);
    expect(rows[2]).toHaveTextContent('架空 太郎');
    expect(rows[2]).toHaveTextContent('3段');
    expect(rows[2]).toHaveTextContent('2'); // 相手はNo.2(花子)
    expect(rows[2]).toHaveTextContent('○');
    expect(rows[3]).toHaveTextContent('仮名 花子');
    expect(rows[3]).toHaveTextContent('未入力'); // 段級位未入力
    expect(rows[3]).toHaveTextContent('●');
  });

  it('不戦勝(BYE)は「不戦勝」と表示する', () => {
    const taro = summaryOf({ id: 'p1', entryOrder: 1 });
    const rounds = [
      roundOf({
        roundNumber: 1,
        matches: [matchOf({ player1: taro, player2: null, result: 'BYE' })],
      }),
    ];
    const standings = [standingOf({ participant: taro })];

    renderWithProviders(<CrossTable rounds={rounds} standings={standings} />);

    expect(screen.getByText('不戦勝')).toBeInTheDocument();
  });
});
