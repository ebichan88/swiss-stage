import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { TeamTable } from '../../../src/components/features/team/TeamTable';
import { teamOf } from '../../fixtures';
import { renderWithProviders } from '../../testUtils';

describe('TeamTable', () => {
  it('TEAM-AC-025: 棄権者の行は半透明表示とPersonOffIconの両方で他の行と区別できる', () => {
    renderWithProviders(
      <TeamTable
        teams={[
          teamOf({ id: 't1', name: 'Aチーム', entryOrder: 1, status: 'ACTIVE' }),
          teamOf({ id: 't2', name: 'Bチーム', entryOrder: 2, status: 'WITHDRAWN' }),
        ]}
        teamSize={3}
        groups={[]}
        canEdit={false}
        canWithdraw={false}
        onEdit={() => {}}
        onManageMembers={() => {}}
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

  it('テーブルをoutlined枠線+角丸のPaperで囲む(ページ背景と偶数行の背景色が同化する境界不明瞭の回帰防止)', () => {
    renderWithProviders(
      <TeamTable
        teams={[teamOf({ id: 't1', name: 'Aチーム' })]}
        teamSize={3}
        groups={[]}
        canEdit={false}
        canWithdraw={false}
        onEdit={() => {}}
        onManageMembers={() => {}}
        onWithdraw={() => {}}
        onDelete={() => {}}
        onChangeGroup={() => {}}
      />,
    );

    const paper = screen.getByRole('table').closest('.MuiPaper-root');
    expect(paper).toHaveClass('MuiPaper-outlined');
  });

  it('編集可の場合、チーム名を編集・削除できる操作列が表示される', () => {
    const onEdit = vi.fn();
    renderWithProviders(
      <TeamTable
        teams={[teamOf({ id: 't1', name: 'Aチーム' })]}
        teamSize={3}
        groups={[]}
        canEdit
        canWithdraw={false}
        onEdit={onEdit}
        onManageMembers={() => {}}
        onWithdraw={() => {}}
        onDelete={() => {}}
        onChangeGroup={() => {}}
      />,
    );

    expect(screen.getByRole('button', { name: 'Aチームを編集' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Aチームを削除' })).toBeInTheDocument();
  });
});
