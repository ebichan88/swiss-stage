import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { PrintRoster } from '../../../../src/components/features/print/PrintRoster';
import { groupOf, participantOf } from '../../../fixtures';
import { renderWithProviders } from '../../../testUtils';

describe('PrintRoster', () => {
  it('PRT-AC-002: 単一グループ大会はグループ列を出さない', () => {
    renderWithProviders(<PrintRoster participants={[participantOf()]} groups={[groupOf()]} />);
    expect(screen.queryByRole('columnheader', { name: 'グループ' })).not.toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '架空 太郎' })).toBeInTheDocument();
  });

  it('PRT-AC-002: 複数グループ大会はグループ列を出す', () => {
    const groupA = groupOf({ id: 'gA', name: 'A' });
    const groupB = groupOf({ id: 'gB', name: 'B' });
    renderWithProviders(
      <PrintRoster participants={[participantOf({ groupId: 'gA' })]} groups={[groupA, groupB]} />,
    );
    expect(screen.getByRole('columnheader', { name: 'グループ' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'A' })).toBeInTheDocument();
  });

  it('PRT-AC-002: 状態列は無く、出欠・備考列を持つ(受付での手書き記入用)', () => {
    renderWithProviders(<PrintRoster participants={[participantOf()]} groups={[groupOf()]} />);
    expect(screen.queryByRole('columnheader', { name: '状態' })).not.toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: '出欠' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: '備考' })).toBeInTheDocument();
  });

  it('PRT-AC-014: 参加者が0件でも見出し行を出力する(空欄テンプレートとして使える)', () => {
    renderWithProviders(<PrintRoster participants={[]} groups={[groupOf()]} />);
    expect(screen.getByRole('columnheader', { name: 'No.' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: '氏名' })).toBeInTheDocument();
    // データ行は無い(ヘッダー行のみ)
    expect(screen.getAllByRole('row')).toHaveLength(1);
  });
});
