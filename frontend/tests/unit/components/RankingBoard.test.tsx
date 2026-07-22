import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { RankingBoard } from '../../../src/components/features/standing/RankingBoard';
import { standingOf, summaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('RankingBoard', () => {
  it('1〜3位はカード、4位以降はリスト行で表示する(同順位あり)', () => {
    renderWithProviders(
      <RankingBoard
        standings={[
          standingOf({ rank: 1, participant: summaryOf({ id: 'a', name: '架空 太郎' }), wins: 4 }),
          standingOf({
            rank: 2,
            participant: summaryOf({ id: 'b', name: '仮名 花子', organization: null }),
            wins: 3.5,
          }),
          standingOf({
            rank: 2,
            participant: summaryOf({ id: 'c', name: '試験 次郎' }),
            wins: 3.5,
          }),
          standingOf({ rank: 4, participant: summaryOf({ id: 'd', name: '検証 三郎' }), wins: 2 }),
        ]}
      />,
    );

    const rows = screen.getAllByTestId('standing-row');
    expect(rows).toHaveLength(4);

    expect(within(rows[0]).getByTestId('standing-rank')).toHaveTextContent('1位');
    expect(rows[0]).toHaveTextContent('架空 太郎');
    expect(within(rows[0]).getByTestId('standing-wins')).toHaveTextContent('4 pt');

    expect(rows[1]).toHaveTextContent('仮名 花子');
    expect(within(rows[2]).getByTestId('standing-rank')).toHaveTextContent('2位'); // 同順位(1,2,2,4形式)

    expect(within(rows[3]).getByTestId('standing-rank')).toHaveTextContent('4');
    expect(rows[3]).toHaveTextContent('検証 三郎');
    expect(within(rows[3]).getByTestId('standing-wins')).toHaveTextContent('2 pt');
  });

  it('SOS/SOSOSラベルで表示する', () => {
    renderWithProviders(
      <RankingBoard standings={[standingOf({ rank: 1, sos: 10, sosos: 24.5 })]} />,
    );

    const row = screen.getByTestId('standing-row');
    expect(within(row).getByTestId('standing-sos')).toHaveTextContent('10');
    expect(within(row).getByTestId('standing-sosos')).toHaveTextContent('24.5');
    expect(row).toHaveTextContent('SOSOS');
  });
});
