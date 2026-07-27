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
    rowCount: 5,
    boardLabels: [],
    ...overrides,
  };
}

describe('MatchCardSheet', () => {
  it('カード枚数分の table を出力する', () => {
    renderWithProviders(
      <MatchCardSheet
        pages={[[cardOf({ entryOrder: 1 }), cardOf({ entryOrder: 2 })]]}
        layout={{ columns: 4, rows: 4 }}
      />,
    );
    expect(screen.getAllByRole('table')).toHaveLength(2);
  });

  it('1枚の記入行数は rowCount+1(ヘッダー込み)、結果セルは空になる', () => {
    renderWithProviders(
      <MatchCardSheet pages={[[cardOf({ rowCount: 5 })]]} layout={{ columns: 4, rows: 4 }} />,
    );
    const rows = screen.getAllByRole('row');
    expect(rows).toHaveLength(6);
    // データ行(最終行)の卓・相手・結果セルは空
    const cells = screen.getAllByRole('cell');
    const lastRowCells = cells.slice(-3);
    lastRowCells.forEach((cell) => expect(cell).toHaveTextContent(''));
  });

  it('氏名・所属・段級位・No.を表示する', () => {
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
        layout={{ columns: 4, rows: 4 }}
      />,
    );
    expect(screen.getByText(/No\.12/)).toBeInTheDocument();
    expect(screen.getByText(/架空 太郎/)).toBeInTheDocument();
    expect(screen.getByText(/テスト囲碁会/)).toBeInTheDocument();
    expect(screen.getByText(/3段/)).toBeInTheDocument();
  });

  it('複数グループ大会はグループ名をプレフィックス表示し、単一グループは出さない', () => {
    renderWithProviders(
      <MatchCardSheet pages={[[cardOf({ groupName: 'A' })]]} layout={{ columns: 4, rows: 4 }} />,
    );
    expect(screen.getByText(/\[A\]/)).toBeInTheDocument();
  });
});
