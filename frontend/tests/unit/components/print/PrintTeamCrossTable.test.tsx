import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PrintTeamCrossTable } from '../../../../src/components/features/print/PrintTeamCrossTable';
import { teamOf } from '../../../fixtures';
import { renderWithProviders } from '../../../testUtils';

describe('PrintTeamCrossTable', () => {
  it('生成済みラウンド数によらず totalRounds 分すべてのラウンド列を出す', () => {
    renderWithProviders(<PrintTeamCrossTable teams={[teamOf()]} totalRounds={5} />);
    for (let round = 1; round <= 5; round++) {
      expect(screen.getByRole('columnheader', { name: `${round}回戦` })).toBeInTheDocument();
    }
  });

  it('No.・チーム名は入力済みで表示し、対戦相手・結果・勝点・SOS・SOSOS・順位は空欄で出力する(段級位列は無い)', () => {
    renderWithProviders(
      <PrintTeamCrossTable teams={[teamOf({ entryOrder: 1, name: 'Aチーム' })]} totalRounds={2} />,
    );

    const rows = screen.getAllByRole('row');
    // ヘッダー2行 + データ1行
    expect(rows).toHaveLength(3);
    expect(rows[2]).toHaveTextContent('Aチーム');
    expect(screen.queryByRole('columnheader', { name: '段級位' })).not.toBeInTheDocument();

    // データ行のNo./チーム名を除く残り全セル(相手×2・結果×2・勝点・SOS・SOSOS・順位)は空欄
    const cells = screen.getAllByRole('cell');
    const blankCells = cells.slice(2);
    expect(blankCells).toHaveLength(2 * 2 + 4);
    blankCells.forEach((cell) => expect(cell).toHaveTextContent(''));
  });

  it('個人名を一切含めない(チーム名のみ)', () => {
    renderWithProviders(<PrintTeamCrossTable teams={[teamOf()]} totalRounds={1} />);
    expect(screen.queryByText('架空 主将')).not.toBeInTheDocument();
  });

  it('棄権(WITHDRAWN)は出力しない', () => {
    renderWithProviders(
      <PrintTeamCrossTable
        teams={[teamOf({ id: 't1' }), teamOf({ id: 't2', status: 'WITHDRAWN' })]}
        totalRounds={3}
      />,
    );
    expect(screen.getAllByRole('row')).toHaveLength(3); // ヘッダー2行 + データ1行
  });
});
