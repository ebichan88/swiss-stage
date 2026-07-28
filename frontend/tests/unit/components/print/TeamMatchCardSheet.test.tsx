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
    rowCount: 3,
    boardLabels: ['主将', '副将', '三将'],
    ...overrides,
  };
}

const layout = { columns: 1, rows: 3 };

describe('TeamMatchCardSheet', () => {
  it('メンバーの個人名を一切含めない(チーム名のみ表示する)', () => {
    renderWithProviders(<TeamMatchCardSheet pages={[[teamCardOf()]]} layout={layout} />);
    expect(screen.getByText('Aチーム')).toBeInTheDocument();
    expect(screen.queryByText('架空 主将')).not.toBeInTheDocument();
  });

  it('転置レイアウト: ラウンドを列に、記入項目(相手/チーム勝敗/個人勝敗)を行に持つ', () => {
    renderWithProviders(
      <TeamMatchCardSheet pages={[[teamCardOf({ rowCount: 3 })]]} layout={layout} />,
    );
    expect(screen.getByText('1回戦')).toBeInTheDocument();
    expect(screen.getByText('3回戦')).toBeInTheDocument();
    expect(screen.getByText('相手')).toBeInTheDocument();
    expect(screen.getByText('チーム勝敗')).toBeInTheDocument();
    expect(screen.getByText('個人勝敗')).toBeInTheDocument();
  });

  it('ボード役割(主将・副将…)を各ラウンドの列に出す', () => {
    renderWithProviders(
      <TeamMatchCardSheet pages={[[teamCardOf({ rowCount: 3 })]]} layout={layout} />,
    );
    // 3ラウンド分、各ラウンドに主将・副将・三将が並ぶ
    expect(screen.getAllByText('主将')).toHaveLength(3);
    expect(screen.getAllByText('三将')).toHaveLength(3);
  });

  it('チーム勝敗合計・個人勝敗合計の集計欄を持つ', () => {
    renderWithProviders(<TeamMatchCardSheet pages={[[teamCardOf()]]} layout={layout} />);
    expect(screen.getByText(/チーム勝敗合計/)).toBeInTheDocument();
    expect(screen.getByText(/個人勝敗合計/)).toBeInTheDocument();
  });

  it('カード枚数分の table を出力する', () => {
    renderWithProviders(
      <TeamMatchCardSheet
        pages={[[teamCardOf({ entryOrder: 1 }), teamCardOf({ entryOrder: 2 })]]}
        layout={layout}
      />,
    );
    expect(screen.getAllByRole('table')).toHaveLength(2);
  });
});
