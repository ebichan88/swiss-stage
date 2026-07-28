import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { MatchCardSheet } from '../../../../src/components/features/print/MatchCardSheet';
import type { MatchCard } from '../../../../src/components/features/print/matchCardData';
import { renderWithProviders } from '../../../testUtils';

function cardOf(overrides: Partial<MatchCard> = {}): MatchCard {
  return {
    entryOrder: 1,
    name: '架空 太郎',
    organization: 'テスト囲碁会',
    rankText: '3段',
    groupName: null,
    rowCount: 3,
    boardLabels: [],
    ...overrides,
  };
}

const layout = { columns: 2, rows: 6 };

describe('MatchCardSheet', () => {
  it('カード枚数分の table を出力する', () => {
    renderWithProviders(
      <MatchCardSheet
        pages={[[cardOf({ entryOrder: 1 }), cardOf({ entryOrder: 2 })]]}
        layout={layout}
      />,
    );
    expect(screen.getAllByRole('table')).toHaveLength(2);
  });

  it('転置レイアウト: ラウンドを列に、相手・勝敗を行に持つ(個人勝敗の行は持たない)', () => {
    renderWithProviders(<MatchCardSheet pages={[[cardOf({ rowCount: 3 })]]} layout={layout} />);
    expect(screen.getByText('1回戦')).toBeInTheDocument();
    expect(screen.getByText('3回戦')).toBeInTheDocument();
    expect(screen.getByText('相手')).toBeInTheDocument();
    expect(screen.getByText('勝敗')).toBeInTheDocument();
    expect(screen.queryByText('個人勝敗')).not.toBeInTheDocument();
  });

  it('氏名・所属・段級位・No.を表示する(氏名の横に段級位の枠がある)', () => {
    renderWithProviders(
      <MatchCardSheet
        pages={[
          [
            cardOf({
              entryOrder: 12,
              name: '架空 太郎',
              organization: 'テスト囲碁会',
              rankText: '3段',
            }),
          ],
        ]}
        layout={layout}
      />,
    );
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('架空 太郎(テスト囲碁会)')).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '3段' })).toBeInTheDocument();
  });

  it('勝敗合計の集計欄を持ち、団体戦の個人勝敗合計・チーム勝敗合計は持たない', () => {
    renderWithProviders(<MatchCardSheet pages={[[cardOf()]]} layout={layout} />);
    expect(screen.getByText(/勝敗合計/)).toBeInTheDocument();
    expect(screen.queryByText(/個人勝敗合計/)).not.toBeInTheDocument();
    expect(screen.queryByText(/チーム勝敗合計/)).not.toBeInTheDocument();
  });

  it('複数グループ大会はNo.にグループ名を前置し、単一グループは出さない', () => {
    renderWithProviders(
      <MatchCardSheet pages={[[cardOf({ groupName: 'A', entryOrder: 12 })]]} layout={layout} />,
    );
    expect(screen.getByText('A-12')).toBeInTheDocument();
  });
});
