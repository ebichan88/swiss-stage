import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { TeamMatchCardSheet } from '../../../../src/components/features/print/TeamMatchCardSheet';
import type { MatchCard } from '../../../../src/components/features/print/matchCardData';
import { renderWithProviders } from '../../../testUtils';

function teamCardOf(overrides: Partial<MatchCard> = {}): MatchCard {
  return {
    entryOrder: 1,
    name: 'Aチーム',
    organization: null,
    rankText: '',
    groupName: null,
    rowCount: 5,
    boardLabels: ['主将', '副将', '三将'],
    ...overrides,
  };
}

describe('TeamMatchCardSheet', () => {
  it('メンバーの個人名を一切含めない(チーム名とボード役割のみ)', () => {
    renderWithProviders(
      <TeamMatchCardSheet pages={[[teamCardOf()]]} layout={{ columns: 2, rows: 4 }} />,
    );
    expect(screen.getByText('Aチーム')).toBeInTheDocument();
    expect(screen.queryByText('架空 主将')).not.toBeInTheDocument();
  });

  it('ボード役割(主将・副将…)の列見出しを持つ', () => {
    renderWithProviders(
      <TeamMatchCardSheet pages={[[teamCardOf()]]} layout={{ columns: 2, rows: 4 }} />,
    );
    expect(screen.getByRole('columnheader', { name: '主将' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: '副将' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: '三将' })).toBeInTheDocument();
  });

  it('カード枚数分の table を出力する', () => {
    renderWithProviders(
      <TeamMatchCardSheet
        pages={[[teamCardOf({ entryOrder: 1 }), teamCardOf({ entryOrder: 2 })]]}
        layout={{ columns: 2, rows: 4 }}
      />,
    );
    expect(screen.getAllByRole('table')).toHaveLength(2);
  });
});
