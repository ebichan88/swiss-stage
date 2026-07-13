import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { PairingTable } from '../../../src/components/features/round/PairingTable';
import { matchOf, summaryOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('PairingTable', () => {
  it('卓番号と対局者を表示し、勝敗入力済みは○/●が付く', () => {
    renderWithProviders(
      <PairingTable
        matches={[
          matchOf({ id: 'm1', tableNumber: 1, result: 'PLAYER1_WIN' }),
          matchOf({
            id: 'm2',
            tableNumber: 2,
            player1: summaryOf({ id: 'p3', name: '試験 次郎', organization: null }),
            player2: null,
            result: 'BYE',
          }),
        ]}
        editable
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    expect(rows[1]).toHaveTextContent('○ 架空 太郎(テスト囲碁会)');
    expect(rows[1]).toHaveTextContent('● 仮名 花子');
    // BYE(不戦勝)は入力不可で「不戦勝」表示
    expect(rows[2]).toHaveTextContent('(不戦勝)');
    expect(within(rows[2]).getByText('不戦勝')).toBeInTheDocument();
  });

  it('結果を選ぶと onInputResult が呼ばれる', async () => {
    const user = userEvent.setup();
    const onInputResult = vi.fn();
    const match = matchOf();
    renderWithProviders(
      <PairingTable
        matches={[match]}
        editable
        savingMatchId={null}
        onInputResult={onInputResult}
      />,
    );

    await user.click(screen.getByRole('combobox'));
    await user.click(screen.getByRole('option', { name: '○ 仮名 花子 の勝ち' }));

    expect(onInputResult).toHaveBeenCalledWith(match, 'PLAYER2_WIN');
  });

  it('確定済み(editable=false)は入力コントロールを出さない', () => {
    renderWithProviders(
      <PairingTable
        matches={[matchOf({ result: 'DRAW' })]}
        editable={false}
        savingMatchId={null}
        onInputResult={() => {}}
      />,
    );

    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.getByText('引き分け')).toBeInTheDocument();
  });
});
