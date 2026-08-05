import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { ParticipantTable } from '../../../src/components/features/participant/ParticipantTable';
import { participantOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('ParticipantTable', () => {
  it('PTC-AC-013: 棄権者の行は半透明表示とPersonOffIconの両方で他の行と区別できる', () => {
    renderWithProviders(
      <ParticipantTable
        participants={[
          participantOf({ id: 'p1', name: '架空 太郎', entryOrder: 1, status: 'ACTIVE' }),
          participantOf({ id: 'p2', name: '仮名 花子', entryOrder: 2, status: 'WITHDRAWN' }),
        ]}
        groups={[]}
        canEdit={false}
        canWithdraw={false}
        onEdit={() => {}}
        onWithdraw={() => {}}
        onDelete={() => {}}
        onChangeGroup={() => {}}
      />,
    );

    const rows = screen.getAllByRole('row');
    const activeRow = rows[1];
    const withdrawnRow = rows[2];

    expect(activeRow).toHaveStyle({ opacity: '1' });
    expect(withdrawnRow).toHaveStyle({ opacity: '0.55' });
    expect(within(withdrawnRow).getByText('棄権')).toBeInTheDocument();
    // PersonOffIconはMUI ChipのiconプロップでSVGとして描画される
    expect(within(withdrawnRow).getByTestId('PersonOffIcon')).toBeInTheDocument();
    expect(within(activeRow).queryByTestId('PersonOffIcon')).not.toBeInTheDocument();
  });

  it('編集可の場合、氏名を編集・削除できる操作列が表示される', async () => {
    const onEdit = vi.fn();
    renderWithProviders(
      <ParticipantTable
        participants={[participantOf({ id: 'p1', name: '架空 太郎' })]}
        groups={[]}
        canEdit
        canWithdraw={false}
        onEdit={onEdit}
        onWithdraw={() => {}}
        onDelete={() => {}}
        onChangeGroup={() => {}}
      />,
    );

    expect(screen.getByRole('button', { name: '架空 太郎を編集' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '架空 太郎を削除' })).toBeInTheDocument();
  });
});
