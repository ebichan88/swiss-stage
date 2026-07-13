import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { StandingsTable } from '../../../src/components/features/standing/StandingsTable';
import { standingOf, summaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('StandingsTable', () => {
  it('順位・氏名(所属)・勝点を表示する(同順位あり)', () => {
    renderWithProviders(
      <StandingsTable
        standings={[
          standingOf({ rank: 1, participant: summaryOf({ id: 'a', name: '架空 太郎' }), wins: 4 }),
          standingOf({
            rank: 2,
            participant: summaryOf({ id: 'b', name: '仮名 花子', organization: null }),
            wins: 3.5,
            hadBye: true,
          }),
          standingOf({
            rank: 2,
            participant: summaryOf({ id: 'c', name: '試験 次郎' }),
            wins: 3.5,
          }),
        ]}
      />,
    );

    const rows = screen.getAllByRole('row');
    expect(rows).toHaveLength(4); // ヘッダー + 3行
    expect(rows[1]).toHaveTextContent('架空 太郎');
    expect(rows[1]).toHaveTextContent('(テスト囲碁会)');
    expect(rows[1]).toHaveTextContent('4');
    expect(rows[2]).toHaveTextContent('仮名 花子');
    expect(rows[2]).toHaveTextContent('3.5');
    expect(rows[2]).toHaveTextContent('あり'); // 不戦勝
    expect(rows[3]).toHaveTextContent('2'); // 同順位(1,2,2形式)
  });
});
